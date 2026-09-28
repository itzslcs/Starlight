package dev.starlight.core.gui.page;

import dev.starlight.api.setting.Setting;
import dev.starlight.api.util.Colors;
import dev.starlight.core.gui.Anim;
import dev.starlight.core.gui.GuiRoot;
import dev.starlight.core.gui.MenuStyle;
import dev.starlight.core.gui.Theme;
import dev.starlight.core.gui.Ui;
import dev.starlight.core.gui.Widget;
import dev.starlight.core.gui.widget.ScrollList;
import dev.starlight.core.gui.widget.SettingRow;

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
        heading(ui, "Themes & Menu", "Each theme has its own animated scene; also the accent colour, blur, scale and animations");
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

    /** Every theme as a card showing its live scene and a sample keycap (the preset's own colours). */
    private final class Presets extends Widget {
        private static final int PER_ROW = 5;
        private static final float CARD_H = 58;
        private final Theme[] themes = new Theme[Theme.PRESETS.length];

        Presets() {
            for (int i = 0; i < themes.length; i++) themes[i] = Theme.preset(Theme.PRESETS[i]);
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
            for (int i = 0; i < themes.length; i++) {
                Theme t = themes[i];
                float cx = cx(i), cy = cy(i);
                boolean sel = root.k.client.theme.is(t.name), hv = ui.hover(cx, cy, cw, CARD_H);
                ui.g.pushClip(cx, cy, cw, CARD_H);
                MenuStyle.backdrop(ui.g, t, cx, cy, cw, CARD_H, false, ui.now, Anim.speed <= 0);
                MenuStyle.key(ui.g, t, cx + 8, cy + 9, cw - 16, 14, hv ? 1 : 0, true);
                ui.g.rect(cx, cy + CARD_H - 13, cx + cw, cy + CARD_H, 0x96000000);
                ui.g.textCentered(t.name, cx + cw / 2f, cy + CARD_H - 11, 0xFFFFFFFF, false);
                ui.g.popClip();
                if (sel || hv) ui.g.roundOutline(cx - 1, cy - 1, cw + 2, CARD_H + 2, 2, 1, sel ? ui.t.accent : ui.t.border);
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
