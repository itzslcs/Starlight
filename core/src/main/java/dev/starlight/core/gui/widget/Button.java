package dev.starlight.core.gui.widget;

import dev.starlight.api.util.Colors;
import dev.starlight.core.gui.Anim;
import dev.starlight.core.gui.Icons;
import dev.starlight.core.gui.MenuStyle;
import dev.starlight.core.gui.Ui;
import dev.starlight.core.gui.Widget;

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
        int fg;
        // Keycaps in the theme's colours (MenuStyle); a ghost button stays a plain label with a soft hover.
        switch (style) {
            case PRIMARY:
                MenuStyle.key(ui.g, x, y, w, h, t, enabled, Colors.lerp(ui.t.accent, 0xFFFFFFFF, 0.18f), ui.t.accent,
                        Colors.lerp(ui.t.accent, 0xFF000000, 0.45f), MenuStyle.glow(ui.t.text));
                fg = ui.t.onAccent;
                break;
            case DANGER:
                MenuStyle.key(ui.g, x, y, w, h, t, enabled, Colors.lerp(ui.t.bad, 0xFF000000, 0.35f), Colors.lerp(ui.t.bad, 0xFF000000, 0.5f),
                        Colors.lerp(ui.t.bad, 0xFF000000, 0.75f), ui.t.bad);
                fg = 0xFFFFFFFF;
                break;
            case GHOST:
                ui.g.roundRect(x, y, w, h, 3, Colors.fade(ui.t.surface2, t));
                fg = Colors.lerp(ui.t.textDim, ui.t.text, t);
                break;
            default:
                MenuStyle.key(ui.g, x, y, w, h, t, enabled, Colors.lerp(ui.t.surface2, ui.t.text, 0.10f), ui.t.surface2,
                        Colors.lerp(ui.t.surface2, 0xFF000000, 0.45f), MenuStyle.glow(ui.t.accent));
                fg = ui.t.text;
                break;
        }
        if (!enabled) fg = Colors.fade(fg, 0.5f);
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
