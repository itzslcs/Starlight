package dev.kestrel.core.gui.widget;

import dev.kestrel.api.setting.NumberSetting;
import dev.kestrel.api.util.Colors;
import dev.kestrel.core.Keys;
import dev.kestrel.core.gui.Anim;
import dev.kestrel.core.gui.Ui;
import dev.kestrel.core.gui.Widget;

public class Slider extends Widget {
    private final NumberSetting s;
    private final Anim pos;
    private boolean dragging;
    private double shown = Double.NaN;
    private String label = "";

    public Slider(NumberSetting s) {
        this.s = s;
        this.pos = new Anim((float) s.fraction());
    }

    @Override
    public void render(Ui ui) {
        if (dragging) set(ui);
        if (s.get() != shown) {
            shown = s.get();
            label = s.format();
        }
        float lw = 34, tx = x, tw = w - lw - 4, ty = y + h / 2f - 1.5f;
        pos.to((float) s.fraction(), dragging ? 0 : 120, ui.now);
        float f = pos.get(ui.now);
        ui.g.roundRect(tx, ty, tw, 3, 1.5f, ui.t.surface2);
        ui.g.roundRect(tx, ty, Math.max(3, tw * f), 3, 1.5f, ui.t.accent);
        boolean hv = hovered(ui) || dragging;
        float kr = hv ? 4.5f : 3.5f;
        ui.g.roundRect(tx + tw * f - kr, ty + 1.5f - kr, kr * 2, kr * 2, kr, 0xFFFFFFFF);
        ui.g.textRight(label, x + w, y + (h - 8) / 2f, hv ? ui.t.text : ui.t.textDim, false);
        focusRing(ui, 3);
        if (hovered(ui) && tooltip != null) ui.tooltip = tooltip;
    }

    private void set(Ui ui) {
        float tw = w - 38;
        double f = Math.max(0, Math.min(1, (ui.mx - x) / tw));
        s.set(s.min() + f * (s.max() - s.min()));
    }

    @Override
    public boolean mouseClicked(Ui ui, int button) {
        if (button != 0 || !contains(ui.mx, ui.my)) return false;
        dragging = true;
        ui.root.capture(this);
        ui.root.focus(this);
        set(ui);
        return true;
    }

    @Override
    public boolean mouseDragged(Ui ui, int button) {
        if (dragging) set(ui);
        return dragging;
    }

    @Override
    public boolean mouseReleased(Ui ui, int button) {
        boolean was = dragging;
        dragging = false;
        return was;
    }

    @Override
    public boolean mouseScrolled(Ui ui, double amount) {
        if (!contains(ui.mx, ui.my) || !ui.root.isFocused(this)) return false;
        s.set(s.get() + Math.signum(amount) * s.step());
        return true;
    }

    @Override
    public boolean keyPressed(Ui ui, int key, int mods) {
        double step = (mods & Keys.MOD_SHIFT) != 0 ? s.step() * 10 : s.step();
        if (key == Keys.LEFT || key == Keys.DOWN) s.set(s.get() - step);
        else if (key == Keys.RIGHT || key == Keys.UP) s.set(s.get() + step);
        else if (key == Keys.HOME) s.set(s.min());
        else if (key == Keys.END) s.set(s.max());
        else return false;
        return true;
    }

    @Override
    public boolean focusable() {
        return true;
    }

    public static int dimOf(int c) {
        return Colors.fade(c, 0.6f);
    }
}
