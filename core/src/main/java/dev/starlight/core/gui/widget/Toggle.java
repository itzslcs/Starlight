package dev.starlight.core.gui.widget;

import dev.starlight.api.util.Colors;
import dev.starlight.core.gui.Anim;
import dev.starlight.core.gui.Ui;
import dev.starlight.core.gui.Widget;

/** Animated switch bound to a getter/setter. */
public class Toggle extends Widget {
    public interface Model {
        boolean get();

        void set(boolean v);
    }

    private final Model model;
    private final Anim knob;
    public boolean enabled = true;

    public Toggle(Model model) {
        this.model = model;
        this.knob = new Anim(model.get() ? 1 : 0);
    }

    @Override
    public void render(Ui ui) {
        boolean on = model.get();
        knob.to(on ? 1 : 0, 180, ui.now);
        float t = knob.get(ui.now);
        float tw = 22, th = 12, tx = x + w - tw, ty = y + (h - th) / 2f;
        int track = Colors.lerp(ui.t.surface2, ui.t.accent, t);
        if (!enabled) track = Colors.fade(track, 0.4f);
        ui.g.roundRect(tx, ty, tw, th, th / 2f, track);
        if (hovered(ui)) ui.g.roundOutline(tx, ty, tw, th, th / 2f, 1, Colors.fade(ui.t.text, 0.25f));
        float kx = tx + 2 + (tw - th) * t;
        ui.g.roundRect(kx, ty + 2, th - 4, th - 4, (th - 4) / 2f, enabled ? 0xFFFFFFFF : 0xFFAAAAAA);
        focusRing(ui, th / 2f);
        if (hovered(ui) && tooltip != null) ui.tooltip = tooltip;
    }

    @Override
    public boolean mouseClicked(Ui ui, int button) {
        if (button != 0 || !enabled || !contains(ui.mx, ui.my)) return false;
        activate(ui);
        return true;
    }

    @Override
    public boolean focusable() {
        return enabled;
    }

    @Override
    public void activate(Ui ui) {
        if (enabled) model.set(!model.get());
    }
}
