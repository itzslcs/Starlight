package dev.mw19.core.gui.page;

import dev.mw19.api.module.Category;
import dev.mw19.api.module.HudModule;
import dev.mw19.api.module.Module;
import dev.mw19.api.module.Rule;
import dev.mw19.api.setting.Setting;
import dev.mw19.api.util.Colors;
import dev.mw19.core.Keys;
import dev.mw19.core.gui.Anim;
import dev.mw19.core.gui.GuiRoot;
import dev.mw19.core.gui.Icons;
import dev.mw19.core.gui.Ui;
import dev.mw19.core.gui.Widget;
import dev.mw19.core.gui.widget.Button;
import dev.mw19.core.gui.widget.ScrollList;
import dev.mw19.core.gui.widget.SettingRow;
import dev.mw19.core.gui.widget.TextField;
import dev.mw19.core.gui.widget.Toggle;
import dev.mw19.core.module.ModuleManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Module browser: search, category chips, card grid; settings view per module. */
public final class ModsPage extends Page {
    private static final String ALL = "All", FAV = "Favorites";
    private final TextField search = new TextField("");
    private String category = ALL;
    private final List<String> chips = new ArrayList<String>();
    private final ScrollList grid = new ScrollList();
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
        grid.clear();
        String q = search.text.trim().toLowerCase(Locale.ROOT);
        List<ModuleManager.State> matches = new ArrayList<ModuleManager.State>();
        for (ModuleManager.State s : root.k.modules.all()) {
            Module m = s.module;
            if ((s.suspend() & ModuleManager.SUSPEND_UNAVAILABLE) != 0) continue;
            if (FAV.equals(category) && !s.favorite()) continue;
            if (!ALL.equals(category) && !FAV.equals(category) && !m.category().label().equals(category)) continue;
            if (!q.isEmpty() && !(m.name().toLowerCase(Locale.ROOT).contains(q) || m.description().toLowerCase(Locale.ROOT).contains(q)
                    || m.id().contains(q))) continue;
            matches.add(s);
        }
        for (int i = 0; i < matches.size(); i += 2) {
            grid.add(new CardRow(matches.get(i), i + 1 < matches.size() ? matches.get(i + 1) : null));
        }
    }

    @Override
    public void render(Ui ui) {
        if (open != null) {
            renderSettings(ui);
            return;
        }
        search.bounds(x, y, w, 16).render(ui);
        float cx = x, cy = y + 21;
        for (String c : chips) {
            float cw = ui.g.textWidth(c) + 12;
            if (cx + cw > x + w) break;
            boolean sel = c.equals(category), hv = ui.hover(cx, cy, cw, 14);
            ui.g.roundRect(cx, cy, cw, 14, 7, sel ? ui.t.accent : hv ? ui.t.surface2 : Colors.fade(ui.t.surface2, 0.6f));
            ui.g.text(c, cx + 6, cy + 3, sel ? ui.t.onAccent : ui.t.text, false);
            cx += cw + 4;
        }
        grid.bounds(x, y + 40, w, h - 40);
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
        float cx = x, cy = y + 21;
        for (String c : chips) {
            float cw = ui.g.textWidth(c) + 12;
            if (cx + cw > x + w) break;
            if (ui.hover(cx, cy, cw, 14)) {
                category = c;
                rebuild();
                return true;
            }
            cx += cw + 4;
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
        for (dev.mw19.core.gui.Widget w : grid.children) {
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
}
