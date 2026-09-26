package dev.mw19.core.gui.page;

import dev.mw19.api.hud.Anchor;
import dev.mw19.api.module.HudModule;
import dev.mw19.api.setting.BoolSetting;
import dev.mw19.api.setting.ColorSetting;
import dev.mw19.api.setting.NumberSetting;
import dev.mw19.api.setting.Setting;
import dev.mw19.api.util.Colors;
import dev.mw19.core.Keys;
import dev.mw19.core.gui.GuiRoot;
import dev.mw19.core.gui.Ui;
import dev.mw19.core.gui.Widget;
import dev.mw19.core.gui.widget.Button;
import dev.mw19.core.gui.widget.Dropdown;
import dev.mw19.core.gui.widget.ScrollList;
import dev.mw19.core.gui.widget.SettingRow;
import dev.mw19.core.hud.HudElement;
import dev.mw19.core.hud.HudLayout;
import dev.mw19.core.module.ModuleManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Full-screen HUD layout editor. HUD elements live in vanilla GUI units, so drawing and hit-testing here undo the
 * menu scale; the toolbar and popups stay in menu space.
 */
public final class HudEditor extends Page {
    private static final float SNAP = 4, HANDLE = 6;
    private static final int UNDO_DEPTH = 64;
    private static final String HINT = "Drag: move (hold Alt: no snapping) · Corner: scale · Right-click: style · Arrows: nudge"
            + " · Ctrl+Z / Ctrl+Y · Del: hide · Esc: back";
    private List<String> hintLines;
    private float hintWidth;

    private ModuleManager.State selected;
    private boolean dragging, resizing;
    private float offX, offY, startScale, startDist, startBx, startBy;
    private final float[] gx = new float[128], gy = new float[128], hit = new float[1];
    private float guideX = Float.NaN, guideY = Float.NaN;
    private final List<Map<String, Map<String, Object>>> undo = new ArrayList<Map<String, Map<String, Object>>>();
    private final List<Map<String, Map<String, Object>>> redo = new ArrayList<Map<String, Map<String, Object>>>();
    private final List<Button> toolbar = new ArrayList<Button>();
    private final Dropdown addMenu;
    private final Button undoB, redoB, gridB, snapB;

    public HudEditor(final GuiRoot root) {
        super(root);
        undoB = tool("Undo", new Runnable() {
            @Override
            public void run() {
                undo();
            }
        });
        redoB = tool("Redo", new Runnable() {
            @Override
            public void run() {
                redo();
            }
        });
        gridB = tool("Grid", new Runnable() {
            @Override
            public void run() {
                root.k.client.hudGrid.toggle();
            }
        });
        snapB = tool("Snap", new Runnable() {
            @Override
            public void run() {
                root.k.client.hudSnap.toggle();
            }
        });
        addMenu = new Dropdown(new Dropdown.Model() {
            @Override
            public List<String> options() {
                List<String> out = new ArrayList<String>();
                for (ModuleManager.State s : root.k.modules.all()) {
                    if (s.module instanceof HudModule && !s.enabled() && (s.suspend() & ModuleManager.SUSPEND_UNAVAILABLE) == 0) out.add(s.module.name());
                }
                if (out.isEmpty()) out.add("(all elements shown)");
                return out;
            }

            @Override
            public boolean selected(String option) {
                return false;
            }

            @Override
            public void pick(String option) {
                for (ModuleManager.State s : root.k.modules.all()) {
                    if (s.module.name().equals(option)) {
                        snapshot();
                        root.k.modules.setEnabled(s, true);
                        selected = s;
                    }
                }
            }

            @Override
            public String summary() {
                return "+ Add";
            }
        }, false);
        addMenu.tooltip = "Show a hidden HUD element";
        tool("Reset all", new Runnable() {
            @Override
            public void run() {
                snapshot();
                for (ModuleManager.State s : root.k.modules.all()) {
                    if (s.module instanceof HudModule) root.k.hud.resetElement((HudModule) s.module);
                }
                root.k.config.markDirty();
            }
        }).style = Button.Style.DANGER;
        tool("Done", new Runnable() {
            @Override
            public void run() {
                root.closeHudEditor();
            }
        }).style = Button.Style.PRIMARY;
    }

    private Button tool(String label, Runnable r) {
        Button b = new Button(label, Button.Style.SECONDARY, r);
        toolbar.add(b);
        return b;
    }

    @Override
    public String title() {
        return "HUD";
    }

    @Override
    public String icon() {
        return "hud";
    }

    @Override
    public void onShow() {
        dragging = resizing = false;
    }

    private float menuScale() {
        return root.k.client.uiScale.floatValue();
    }

    // ------------------------------------------------------------------ render

    @Override
    public void render(Ui ui) {
        float s = menuScale();
        float sw = root.width() * s, sh = root.height() * s;
        float mx = ui.mx * s, my = ui.my * s;
        ui.g.push();
        ui.g.scale(1f / s);
        ui.g.rect(0, 0, sw, sh, 0x30000000);
        if (root.k.client.hudGrid.on()) {
            float cell = root.k.client.gridSize.floatValue();
            for (float x = cell; x < sw; x += cell) ui.g.rect(x, 0, x + 0.5f, sh, 0x18FFFFFF);
            for (float y = cell; y < sh; y += cell) ui.g.rect(0, y, sw, y + 0.5f, 0x18FFFFFF);
        }
        ui.g.rect(sw / 2 - 0.25f, 0, sw / 2 + 0.25f, sh, 0x20FFFFFF);
        ui.g.rect(0, sh / 2 - 0.25f, sw, sh / 2 + 0.25f, 0x20FFFFFF);
        root.k.hud.render(ui.g, sw, sh, true);
        for (ModuleManager.State st : root.k.modules.activeHud()) {
            HudElement e = root.k.hud.element((HudModule) st.module);
            if (e.bw <= 0) continue;
            boolean sel = st == selected;
            boolean hv = !dragging && !resizing && mx >= e.bx && my >= e.by && mx < e.bx + e.bw && my < e.by + e.bh && !root.blockedAt(ui.mx, ui.my);
            int col = sel ? ui.t.accent : hv ? 0xC0FFFFFF : 0x50FFFFFF;
            ui.g.roundOutline(e.bx - 1, e.by - 1, e.bw + 2, e.bh + 2, 2, sel ? 1 : 0.5f, col);
            if (sel) ui.g.rect(e.bx + e.bw - HANDLE / 2, e.by + e.bh - HANDLE / 2, e.bx + e.bw + HANDLE / 2, e.by + e.bh + HANDLE / 2, ui.t.accent);
            if (sel || hv) {
                String label = st.module.name() + "  " + Math.round(e.scale * 100) + "%  " + e.anchor.name().toLowerCase().replace('_', '-');
                float ly = e.by > 12 ? e.by - 11 : e.by + e.bh + 3;
                float lw = ui.g.textWidth(label) + 6;
                ui.g.roundRect(e.bx, ly, lw, 10, 2, 0xD0101216);
                ui.g.text(label, e.bx + 3, ly + 1, sel ? ui.t.accent : 0xFFFFFFFF, false);
            }
        }
        if (!Float.isNaN(guideX)) ui.g.rect(guideX - 0.5f, 0, guideX + 0.5f, sh, ui.t.accent);
        if (!Float.isNaN(guideY)) ui.g.rect(0, guideY - 0.5f, sw, guideY + 0.5f, ui.t.accent);
        ui.g.pop();

        // toolbar (menu space)
        gridB.style = root.k.client.hudGrid.on() ? Button.Style.PRIMARY : Button.Style.SECONDARY;
        snapB.style = root.k.client.hudSnap.on() ? Button.Style.PRIMARY : Button.Style.SECONDARY;
        undoB.enabled = !undo.isEmpty();
        redoB.enabled = !redo.isEmpty();
        float total = 0;
        for (Button b : toolbar) total += ui.g.textWidth(b.label) + 14 + 4;
        total += 54;
        float tx = (root.width() - total) / 2f, ty = 6;
        ui.g.roundRect(tx - 5, ty - 4, total + 10, 24, 6, ui.t.panel);
        for (int i = 0; i < toolbar.size(); i++) {
            Button b = toolbar.get(i);
            float bw = ui.g.textWidth(b.label) + 14;
            b.bounds(tx, ty, bw, 16).render(ui);
            tx += bw + 4;
            if (i == 3) {
                addMenu.bounds(tx, ty, 50, 16).render(ui);
                tx += 54;
            }
        }
        if (hintLines == null || hintWidth != root.width()) {
            hintWidth = root.width();
            hintLines = dev.mw19.core.gui.Toasts.wrap(ui.g, HINT, (root.width() - 20) / 0.75f);
        }
        float lh = 7.5f, hy = root.height() - 6 - hintLines.size() * lh;
        float hw = 0;
        for (String l : hintLines) hw = Math.max(hw, ui.g.textWidth(l) * 0.75f);
        ui.g.roundRect((root.width() - hw) / 2f - 5, hy - 3, hw + 10, hintLines.size() * lh + 4, 3, 0xB0101216);
        for (int i = 0; i < hintLines.size(); i++) {
            float lw = ui.g.textWidth(hintLines.get(i)) * 0.75f;
            ui.g.text(hintLines.get(i), (root.width() - lw) / 2f, hy + i * lh, 0.75f, 0xFFCCCCCC, false);
        }
    }

    // ------------------------------------------------------------------ input

    private ModuleManager.State elementAt(float mx, float my) {
        ModuleManager.State[] arr = root.k.modules.activeHud();
        for (int i = arr.length - 1; i >= 0; i--) {
            HudElement e = root.k.hud.element((HudModule) arr[i].module);
            if (e.bw > 0 && mx >= e.bx - 2 && my >= e.by - 2 && mx < e.bx + e.bw + 2 && my < e.by + e.bh + 2) return arr[i];
        }
        return null;
    }

    @Override
    public boolean mouseClicked(Ui ui, int button) {
        for (Button b : toolbar) if (b.mouseClicked(ui, button)) return true;
        if (addMenu.mouseClicked(ui, button)) return true;
        float s = menuScale(), mx = ui.mx * s, my = ui.my * s;
        if (selected != null && selected.active()) {
            HudElement e = root.k.hud.element((HudModule) selected.module);
            if (Math.abs(mx - (e.bx + e.bw)) <= HANDLE && Math.abs(my - (e.by + e.bh)) <= HANDLE && button == 0) {
                snapshot();
                resizing = true;
                startScale = e.scale;
                startBx = e.bx;
                startBy = e.by;
                startDist = Math.max(4, (float) Math.hypot(mx - e.bx, my - e.by));
                return true;
            }
        }
        ModuleManager.State st = elementAt(mx, my);
        if (st == null) {
            selected = null;
            return false;
        }
        selected = st;
        HudElement e = root.k.hud.element((HudModule) st.module);
        if (button == 1) {
            openStyle(ui, st, e);
            return true;
        }
        if (button == 0) {
            snapshot();
            dragging = true;
            offX = mx - e.bx;
            offY = my - e.by;
        }
        return true;
    }

    @Override
    public boolean mouseDragged(Ui ui, int button) {
        if (selected == null || !selected.active()) return false;
        float s = menuScale(), sw = root.width() * s, sh = root.height() * s, mx = ui.mx * s, my = ui.my * s;
        HudElement e = root.k.hud.element((HudModule) selected.module);
        if (resizing) {
            float d = (float) Math.hypot(mx - startBx, my - startBy);
            float ns = Math.round(Math.max(0.25f, Math.min(4f, startScale * d / startDist)) * 20f) / 20f;
            float cw = e.bw / e.scale - 2 * e.padding, ch = e.bh / e.scale - 2 * e.padding;
            e.scale = ns;
            e.bw = HudLayout.boxW(e, cw);
            e.bh = HudLayout.boxH(e, ch);
            HudLayout.moveTo(e, startBx, startBy, sw, sh);
            return true;
        }
        if (!dragging) return false;
        float nx = mx - offX, ny = my - offY;
        guideX = guideY = Float.NaN;
        boolean alt = root.k.platform.isKeyDown(Keys.LEFT_ALT) || root.k.platform.isKeyDown(Keys.RIGHT_ALT);
        if (root.k.client.hudSnap.on() && !alt) {
            int nxs = 0, nys = 0;
            gx[nxs++] = 0;
            gx[nxs++] = sw / 2;
            gx[nxs++] = sw;
            gy[nys++] = 0;
            gy[nys++] = sh / 2;
            gy[nys++] = sh;
            for (ModuleManager.State o : root.k.modules.activeHud()) {
                if (o == selected || nxs > gx.length - 3) continue;
                HudElement oe = root.k.hud.element((HudModule) o.module);
                if (oe.bw <= 0) continue;
                gx[nxs++] = oe.bx;
                gx[nxs++] = oe.bx + oe.bw / 2;
                gx[nxs++] = oe.bx + oe.bw;
                gy[nys++] = oe.by;
                gy[nys++] = oe.by + oe.bh / 2;
                gy[nys++] = oe.by + oe.bh;
            }
            nx = HudLayout.snap1(nx, e.bw, gx, nxs, SNAP, hit);
            guideX = hit[0];
            ny = HudLayout.snap1(ny, e.bh, gy, nys, SNAP, hit);
            guideY = hit[0];
        }
        if (root.k.client.hudGrid.on()) {
            float cell = root.k.client.gridSize.floatValue();
            if (Float.isNaN(guideX)) nx = HudLayout.grid(nx, cell);
            if (Float.isNaN(guideY)) ny = HudLayout.grid(ny, cell);
        }
        HudLayout.moveTo(e, nx, ny, sw, sh);
        return true;
    }

    @Override
    public boolean mouseReleased(Ui ui, int button) {
        boolean was = dragging || resizing;
        dragging = resizing = false;
        guideX = guideY = Float.NaN;
        if (was) root.k.config.markDirty();
        return was;
    }

    @Override
    public boolean keyPressed(Ui ui, int key, int mods) {
        boolean ctrl = (mods & (Keys.MOD_CONTROL | Keys.MOD_SUPER)) != 0;
        if (ctrl && key == Keys.Z) {
            if ((mods & Keys.MOD_SHIFT) != 0) redo();
            else undo();
            return true;
        }
        if (ctrl && key == 'Y') {
            redo();
            return true;
        }
        if (selected == null || !selected.active()) return false;
        HudElement e = root.k.hud.element((HudModule) selected.module);
        float s = menuScale(), step = (mods & Keys.MOD_SHIFT) != 0 ? 10 : 1;
        float dx = key == Keys.LEFT ? -step : key == Keys.RIGHT ? step : 0;
        float dy = key == Keys.UP ? -step : key == Keys.DOWN ? step : 0;
        if (dx != 0 || dy != 0) {
            snapshot();
            HudLayout.moveTo(e, e.bx + dx, e.by + dy, root.width() * s, root.height() * s);
            root.k.config.markDirty();
            return true;
        }
        if (key == Keys.DELETE || key == Keys.BACKSPACE) {
            snapshot();
            root.k.modules.setEnabled(selected, false);
            selected = null;
            return true;
        }
        return false;
    }

    // ------------------------------------------------------------------ undo / redo

    private Map<String, Map<String, Object>> capture() {
        Map<String, Map<String, Object>> m = new HashMap<String, Map<String, Object>>();
        for (ModuleManager.State st : root.k.modules.all()) {
            if (!(st.module instanceof HudModule)) continue;
            Map<String, Object> e = new HashMap<String, Object>(root.k.hud.element((HudModule) st.module).toJson());
            e.put("__enabled", st.enabled());
            m.put(st.module.id(), e);
        }
        return m;
    }

    private void restore(Map<String, Map<String, Object>> snap) {
        for (Map.Entry<String, Map<String, Object>> en : snap.entrySet()) {
            ModuleManager.State st = root.k.modules.get(en.getKey());
            if (st == null || !(st.module instanceof HudModule)) continue;
            root.k.hud.element((HudModule) st.module).fromJson(en.getValue());
            Object on = en.getValue().get("__enabled");
            if (on instanceof Boolean) root.k.modules.setEnabled(st, (Boolean) on);
        }
        root.k.config.markDirty();
    }

    void snapshot() {
        undo.add(capture());
        if (undo.size() > UNDO_DEPTH) undo.remove(0);
        redo.clear();
    }

    private void undo() {
        if (undo.isEmpty()) return;
        redo.add(capture());
        restore(undo.remove(undo.size() - 1));
    }

    private void redo() {
        if (redo.isEmpty()) return;
        undo.add(capture());
        restore(redo.remove(redo.size() - 1));
    }

    // ------------------------------------------------------------------ style popup

    private void openStyle(Ui ui, final ModuleManager.State st, final HudElement e) {
        snapshot();
        final float s = menuScale();
        final Runnable dirty = new Runnable() {
            @Override
            public void run() {
                root.k.config.markDirty();
            }
        };
        List<Setting<?>> list = new ArrayList<Setting<?>>();
        final NumberSetting scale = num(list, "Scale", e.scale, 0.25, 4, 0.05, "x");
        scale.addListener(new Runnable() {
            @Override
            public void run() {
                e.scale = scale.floatValue();
                dirty.run();
            }
        });
        final NumberSetting opacity = num(list, "Opacity", e.opacity, 0.05, 1, 0.05, "");
        opacity.addListener(new Runnable() {
            @Override
            public void run() {
                e.opacity = opacity.floatValue();
                dirty.run();
            }
        });
        final BoolSetting bg = bool(list, "Background", e.background);
        bg.addListener(new Runnable() {
            @Override
            public void run() {
                e.background = bg.on();
                dirty.run();
            }
        });
        final ColorSetting bgC = color(list, "Background colour", e.bgColor);
        bgC.addListener(new Runnable() {
            @Override
            public void run() {
                e.bgColor = bgC.get();
                dirty.run();
            }
        });
        final BoolSetting border = bool(list, "Border", e.border);
        border.addListener(new Runnable() {
            @Override
            public void run() {
                e.border = border.on();
                dirty.run();
            }
        });
        final ColorSetting borderC = color(list, "Border colour", e.borderColor);
        borderC.addListener(new Runnable() {
            @Override
            public void run() {
                e.borderColor = borderC.get();
                dirty.run();
            }
        });
        final BoolSetting shadow = bool(list, "Text shadow", e.shadow);
        shadow.addListener(new Runnable() {
            @Override
            public void run() {
                e.shadow = shadow.on();
                dirty.run();
            }
        });
        final ColorSetting text = color(list, "Text colour", e.textColor);
        text.addListener(new Runnable() {
            @Override
            public void run() {
                e.textColor = text.get();
                dirty.run();
            }
        });
        final ColorSetting accent = color(list, "Accent colour", e.accentColor);
        accent.addListener(new Runnable() {
            @Override
            public void run() {
                e.accentColor = accent.get();
                dirty.run();
            }
        });
        final NumberSetting radius = num(list, "Corner radius", e.radius, 0, 12, 0.5, "");
        radius.addListener(new Runnable() {
            @Override
            public void run() {
                e.radius = radius.floatValue();
                dirty.run();
            }
        });
        final NumberSetting padding = num(list, "Padding", e.padding, 0, 12, 0.5, "");
        padding.addListener(new Runnable() {
            @Override
            public void run() {
                e.padding = padding.floatValue();
                dirty.run();
            }
        });
        final BoolSetting auto = bool(list, "Auto anchor", e.autoAnchor);
        auto.addListener(new Runnable() {
            @Override
            public void run() {
                e.autoAnchor = auto.on();
                dirty.run();
            }
        });
        final BoolSetting pct = bool(list, "Offsets in % of screen", e.percent);
        pct.addListener(new Runnable() {
            @Override
            public void run() {
                float bx = e.bx, by = e.by;
                e.percent = pct.on();
                HudLayout.moveTo(e, bx, by, root.width() * s, root.height() * s);
                dirty.run();
            }
        });
        final ScrollList rows = new ScrollList();
        rows.gap = 1;
        rows.add(new AnchorGrid(e, s, auto));
        for (Setting<?> set : list) rows.add(new SettingRow(set));
        Widget panel = new StylePanel(st, rows);
        float pw = 190, ph = Math.min(root.height() - 12, 300);
        float px = Math.min(root.width() - pw - 6, (e.bx + e.bw) / s + 8);
        if (px < 6) px = 6;
        float py = Math.max(6, Math.min(root.height() - ph - 6, e.by / s));
        root.openPopup(null, panel.bounds(px, py, pw, ph));
    }

    private static NumberSetting num(List<Setting<?>> l, String name, double v, double min, double max, double step, String suffix) {
        NumberSetting s = new NumberSetting(id(name), name, "", v, min, max, step, suffix);
        l.add(s);
        return s;
    }

    private static BoolSetting bool(List<Setting<?>> l, String name, boolean v) {
        BoolSetting s = new BoolSetting(id(name), name, "", v);
        l.add(s);
        return s;
    }

    private static ColorSetting color(List<Setting<?>> l, String name, int v) {
        ColorSetting s = new ColorSetting(id(name), name, "", v);
        l.add(s);
        return s;
    }

    private static String id(String name) {
        return name.toLowerCase().replaceAll("[^a-z0-9]+", "_");
    }

    private final class StylePanel extends Widget {
        private final ModuleManager.State st;
        private final ScrollList rows;
        private final Button settingsB, resetB;

        StylePanel(final ModuleManager.State st, ScrollList rows) {
            this.st = st;
            this.rows = rows;
            settingsB = new Button("Module settings", Button.Style.SECONDARY, new Runnable() {
                @Override
                public void run() {
                    root.closeHudEditor();
                    ModsPage mp = root.page(ModsPage.class);
                    root.show(mp);
                    mp.openSettings(st);
                }
            });
            resetB = new Button("Reset", Button.Style.DANGER, new Runnable() {
                @Override
                public void run() {
                    snapshot();
                    root.k.hud.resetElement((HudModule) st.module);
                    root.closePopup();
                    root.k.config.markDirty();
                }
            });
        }

        @Override
        public void render(Ui ui) {
            ui.g.roundRect(x, y, w, h, 6, ui.t.panel);
            ui.g.roundOutline(x, y, w, h, 6, 1, ui.t.border);
            ui.g.text(st.module.name(), x + 8, y + 7, ui.t.text, false);
            rows.bounds(x + 4, y + 20, w - 8, h - 44);
            rows.render(ui);
            settingsB.bounds(x + 6, y + h - 21, w - 66, 15).render(ui);
            resetB.bounds(x + w - 56, y + h - 21, 50, 15).render(ui);
        }

        @Override
        public boolean mouseClicked(Ui ui, int button) {
            return settingsB.mouseClicked(ui, button) || resetB.mouseClicked(ui, button) || rows.mouseClicked(ui, button) || true;
        }

        @Override
        public boolean mouseScrolled(Ui ui, double amount) {
            return rows.mouseScrolled(ui, amount);
        }
    }

    /** 3x3 anchor picker. */
    private final class AnchorGrid extends Widget {
        private final HudElement e;
        private final float s;
        private final BoolSetting auto;

        AnchorGrid(HudElement e, float s, BoolSetting auto) {
            this.e = e;
            this.s = s;
            this.auto = auto;
            this.h = 38;
        }

        @Override
        public void render(Ui ui) {
            ui.g.text("Anchor", x + 6, y + 4, ui.t.text, false);
            float gx0 = x + w - 44, gy0 = y + 2;
            ui.g.roundRect(gx0 - 2, gy0 - 2, 40, 36, 3, ui.t.surface2);
            for (int i = 0; i < 9; i++) {
                float cx = gx0 + (i % 3) * 12, cy = gy0 + (i / 3) * 11;
                boolean sel = e.anchor.ordinal() == i, hv = ui.hover(cx, cy, 10, 9);
                ui.g.roundRect(cx, cy, 10, 9, 2, sel ? ui.t.accent : hv ? Colors.fade(ui.t.text, 0.4f) : Colors.fade(ui.t.text, 0.15f));
            }
            ui.g.text(e.anchor.name().toLowerCase().replace('_', '-'), x + 6, y + 16, ui.t.textDim, false);
            if (ui.hover(x, y, w, h)) ui.tooltip = "Which screen point the element is measured from (turn off Auto anchor to keep it)";
        }

        @Override
        public boolean mouseClicked(Ui ui, int button) {
            float gx0 = x + w - 44, gy0 = y + 2;
            for (int i = 0; i < 9; i++) {
                float cx = gx0 + (i % 3) * 12, cy = gy0 + (i / 3) * 11;
                if (ui.hover(cx, cy, 10, 9)) {
                    // A manual anchor only sticks with auto-anchor off (auto would re-pick on the next drag).
                    auto.set(false);
                    float bx = e.bx, by = e.by;
                    e.anchor = Anchor.values()[i];
                    HudLayout.moveTo(e, bx, by, root.width() * s, root.height() * s);
                    root.k.config.markDirty();
                    return true;
                }
            }
            return false;
        }
    }
}
