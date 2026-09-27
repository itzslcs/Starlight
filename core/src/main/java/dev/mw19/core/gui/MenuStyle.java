package dev.mw19.core.gui;

import dev.mw19.api.util.Colors;
import dev.mw19.core.render.Gfx;

/**
 * MW19's own look, taken from its logo: letters built from blocks, with depth, and the "19" in the accent colour.
 *
 * <p>A button is a keycap. A dark outline with notched corners holds a face that is lit along its top edge. Below the
 * face is the key's side, which shows its depth. On hover the side lights up in the accent, like a backlit key.
 *
 * <p>Behind the menus runs the theme's scene ({@link Scenes}): a night sky for Starlight, embers rising from a
 * forge for Ember, and so on. Over a running world it is see-through.
 *
 * <p>Everything is drawn in whole GUI pixels (crisp at every GUI scale), with no allocation per frame.
 */
public final class MenuStyle {
    private MenuStyle() {}

    /** The keycap outline (dark themes). */
    public static final int OUTLINE = 0xFF060608;
    /** Label colours on a charcoal key. */
    public static final int LABEL = 0xFFE8E8EC, LABEL_HOVER = 0xFFFFFFFF, LABEL_OFF = 0xFF7E8089;

    /** A keycap in the theme's menu-key colours (Minecraft's menus, the home screen). {@code hover}: 0..1. */
    public static void key(Gfx g, Theme t, float x, float y, float w, float h, float hover, boolean active) {
        key(g, x, y, w, h, hover, active, t.keyTop, t.keyBottom, t.keySide, glow(t.accent));
    }

    /**
     * A keycap in (x, y, w, h) with its own colours: {@code top}/{@code bottom} for the face's gradient, {@code side}
     * for the depth, {@code glow} for the backlight on hover. The face ends 3 px above the bottom (2 px of side, 1 px
     * of outline), so a label drawn at {@code y + (h - 8) / 2}, as vanilla draws it, sits in the face's middle.
     */
    public static void key(Gfx g, float x, float y, float w, float h, float hover, boolean active, int top, int bottom, int side, int glow) {
        float x1 = (float) Math.floor(x), y1 = (float) Math.floor(y);
        float x2 = x1 + (float) Math.floor(w), y2 = y1 + (float) Math.floor(h), face = y2 - 3;
        if (x2 - x1 < 4 || y2 - y1 < 6) return;
        int outline = luma(bottom) > 0.55f ? 0xFF8E9098 : OUTLINE; // light themes: a grey outline, not black
        g.rect(x1 + 1, y1, x2 - 1, y1 + 1, outline); // outline without its four corner pixels: the notch
        g.rect(x1 + 1, y2 - 1, x2 - 1, y2, outline);
        g.rect(x1, y1 + 1, x1 + 1, y2 - 1, outline);
        g.rect(x2 - 1, y1 + 1, x2, y2 - 1, outline);
        if (!active) { // unlit and flat: a key that cannot be pressed
            g.rect(x1 + 1, y1 + 1, x2 - 1, y2 - 1, Colors.lerp(bottom, side, 0.55f));
            return;
        }
        float t = Math.max(0f, Math.min(1f, hover));
        g.rect(x1 + 1, face, x2 - 1, y2 - 1, Colors.lerp(side, Colors.lerp(glow, 0xFF000000, 0.2f), t)); // the side
        g.gradient(x1 + 1, y1 + 1, x2 - 1, face, lighten(top, 0.10f * t), lighten(bottom, 0.10f * t));
        g.rect(x1 + 1, y1 + 1, x2 - 1, y1 + 2, Colors.withAlpha(0xFFFFFF, 0x2C + Math.round(0x34 * t))); // lit top edge
        g.rect(x1 + 1, y1 + 2, x1 + 2, face, 0x12FFFFFF); // and a fainter left edge
        if (t > 0.01f) g.rect(x1 + 1, face - 1, x2 - 1, face, Colors.withAlpha(glow, Math.round(0x70 * t))); // light on the face
    }

    /**
     * A slider in (x, y, w, h), in vanilla's geometry (an 8 px handle at {@code value} of the free width). The track is a
     * flatter, darker key whose side is lit in the glow up to the handle; the handle is a small key that lights up on hover.
     */
    public static void slider(Gfx g, Theme t, float x, float y, float w, float h, float value, float hover, boolean active) {
        int glow = glow(t.accent);
        key(g, x, y, w, h, 0, active, Colors.lerp(t.keyTop, 0xFF000000, 0.25f), Colors.lerp(t.keyBottom, 0xFF000000, 0.25f), t.keySide, glow);
        float x1 = (float) Math.floor(x), y2 = (float) Math.floor(y) + (float) Math.floor(h);
        float hx = x1 + Math.round(Math.max(0f, Math.min(1f, value)) * ((float) Math.floor(w) - 8));
        if (active && hx > x1 + 1) g.rect(x1 + 1, y2 - 3, hx + 4, y2 - 1, Colors.lerp(glow, 0xFF000000, 0.35f));
        key(g, hx, y, 8, h, hover, active, t.keyTop, t.keyBottom, t.keySide, glow);
    }

    /** The glow colour for an accent: a near-black accent (the White theme) would not show on a dark key. */
    public static int glow(int accent) {
        return luma(accent) < 0.3f ? 0xFFFFFFFF : accent | 0xFF000000;
    }

    /** The theme's backdrop over (x, y, w, h) (see {@link Scenes}). */
    public static void backdrop(Gfx g, Theme t, float x, float y, float w, float h, boolean overWorld, long now, boolean still) {
        Scenes.draw(g, t, x, y, w, h, overWorld, now, still);
    }

    private static int lighten(int argb, float f) {
        return Colors.lerp(argb, argb | 0xFFFFFF, f);
    }

    private static float luma(int argb) {
        return (0.2126f * ((argb >> 16) & 255) + 0.7152f * ((argb >> 8) & 255) + 0.0722f * (argb & 255)) / 255f;
    }

}
