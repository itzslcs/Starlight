package dev.mw19.core.gui.widget;

import dev.mw19.api.util.Colors;
import dev.mw19.core.gui.Anim;
import dev.mw19.core.gui.Icons;
import dev.mw19.core.gui.Ui;
import dev.mw19.core.gui.Widget;

public class Button extends Widget {
    public enum Style { PRIMARY, SECONDARY, DANGER, GHOST }

    public String label;
    public String icon;
    public Style style;
    public Runnable onClick;
    public boolean enabled = true;
    private final Anim hover = new Anim(0);

    public Button(String label, Style style, Runnable onClick) {
        this.label = label;
        this.style = style;
        this.onClick = onClick;
    }

    public Button icon(String icon) {
        this.icon = icon;
        return this;
    }

    @Override
    public void render(Ui ui) {
        boolean hv = enabled && hovered(ui);
        hover.to(hv ? 1 : 0, 150, ui.now);
        float t = hover.get(ui.now);
        int bg, fg;
        switch (style) {
            case PRIMARY: bg = Colors.lerp(ui.t.accent, 0xFFFFFFFF, t * 0.15f); fg = ui.t.onAccent; break;
            case DANGER: bg = Colors.lerp(Colors.fade(ui.t.bad, 0.25f), ui.t.bad, t * 0.6f); fg = t > 0.5f ? 0xFFFFFFFF : ui.t.bad; break;
            case GHOST: bg = Colors.fade(ui.t.surface2, t); fg = Colors.lerp(ui.t.textDim, ui.t.text, t); break;
            default: bg = Colors.lerp(ui.t.surface2, Colors.lerp(ui.t.surface2, ui.t.text, 0.12f), t); fg = ui.t.text; break;
        }
        if (!enabled) {
            bg = Colors.fade(bg, 0.4f);
            fg = Colors.fade(fg, 0.5f);
        }
        ui.g.roundRect(x, y, w, h, 3, bg);
        float lw = label == null || label.isEmpty() ? 0 : ui.g.textWidth(label);
        float iw = icon == null ? 0 : 10 + (lw > 0 ? 4 : 0);
        float cx = x + (w - lw - iw) / 2f;
        if (icon != null) Icons.draw(ui.g, icon, cx, y + (h - 10) / 2f, fg);
        if (lw > 0) ui.g.text(label, cx + iw, y + (h - 8) / 2f, fg, false);
        focusRing(ui, 3);
        if (hv && tooltip != null) ui.tooltip = tooltip;
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
        if (onClick != null) onClick.run();
    }
}
