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
        private static final int PER_ROW = 5;
        private static final float CARD_H = 46;

        Presets() {
            int rows = (Theme.PRESETS.length + PER_ROW - 1) / PER_ROW;
            h = rows * CARD_H + (rows - 1) * 4;
        }

        private float cw() {
            return (w - (PER_ROW - 1) * 4) / PER_ROW;
        }

        private float cx(int i) {
            return x + (i % PER_ROW) * (cw() + 4);
        }

        private float cy(int i) {
            return y + (i / PER_ROW) * (CARD_H + 4);
        }

        @Override
        public void render(Ui ui) {
            float cw = cw();
            for (int i = 0; i < Theme.PRESETS.length; i++) {
                Theme t = Theme.preset(Theme.PRESETS[i]);
                float cx = cx(i), cy = cy(i);
                boolean sel = root.k.client.theme.is(t.name), hv = ui.hover(cx, cy, cw, CARD_H);
                ui.g.roundRect(cx, cy, cw, CARD_H, 5, t.panel);
                ui.g.roundRect(cx + 4, cy + 4, cw - 8, 11, 3, t.surface);
                ui.g.roundRect(cx + 4, cy + 18, (cw - 8) * 0.6f, 6, 3, t.accent);
                ui.g.roundRect(cx + 4, cy + 27, (cw - 8) * 0.8f, 4, 2, Colors.fade(t.text, 0.5f));
                ui.g.textCentered(t.name, cx + cw / 2f, cy + CARD_H - 11, t.text, false);
                if (sel || hv) ui.g.roundOutline(cx, cy, cw, CARD_H, 5, 1, sel ? ui.t.accent : ui.t.border);
            }
        }

        @Override
        public boolean mouseClicked(Ui ui, int button) {
            for (int i = 0; i < Theme.PRESETS.length; i++) {
                if (ui.hover(cx(i), cy(i), cw(), CARD_H)) {
                    root.k.client.theme.set(Theme.PRESETS[i]);
                    return true;
                }
            }
            return false;
        }
    }
}
