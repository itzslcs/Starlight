package dev.starlight.core.gui.page;

import dev.starlight.api.module.Category;
import dev.starlight.api.module.HudModule;
import dev.starlight.api.module.Module;
import dev.starlight.api.module.Rule;
import dev.starlight.api.setting.Setting;
import dev.starlight.api.util.Colors;
import dev.starlight.core.Keys;
import dev.starlight.core.gui.Anim;
import dev.starlight.core.gui.GuiRoot;
import dev.starlight.core.gui.Icons;
import dev.starlight.core.gui.Ui;
import dev.starlight.core.gui.Widget;
import dev.starlight.core.gui.widget.Button;
import dev.starlight.core.gui.widget.ScrollList;
import dev.starlight.core.gui.widget.SettingRow;
import dev.starlight.core.gui.widget.TextField;
import dev.starlight.core.gui.widget.Toggle;
import dev.starlight.core.module.ModuleManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Module browser: search, category chips (a highlight slides between them), then either tiles (default: item icon,
 * name and an ENABLED/DISABLED bar that toggles, entering with a short stagger) or the detailed list; settings view per
 * module. The view choice is kept in the config ("ui" section).
 */
public final class ModsPage extends Page {
    private static final String ALL = "All", FAV = "Favorites";
    private static final float TILE_MIN_W = 84, TILE_H = 74, TILE_GAP = 6;
    private final TextField search = new TextField("");
    private String category = ALL;
    private final List<String> chips = new ArrayList<String>();
    private final ScrollList grid = new ScrollList();
    private final List<ModuleManager.State> matches = new ArrayList<ModuleManager.State>();
    private boolean tiles;
    private int builtCols = -1;
    private final Anim chipX = new Anim(-1), chipY = new Anim(0), chipW = new Anim(0);
    private ModuleManager.State open;
    private final ScrollList settings = new ScrollList();
    private Toggle openToggle;
    private final Button back, reset, editHud;

    public ModsPage(final GuiRoot root) {
        super(root);
        search.placeholder = "Search modules…";
        search.onChange = new TextField.Listener() {
            @Override
            public void changed(String text) {
                rebuild();
            }
        };
        grid.gap = 0;
        settings.gap = 2;
        tiles = !"list".equals(root.k.config.section("ui").get("modsView"));
        back = new Button("", Button.Style.GHOST, new Runnable() {
            @Override
            public void run() {
                open = null;
            }
        }).icon("back");
        back.tooltip = "Back to modules";
        reset = new Button("Reset", Button.Style.SECONDARY, new Runnable() {
            @Override
            public void run() {
                if (open == null) return;
                for (Setting<?> s : open.module.settings()) s.reset();
                if (open.module instanceof HudModule) root.k.hud.resetElement((HudModule) open.module);
                root.k.toast(open.module.name(), "Settings reset to defaults", root.k.theme.accent);
            }
        });
        reset.tooltip = "Reset this module's settings and HUD position (right-click a setting to reset just that one)";
        editHud = new Button("Position", Button.Style.SECONDARY, new Runnable() {
            @Override
            public void run() {
                root.openHudEditor();
            }
        }).icon("hud");
    }

    @Override
    public String title() {
        return "Mods";
    }

    @Override
    public String icon() {
        return "mods";
    }

    @Override
    public void onShow() {
        rebuild();
    }

    public void openSettings(ModuleManager.State s) {
        open = s;
        settings.clear();
        for (Setting<?> set : s.module.settings()) settings.add(new SettingRow(set));
        final ModuleManager.State st = s;
        final ModuleManager mm = root.k.modules;
        openToggle = new Toggle(new Toggle.Model() {
            @Override
            public boolean get() {
                return st.enabled();
            }

            @Override
            public void set(boolean v) {
                mm.setEnabled(st, v);
            }
        });
    }

    private void rebuild() {
        chips.clear();
        chips.add(ALL);
        chips.add(FAV);
        for (Category c : Category.values()) {
            for (ModuleManager.State s : root.k.modules.all()) {
                if (s.module.category() == c && (s.suspend() & ModuleManager.SUSPEND_UNAVAILABLE) == 0) {
                    chips.add(c.label());
                    break;
                }
            }
        }
        if (!chips.contains(category)) category = ALL;
        String q = search.text.trim().toLowerCase(Locale.ROOT);
        matches.clear();
        for (ModuleManager.State s : root.k.modules.all()) {
            Module m = s.module;
            if ((s.suspend() & ModuleManager.SUSPEND_UNAVAILABLE) != 0) continue;
            if (FAV.equals(category) && !s.favorite()) continue;
            if (!ALL.equals(category) && !FAV.equals(category) && !m.category().label().equals(category)) continue;
            if (!q.isEmpty() && !(m.name().toLowerCase(Locale.ROOT).contains(q) || m.description().toLowerCase(Locale.ROOT).contains(q)
                    || m.id().contains(q))) continue;
            matches.add(s);
        }
        layoutRows(tiles ? columns() : 2);
    }

    private int columns() {
        float avail = grid.w > 0 ? grid.w - 6 : w;
        return Math.max(2, (int) ((avail + TILE_GAP) / (TILE_MIN_W + TILE_GAP)));
    }

    private void layoutRows(int cols) {
        builtCols = cols;
        grid.clear();
        if (tiles) {
            for (int i = 0; i < matches.size(); i += cols) grid.add(new TileRow(i, cols));
        } else {
            for (int i = 0; i < matches.size(); i += 2) {
                grid.add(new CardRow(matches.get(i), i + 1 < matches.size() ? matches.get(i + 1) : null));
            }
        }
    }

    private void setView(boolean asTiles) {
        tiles = asTiles;
        root.k.config.section("ui").put("modsView", asTiles ? "tiles" : "list");
        root.k.config.markDirty();
        layoutRows(tiles ? columns() : 2);
    }

    /** Chip positions (relative to the page), wrapped onto more rows when they do not fit; filled by layoutChips. */
    private float[] chipPos = new float[0];
    private int chipRows = 1;

    /** Lays the chips out left to right (the view switch keeps the end of the first row) and returns the rows used. */
    private int layoutChips(Ui ui) {
        if (chipPos.length < chips.size() * 3) chipPos = new float[chips.size() * 3];
        float cx = 0, cy = 0, room = w - 20;
        int rows = 1;
        for (int i = 0; i < chips.size(); i++) {
            float cw = ui.g.textWidth(chips.get(i)) + 12;
            if (cx > 0 && cx + cw > room) {
                cx = 0;
                cy += 17;
                room = w;
                rows++;
            }
            chipPos[i * 3] = cx;
            chipPos[i * 3 + 1] = cy;
            chipPos[i * 3 + 2] = cw;
            cx += cw + 4;
        }
        return chipRows = rows;
    }

    /** Top of the module grid below the chip rows. */
    private float gridTop() {
        return 40 + (chipRows - 1) * 17;
    }

    @Override
    public void render(Ui ui) {
        if (open != null) {
            renderSettings(ui);
            return;
        }
        search.bounds(x, y, w, 16).render(ui);
        layoutChips(ui);
        float top = y + 21;
        for (int i = 0; i < chips.size(); i++) { // the selected chip's highlight glides to it
            if (!chips.get(i).equals(category)) continue;
            float cx = x + chipPos[i * 3], cy = top + chipPos[i * 3 + 1];
            if (chipX.target() < 0) {
                chipX.snap(cx);
                chipY.snap(cy);
            }
            chipX.to(cx, 160, ui.now);
            chipY.to(cy, 160, ui.now);
            chipW.to(chipPos[i * 3 + 2], 160, ui.now);
        }
        for (int i = 0; i < chips.size(); i++) {
            String c = chips.get(i);
            float cx = x + chipPos[i * 3], cy = top + chipPos[i * 3 + 1], cw = chipPos[i * 3 + 2];
            if (!c.equals(category)) ui.g.roundRect(cx, cy, cw, 14, 3, ui.hover(cx, cy, cw, 14) ? ui.t.surface2 : Colors.fade(ui.t.surface2, 0.6f));
        }
        ui.g.roundRect(chipX.get(ui.now), chipY.get(ui.now), chipW.get(ui.now), 14, 3, ui.t.accent);
        for (int i = 0; i < chips.size(); i++) {
            String c = chips.get(i);
            ui.g.text(c, x + chipPos[i * 3] + 6, top + chipPos[i * 3 + 1] + 3, c.equals(category) ? ui.t.onAccent : ui.t.text, false);
        }
        viewToggle(ui, x + w - 14, top);
        grid.bounds(x, y + gridTop(), w, h - gridTop());
        if (tiles && columns() != builtCols) layoutRows(columns());
        if (grid.children.isEmpty()) {
            ui.g.textCentered("No modules match", x + w / 2f, y + 80, ui.t.textDim, false);
        } else {
            grid.render(ui);
        }
    }

    private void renderSettings(Ui ui) {
        Module m = open.module;
        back.bounds(x, y, 18, 16).render(ui);
        ui.g.text(m.name(), x + 24, y + 1, 1.25f, ui.t.text, false);
        float nx = x + 24 + ui.g.textWidth(m.name()) * 1.25f + 6;
        badges(ui, open, nx, y + 1);
        openToggle.bounds(x + w - 26, y, 26, 16).render(ui);
        reset.bounds(x + w - 76, y, 44, 16).render(ui);
        if (m instanceof HudModule) editHud.bounds(x + w - 140, y, 58, 16).render(ui);
        ui.g.text(ui.g.ellipsize(m.description(), w), x, y + 21, ui.t.textDim, false);
        settings.bounds(x, y + 34, w, h - 34);
        if (settings.children.isEmpty()) ui.g.textCentered("This module has no settings", x + w / 2f, y + 70, ui.t.textDim, false);
        else settings.render(ui);
    }

    /** Grid / list switch at the end of the chip row: shows the view it switches to. */
    private void viewToggle(Ui ui, float bx, float by) {
        boolean hv = ui.hover(bx, by, 14, 14);
        ui.g.roundRect(bx, by, 14, 14, 3, hv ? ui.t.surface2 : Colors.fade(ui.t.surface2, 0.6f));
        int c = hv ? ui.t.text : ui.t.textDim;
        if (tiles) { // three list lines
            for (int i = 0; i < 3; i++) ui.g.rect(bx + 3, by + 3.5f + i * 3, bx + 11, by + 4.5f + i * 3, c);
        } else { // four tiles
            for (int i = 0; i < 4; i++) ui.g.rect(bx + 3 + (i % 2) * 4.5f, by + 3 + (i / 2) * 4.5f, bx + 6.5f + (i % 2) * 4.5f, by + 6.5f + (i / 2) * 4.5f, c);
        }
        if (hv) ui.tooltip = tiles ? "Show as a list (with descriptions)" : "Show as tiles";
    }

    /** Rule / suspension badges. Returns the x after the last badge. */
    static float badges(Ui ui, ModuleManager.State s, float bx, float by) {
        if (s.module.rule() == Rule.GRAY) bx = badge(ui, bx, by, "GRAY", ui.t.warn,
                "Not clearly inside a category of Hypixel's Allowed Modifications; off by default and disabled by Competitive-safe.");
        else if (s.module.rule() == Rule.DISALLOWED_ON_SOME_SERVERS) bx = badge(ui, bx, by, "RESTRICTED", ui.t.warn,
                "Disallowed on some servers (see Server Rules); blocked automatically there.");
        int sus = s.suspend();
        if ((sus & ModuleManager.SUSPEND_FAILED) != 0) bx = badge(ui, bx, by, "ERROR", ui.t.bad, "Disabled after repeated errors this session; see the log.");
        else if ((sus & ModuleManager.SUSPEND_SERVER) != 0) bx = badge(ui, bx, by, "BLOCKED", ui.t.bad, "Blocked by the rules for this server.");
        else if ((sus & ModuleManager.SUSPEND_SAFE) != 0) bx = badge(ui, bx, by, "SAFE-MODE", ui.t.textDim, "Paused by Competitive-safe.");
        return bx;
    }

    private static float badge(Ui ui, float bx, float by, String text, int color, String tip) {
        float bw = ui.g.textWidth(text) * 0.75f + 6;
        ui.g.roundRect(bx, by, bw, 9, 2, Colors.fade(color, 0.2f));
        ui.g.text(text, bx + 3, by + 1.5f, 0.75f, color, false);
        if (ui.hover(bx, by, bw, 9)) ui.tooltip = tip;
        return bx + bw + 3;
    }

    @Override
    public boolean mouseClicked(Ui ui, int button) {
        if (open != null) {
            if (back.mouseClicked(ui, button) || openToggle.mouseClicked(ui, button) || reset.mouseClicked(ui, button)) return true;
            if (open.module instanceof HudModule && editHud.mouseClicked(ui, button)) return true;
            return settings.mouseClicked(ui, button);
        }
        if (search.mouseClicked(ui, button)) return true;
        layoutChips(ui);
        float top = y + 21;
        for (int i = 0; i < chips.size(); i++) {
            if (ui.hover(x + chipPos[i * 3], top + chipPos[i * 3 + 1], chipPos[i * 3 + 2], 14)) {
                category = chips.get(i);
                rebuild();
                return true;
            }
        }
        if (ui.hover(x + w - 14, top, 14, 14)) {
            setView(!tiles);
            return true;
        }
        return grid.mouseClicked(ui, button);
    }

    @Override
    public boolean mouseScrolled(Ui ui, double amount) {
        return open != null ? settings.mouseScrolled(ui, amount) : grid.mouseScrolled(ui, amount);
    }

    @Override
    public boolean keyPressed(Ui ui, int key, int mods) {
        if (open != null && (key == Keys.ESCAPE || (key == Keys.BACKSPACE && root.focused() == null))) {
            open = null;
            return true;
        }
        if (key == 'F' && (mods & Keys.MOD_CONTROL) != 0) {
            root.focus(search);
            return true;
        }
        return false;
    }

    @Override
    public void reveal(Widget w) {
        (open != null ? settings : grid).reveal(w);
    }

    /** Centre of a visible card's toggle in menu units, or null. The smoke run clicks it through the real OS input path. */
    public float[] toggleCenter(String moduleId) {
        for (dev.starlight.core.gui.Widget w : grid.children) {
            if (w instanceof TileRow) {
                for (Tile t : ((TileRow) w).tiles) {
                    if (t.s.module.id().equals(moduleId) && t.w > 0) return new float[]{t.x + t.w / 2f, t.y + t.h - 6.5f};
                }
            }
            if (!(w instanceof CardRow)) continue;
            CardRow row = (CardRow) w;
            for (Card c : new Card[]{row.a, row.b}) {
                if (c != null && c.s.module.id().equals(moduleId) && c.toggle.w > 0) {
                    return new float[]{c.toggle.x + c.toggle.w / 2f, c.toggle.y + c.toggle.h / 2f};
                }
            }
        }
        return null;
    }

    /** Two module cards side by side. */
    private final class CardRow extends Widget {
        private final Card a, b;

        CardRow(ModuleManager.State a, ModuleManager.State b) {
            this.a = new Card(a);
            this.b = b == null ? null : new Card(b);
            this.h = 50;
        }

        @Override
        public void render(Ui ui) {
            float cw = (w - 6) / 2f;
            a.bounds(x, y, cw, 46).render(ui);
            if (b != null) b.bounds(x + cw + 6, y, cw, 46).render(ui);
        }

        @Override
        public boolean mouseClicked(Ui ui, int button) {
            return (a.contains(ui.mx, ui.my) && a.mouseClicked(ui, button)) || (b != null && b.contains(ui.mx, ui.my) && b.mouseClicked(ui, button));
        }
    }

    private final class Card extends Widget {
        private final ModuleManager.State s;
        private final Toggle toggle;
        private final Anim hover = new Anim(0);

        Card(final ModuleManager.State s) {
            this.s = s;
            final ModuleManager mm = root.k.modules;
            toggle = new Toggle(new Toggle.Model() {
                @Override
                public boolean get() {
                    return s.enabled();
                }

                @Override
                public void set(boolean v) {
                    mm.setEnabled(s, v);
                }
            });
        }

        @Override
        public void render(Ui ui) {
            boolean hv = hovered(ui);
            hover.to(hv ? 1 : 0, 150, ui.now);
            float t = hover.get(ui.now);
            ui.g.roundRect(x, y, w, h, 5, Colors.lerp(ui.t.surface, ui.t.surface2, t));
            if (s.enabled()) ui.g.roundOutline(x, y, w, h, 5, 1, ui.t.accentSoft(s.active() ? 0.55f : 0.25f));
            ui.g.text(ui.g.ellipsize(s.module.name(), w - 40), x + 7, y + 6, ui.t.text, false);
            ui.g.text(ui.g.ellipsize(s.module.description(), w - 14), x + 7, y + 18, ui.t.textDim, false);
            toggle.bounds(x + w - 30, y + 4, 24, 12).render(ui);
            badges(ui, s, x + 7, y + 32);
            float gx = x + w - 16, sx = x + w - 30, iy = y + 31;
            boolean gh = ui.hover(gx - 2, iy - 2, 14, 14), sh = ui.hover(sx - 2, iy - 2, 14, 14);
            if (!s.module.settings().isEmpty()) Icons.draw(ui.g, "gear", gx, iy, gh ? ui.t.accent : ui.t.textDim);
            Icons.draw(ui.g, s.favorite() ? "star_on" : "star", sx, iy, s.favorite() ? ui.t.warn : sh ? ui.t.text : ui.t.textDim);
            if (gh) ui.tooltip = "Settings";
            else if (sh) ui.tooltip = s.favorite() ? "Remove from favorites" : "Add to favorites";
            else if (hv && !toggle.contains(ui.mx, ui.my)) ui.tooltip = s.module.description() + " (click to toggle, right-click for settings)";
        }

        @Override
        public boolean mouseClicked(Ui ui, int button) {
            float gx = x + w - 16, sx = x + w - 30, iy = y + 31;
            if (toggle.mouseClicked(ui, button)) return true;
            if (ui.hover(gx - 2, iy - 2, 14, 14) && !s.module.settings().isEmpty() || button == 1) {
                openSettings(s);
                return true;
            }
            if (ui.hover(sx - 2, iy - 2, 14, 14)) {
                root.k.modules.setFavorite(s, !s.favorite());
                if (FAV.equals(category)) rebuild();
                return true;
            }
            if (button == 0) {
                root.k.modules.setEnabled(s, !s.enabled());
                return true;
            }
            return false;
        }
    }

    /** One row of tiles (fixed height, so the scroll list can lay it out before drawing). */
    private final class TileRow extends Widget {
        private final List<Tile> tiles = new ArrayList<Tile>();
        private final int cols;

        TileRow(int first, int cols) {
            this.cols = cols;
            for (int i = first; i < first + cols && i < matches.size(); i++) tiles.add(new Tile(matches.get(i), i));
            this.h = TILE_H + TILE_GAP;
        }

        @Override
        public void render(Ui ui) {
            float tw = (w - TILE_GAP * (cols - 1)) / cols;
            for (int i = 0; i < tiles.size(); i++) tiles.get(i).place(x + i * (tw + TILE_GAP), y, tw, TILE_H).draw(ui);
        }

        @Override
        public boolean mouseClicked(Ui ui, int button) {
            for (Tile t : tiles) if (t.contains(ui.mx, ui.my)) return t.click(ui, button);
            return false;
        }
    }

    /** A module as a tile: star and gear on top, its item icon, name, and the ENABLED / DISABLED bar. */
    private final class Tile {
        private final ModuleManager.State s;
        private final int index;
        private final Anim hover = new Anim(0), on, appear = new Anim(0);
        private dev.starlight.api.render.ItemRef icon;
        private boolean started;
        float x, y, w, h;

        Tile(ModuleManager.State s, int index) {
            this.s = s;
            this.index = index;
            this.on = new Anim(s.enabled() ? 1 : 0);
        }

        Tile place(float x, float y, float w, float h) {
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
            return this;
        }

        boolean contains(float px, float py) {
            return px >= x && py >= y && px < x + w && py < y + h;
        }

        private dev.starlight.api.render.ItemRef icon() {
            if (icon == null) {
                for (String id : dev.starlight.core.gui.ModIcons.of(s.module.id())) {
                    icon = root.k.platform.itemIcon(id);
                    if (!icon.isEmpty()) break;
                }
                if (icon == null) icon = root.k.platform.itemIcon("minecraft:air");
            }
            return icon;
        }

        void draw(Ui ui) {
            if (!started) { // tiles arrive one after another (a few ms apart) when the list changes
                started = true;
                appear.to(1, 220, ui.now + Math.min(index, 24) * 14L);
            }
            float a = appear.get(ui.now);
            if (a <= 0.01f) return;
            boolean hv = contains(ui.mx, ui.my);
            hover.to(hv ? 1 : 0, 120, ui.now);
            on.to(s.enabled() ? 1 : 0, 160, ui.now);
            float t = hover.get(ui.now), o = on.get(ui.now), ty = y + (1 - a) * 8 - t * 1.5f;
            ui.g.pushAlpha(a);
            ui.g.roundRect(x, ty, w, h, 4, Colors.lerp(ui.t.surface, ui.t.surface2, t));
            ui.g.roundOutline(x, ty, w, h, 4, 1, Colors.lerp(Colors.fade(ui.t.border, 0.5f + t * 0.5f), ui.t.accentSoft(0.45f + t * 0.4f), o));
            boolean starHv = ui.hover(x + 3, y + 3, 13, 13), gearHv = ui.hover(x + w - 16, y + 3, 13, 13); // as click() tests
            Icons.draw(ui.g, s.favorite() ? "star_on" : "star", x + 5, ty + 5, s.favorite() ? ui.t.warn : starHv ? ui.t.text : Colors.fade(ui.t.textDim, 0.4f + t * 0.6f));
            if (!s.module.settings().isEmpty()) Icons.draw(ui.g, "gear", x + w - 14, ty + 5, gearHv ? ui.t.accent : Colors.fade(ui.t.textDim, 0.4f + t * 0.6f));
            float cx = x + w / 2f, iy = ty + 12;
            dev.starlight.api.render.ItemRef it = icon();
            if (!it.isEmpty()) {
                ui.g.push();
                ui.g.translate(cx - 12, iy);
                ui.g.scale(1.5f);
                ui.g.item(it, 0, 0);
                ui.g.pop();
            } else {
                ui.g.roundRect(cx - 12, iy, 24, 24, 4, ui.t.surface2);
                ui.g.textCentered(s.module.name().substring(0, 1), cx, iy + 8, ui.t.text, false);
            }
            ui.g.textCentered(ui.g.ellipsize(s.module.name(), w - 8), cx, iy + 29, ui.t.text, false);
            float bh = 13, by = ty + h - bh;
            int bar = Colors.lerp(Colors.fade(ui.t.bad, 0.35f + t * 0.2f), Colors.fade(ui.t.good, 0.75f + t * 0.25f), o);
            ui.g.roundRect(x, by, w, bh, 4, bar);
            ui.g.rect(x, by, x + w, by + 4, bar); // square top edge
            String label = !s.enabled() ? "DISABLED" : s.active() ? "ENABLED" : "PAUSED";
            ui.g.textCentered(label, cx, by + 3, o > 0.5f ? 0xFFFFFFFF : ui.t.text, false);
            ui.g.popAlpha();
            if (starHv) ui.tooltip = s.favorite() ? "Remove from favorites" : "Add to favorites";
            else if (gearHv && !s.module.settings().isEmpty()) ui.tooltip = "Settings";
            else if (hv && !s.active() && s.enabled()) ui.tooltip = "Paused: " + pauseReason(s);
            else if (hv) ui.tooltip = s.module.description() + " (click to toggle, right-click for settings)";
        }

        boolean click(Ui ui, int button) {
            if (ui.hover(x + 3, y + 3, 13, 13) && button == 0) {
                root.k.modules.setFavorite(s, !s.favorite());
                if (FAV.equals(category)) rebuild();
                return true;
            }
            if (button == 1 || ui.hover(x + w - 16, y + 3, 13, 13) && !s.module.settings().isEmpty()) {
                openSettings(s);
                return true;
            }
            if (button == 0) {
                root.k.modules.setEnabled(s, !s.enabled());
                return true;
            }
            return false;
        }
    }

    private static String pauseReason(ModuleManager.State s) {
        int sus = s.suspend();
        if ((sus & ModuleManager.SUSPEND_FAILED) != 0) return "turned off after repeated errors (see the log)";
        if ((sus & ModuleManager.SUSPEND_SERVER) != 0) return "not allowed on this server";
        if ((sus & ModuleManager.SUSPEND_SAFE) != 0) return "Competitive-safe is on";
        return "waiting for the game";
    }
}
