package dev.kestrel.core.gui.widget;

import dev.kestrel.api.util.Colors;
import dev.kestrel.core.Keys;
import dev.kestrel.core.gui.Icons;
import dev.kestrel.core.gui.Ui;
import dev.kestrel.core.gui.Widget;

import java.util.List;

/**
 * Single choice (ChoiceSetting) or, with {@code multi}, any subset (MultiChoiceSetting). Opens an option list popup.
 */
public class Dropdown extends Widget {
    public interface Model {
        List<String> options();

        boolean selected(String option);

        void pick(String option);

        String summary();
    }

    private final Model model;
    private final boolean multi;

    public Dropdown(Model model, boolean multi) {
        this.model = model;
        this.multi = multi;
    }

    @Override
    public void render(Ui ui) {
        boolean open = ui.root.popupOwner() == this;
        boolean hv = hovered(ui);
        ui.g.roundRect(x, y, w, h, 3, hv || open ? Colors.lerp(ui.t.surface2, ui.t.text, 0.08f) : ui.t.surface2);
        ui.g.text(ui.g.ellipsize(model.summary(), w - 18), x + 5, y + (h - 8) / 2f, ui.t.text, false);
        // chevron
        float cx = x + w - 10, cy = y + h / 2f;
        for (int i = 0; i < 3; i++) {
            float yy = open ? cy + 1 - i : cy - 1 + i;
            ui.g.rect(cx - 3 + i, yy, cx + 3 - i, yy + 1, ui.t.textDim);
        }
        focusRing(ui, 3);
        if (hv && tooltip != null) ui.tooltip = tooltip;
    }

    @Override
    public boolean mouseClicked(Ui ui, int button) {
        if (button != 0 || !contains(ui.mx, ui.my)) return false;
        activate(ui);
        return true;
    }

    @Override
    public boolean focusable() {
        return true;
    }

    @Override
    public void activate(Ui ui) {
        if (ui.root.popupOwner() == this) {
            ui.root.closePopup();
            return;
        }
        List<String> opts = model.options();
        float ih = 14, ph = Math.min(opts.size(), 10) * ih + 4;
        float py = y + h + 2 + ph > ui.root.height() ? y - ph - 2 : y + h + 2;
        ui.root.openPopup(this, new OptionList(ph).bounds(x, py, Math.max(w, 90), ph));
    }

    private final class OptionList extends Widget {
        private float scroll;
        private final float fullH;

        OptionList(float h) {
            this.fullH = h;
        }

        @Override
        public void render(Ui ui) {
            List<String> opts = model.options();
            ui.g.roundRect(x, y, w, h, 4, ui.t.surface);
            ui.g.roundOutline(x, y, w, h, 4, 1, ui.t.border);
            ui.g.pushClip(x, y + 2, w, h - 4);
            float ih = 14;
            for (int i = 0; i < opts.size(); i++) {
                String o = opts.get(i);
                float iy = y + 2 + i * ih - scroll;
                if (iy + ih < y || iy > y + h) continue;
                boolean hv = ui.hoverPopup(x, iy, w, ih);
                boolean sel = model.selected(o);
                if (hv) ui.g.roundRect(x + 2, iy, w - 4, ih, 3, ui.t.surface2);
                if (multi) {
                    ui.g.roundOutline(x + 5, iy + 3, 8, 8, 2, 1, sel ? ui.t.accent : ui.t.textDim);
                    if (sel) ui.g.roundRect(x + 7, iy + 5, 4, 4, 1, ui.t.accent);
                } else if (sel) {
                    Icons.draw(ui.g, "check", x + 4, iy + 2, ui.t.accent);
                }
                ui.g.text(ui.g.ellipsize(o, w - 22), x + 17, iy + 3, sel ? ui.t.text : ui.t.textDim, false);
            }
            ui.g.popClip();
        }

        @Override
        public boolean mouseClicked(Ui ui, int button) {
            if (!contains(ui.mx, ui.my)) return false;
            int i = (int) ((ui.my - y - 2 + scroll) / 14);
            List<String> opts = model.options();
            if (i >= 0 && i < opts.size()) {
                model.pick(opts.get(i));
                if (!multi) ui.root.closePopup();
            }
            return true;
        }

        @Override
        public boolean mouseScrolled(Ui ui, double amount) {
            float max = Math.max(0, model.options().size() * 14 + 4 - fullH);
            scroll = Math.max(0, Math.min(max, scroll - (float) amount * 14));
            return true;
        }

        @Override
        public boolean keyPressed(Ui ui, int key, int mods) {
            if (key != Keys.UP && key != Keys.DOWN) return false;
            List<String> opts = model.options();
            if (multi || opts.isEmpty()) return true;
            int cur = 0;
            for (int i = 0; i < opts.size(); i++) if (model.selected(opts.get(i))) cur = i;
            cur = Math.max(0, Math.min(opts.size() - 1, cur + (key == Keys.DOWN ? 1 : -1)));
            model.pick(opts.get(cur));
            return true;
        }
    }
}
