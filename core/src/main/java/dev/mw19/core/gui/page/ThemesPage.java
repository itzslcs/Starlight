package dev.mw19.core.gui.page;

import dev.mw19.api.setting.Setting;
import dev.mw19.api.util.Colors;
import dev.mw19.core.gui.GuiRoot;
import dev.mw19.core.gui.Theme;
import dev.mw19.core.gui.Ui;
import dev.mw19.core.gui.Widget;
import dev.mw19.core.gui.widget.ScrollList;
import dev.mw19.core.gui.widget.SettingRow;

/** Theme presets, accent colour and menu behaviour. */
public final class ThemesPage extends Page {
    private final ScrollList list = new ScrollList();

    public ThemesPage(GuiRoot root) {
        super(root);
        list.gap = 2;
        list.add(new Presets());
        for (Setting<?> s : root.k.client.all()) {
            if (s == root.k.client.theme || s == root.k.client.competitiveSafe) continue;
            list.add(new SettingRow(s));
        }
    }

    @Override
    public String title() {
        return "Themes";
    }

    @Override
    public String icon() {
        return "themes";
    }

    @Override
    public void render(Ui ui) {
        heading(ui, "Themes & Menu", "Presets, accent colour, blur, scale and animations");
        list.bounds(x, y + 28, w, h - 28);
        list.render(ui);
    }

    @Override
    public boolean mouseClicked(Ui ui, int button) {
        return list.mouseClicked(ui, button);
    }

    @Override
    public boolean mouseScrolled(Ui ui, double amount) {
        return list.mouseScrolled(ui, amount);
    }

    @Override
    public void reveal(Widget w) {
        list.reveal(w);
    }

    private final class Presets extends Widget {
        Presets() {
            h = 50;
        }

        @Override
        public void render(Ui ui) {
            int n = Theme.PRESETS.length;
            float cw = (w - (n - 1) * 4) / n;
            for (int i = 0; i < n; i++) {
                Theme t = Theme.preset(Theme.PRESETS[i]);
                float cx = x + i * (cw + 4);
                boolean sel = root.k.client.theme.is(t.name), hv = ui.hover(cx, y, cw, h);
                ui.g.roundRect(cx, y, cw, h, 5, t.panel);
                ui.g.roundRect(cx + 4, y + 4, cw - 8, 12, 3, t.surface);
                ui.g.roundRect(cx + 4, y + 20, (cw - 8) * 0.6f, 6, 3, t.accent);
                ui.g.roundRect(cx + 4, y + 29, (cw - 8) * 0.8f, 4, 2, Colors.fade(t.text, 0.5f));
                ui.g.textCentered(t.name, cx + cw / 2f, y + h - 12, t.text, false);
                if (sel || hv) ui.g.roundOutline(cx, y, cw, h, 5, 1, sel ? ui.t.accent : ui.t.border);
            }
        }

        @Override
        public boolean mouseClicked(Ui ui, int button) {
            int n = Theme.PRESETS.length;
            float cw = (w - (n - 1) * 4) / n;
            for (int i = 0; i < n; i++) {
                if (ui.hover(x + i * (cw + 4), y, cw, h)) {
                    root.k.client.theme.set(Theme.PRESETS[i]);
                    return true;
                }
            }
            return false;
        }
    }
}
