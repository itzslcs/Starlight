package dev.mw19.api.util;

/** ARGB helpers. Deliberately avoids java.awt (its class init loads AWT natives, which fights GLFW on macOS). */
public final class Colors {
    private Colors() {}

    public static int alpha(int argb) {
        return argb >>> 24;
    }

    public static int withAlpha(int argb, int alpha) {
        return (Math.max(0, Math.min(255, alpha)) << 24) | (argb & 0xFFFFFF);
    }

    /** Multiplies the alpha channel by {@code f} in [0,1]. */
    public static int fade(int argb, float f) {
        return withAlpha(argb, Math.round(alpha(argb) * Math.max(0f, Math.min(1f, f))));
    }

    public static int lerp(int a, int b, float t) {
        t = Math.max(0f, Math.min(1f, t));
        int aa = a >>> 24, ar = (a >> 16) & 255, ag = (a >> 8) & 255, ab = a & 255;
        int ba = b >>> 24, br = (b >> 16) & 255, bg = (b >> 8) & 255, bb = b & 255;
        return (Math.round(aa + (ba - aa) * t) << 24) | (Math.round(ar + (br - ar) * t) << 16)
                | (Math.round(ag + (bg - ag) * t) << 8) | Math.round(ab + (bb - ab) * t);
    }

    /** h, s, v in [0,1] -> opaque RGB (alpha 0xFF). */
    public static int hsb(float h, float s, float v) {
        h = (h - (float) Math.floor(h)) * 6f;
        int i = (int) h;
        float f = h - i, p = v * (1 - s), q = v * (1 - s * f), t = v * (1 - s * (1 - f));
        float r, g, b;
        switch (i) {
            case 0: r = v; g = t; b = p; break;
            case 1: r = q; g = v; b = p; break;
            case 2: r = p; g = v; b = t; break;
            case 3: r = p; g = q; b = v; break;
            case 4: r = t; g = p; b = v; break;
            default: r = v; g = p; b = q; break;
        }
        return 0xFF000000 | (Math.round(r * 255) << 16) | (Math.round(g * 255) << 8) | Math.round(b * 255);
    }

    /** RGB -> {h, s, v} in [0,1]. */
    public static float[] toHsb(int argb) {
        float r = ((argb >> 16) & 255) / 255f, g = ((argb >> 8) & 255) / 255f, b = (argb & 255) / 255f;
        float max = Math.max(r, Math.max(g, b)), min = Math.min(r, Math.min(g, b)), d = max - min;
        float h;
        if (d == 0) h = 0;
        else if (max == r) h = ((g - b) / d) / 6f;
        else if (max == g) h = ((b - r) / d + 2) / 6f;
        else h = ((r - g) / d + 4) / 6f;
        if (h < 0) h += 1;
        return new float[]{h, max == 0 ? 0 : d / max, max};
    }
}
