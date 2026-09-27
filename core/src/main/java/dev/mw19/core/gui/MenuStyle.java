package dev.mw19.core.gui;

import dev.mw19.api.util.Colors;
import dev.mw19.core.render.Gfx;

/**
 * MW19's own look, taken from its logo: letters built from blocks, with depth, and the "19" in the accent colour.
 *
 * <p>A button is a keycap. A dark outline with notched corners holds a face that is lit along its top edge. Below the
 * face is the key's side, which shows its depth. On hover the side lights up in the accent, like a backlit key.
 *
 * <p>The menu backdrop is deep charcoal with a warm glow in the accent rising from below. In front of the glow, a
 * skyline of block columns catches it on their top edges, and pixel embers drift up from behind it. Over a running world
 * it is see-through and has no skyline: the world is the landscape.
 *
 * <p>Everything is drawn in whole GUI pixels (crisp at every GUI scale), with no allocation per frame.
 */
public final class MenuStyle {
    private MenuStyle() {}

    /** The charcoal keycap of Minecraft's own menus and the home screen. */
    public static final int KEY_TOP = 0xFF3D404B, KEY_BOTTOM = 0xFF2B2E36, KEY_SIDE = 0xFF18191F, OUTLINE = 0xFF060608;
    /** Label colours on a charcoal key. */
    public static final int LABEL = 0xFFE8E8EC, LABEL_HOVER = 0xFFFFFFFF, LABEL_OFF = 0xFF7E8089;

    /** A charcoal keycap (vanilla menus, home screen). {@code hover}: 0..1, {@code glow}: see {@link #glow}. */
    public static void key(Gfx g, float x, float y, float w, float h, float hover, boolean active, int glow) {
        key(g, x, y, w, h, hover, active, KEY_TOP, KEY_BOTTOM, KEY_SIDE, glow);
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
    public static void slider(Gfx g, float x, float y, float w, float h, float value, float hover, boolean active, int glow) {
        key(g, x, y, w, h, 0, active, 0xFF2A2C34, 0xFF212229, KEY_SIDE, glow);
        float x1 = (float) Math.floor(x), y2 = (float) Math.floor(y) + (float) Math.floor(h);
        float hx = x1 + Math.round(Math.max(0f, Math.min(1f, value)) * ((float) Math.floor(w) - 8));
        if (active && hx > x1 + 1) g.rect(x1 + 1, y2 - 3, hx + 4, y2 - 1, Colors.lerp(glow, 0xFF000000, 0.35f));
        key(g, hx, y, 8, h, hover, active, glow);
    }

    /** The glow colour for an accent: a near-black accent (the White theme) would not show on a dark key. */
    public static int glow(int accent) {
        return luma(accent) < 0.3f ? 0xFFFFFFFF : accent | 0xFF000000;
    }

    private static final int EMBERS = 96;
    private static final long START = System.currentTimeMillis();

    /**
     * The menu backdrop over (0, 0)-(w, h). {@code overWorld}: a world is running behind the menu, so the backdrop lets
     * it show through. {@code glow}: the colour of the glow and of the sparks among the embers ({@link #glow}).
     * {@code still}: animations are off, so the embers do not move.
     */
    public static void backdrop(Gfx g, float w, float h, boolean overWorld, int glow, long now, boolean still) {
        int a = overWorld ? 0xB4 : 0xFF;
        g.gradient(0, 0, w, h, Colors.withAlpha(0x15151B, a), Colors.withAlpha(0x09090C, a));
        g.gradient(0, h * 0.4f, w, h * 0.92f, Colors.withAlpha(glow, 0), Colors.withAlpha(glow, overWorld ? 0x1E : 0x3C)); // the fire below
        double t = still ? 0 : (now - START) / 1000.0;
        float span = h + 24;
        for (int i = 0; i < EMBERS; i++) {
            int s = hash(i);
            float depth = (s & 7) / 7f; // 0 far away .. 1 close
            float size = depth > 0.85f ? 3 : depth > 0.4f ? 2 : 1;
            double speed = 6 + depth * 18; // GUI pixels a second: close embers rise faster
            float rise = (float) ((t * speed + ((s >>> 3) & 1023) / 1023.0 * span) % span);
            float ey = h + 12 - rise;
            float ex = ((s >>> 13) & 1023) / 1023f * w + (float) Math.sin(t * (0.35 + depth * 0.5) + i * 1.7) * (2 + depth * 7);
            float life = Math.max(0f, Math.min(1f, ey / h * 1.2f)); // they fade out on the way up
            boolean spark = ((s >>> 23) % 3) == 0; // a third glow in the accent, the rest is ash
            int c = spark ? Colors.withAlpha(glow, Math.round((0x60 + 0x9F * depth) * life))
                    : Colors.withAlpha(0xFFFFFF, Math.round((0x10 + 0x24 * depth) * life));
            float px = (float) Math.floor(ex), py = (float) Math.floor(ey);
            g.rect(px, py, px + size, py + size, c);
        }
        if (!overWorld) skyline(g, w, h, glow);
        g.gradient(0, 0, w, h * 0.2f, overWorld ? 0x48000000 : 0x6C000000, 0x00000000); // shade at the top
    }

    /**
     * Two rows of block columns along the bottom: a far, lighter row and a near, darker one, stepped like terrain.
     * Their top edges catch the glow behind them. The heights are value noise over the column index: fixed per
     * screen width, no state.
     */
    private static void skyline(Gfx g, float w, float h, int glow) {
        for (int layer = 0; layer < 2; layer++) {
            float col = layer == 0 ? 8 : 12, base = h * (layer == 0 ? 0.82f : 0.9f), rise = h * (layer == 0 ? 0.15f : 0.10f);
            int body = layer == 0 ? 0xFF16161C : 0xFF09090C, rim = Colors.withAlpha(glow, layer == 0 ? 0x40 : 0x70);
            int n = (int) Math.ceil(w / col);
            for (int i = 0; i < n; i++) {
                float k = i / 4f, f = k - (float) Math.floor(k);
                int a = (int) Math.floor(k) + layer * 97;
                float v = lerp(noise(a), noise(a + 1), f * f * (3 - 2 * f)); // smooth between control points
                float top = (float) Math.floor((base - v * rise) / 3) * 3, x = i * col; // in 3 px steps, like blocks
                g.rect(x, top, x + col, h, body);
                g.rect(x, top, x + col, top + 1, rim);
            }
        }
    }

    private static float noise(int i) {
        return (hash(i + 7919) & 1023) / 1023f;
    }

    private static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    private static int lighten(int argb, float f) {
        return Colors.lerp(argb, argb | 0xFFFFFF, f);
    }

    private static float luma(int argb) {
        return (0.2126f * ((argb >> 16) & 255) + 0.7152f * ((argb >> 8) & 255) + 0.0722f * (argb & 255)) / 255f;
    }

    private static int hash(int i) {
        int x = i * 0x9E3779B9;
        x ^= x >>> 16;
        x *= 0x85EBCA6B;
        x ^= x >>> 13;
        x *= 0xC2B2AE35;
        x ^= x >>> 16;
        return x & 0x7FFFFFFF;
    }
}
