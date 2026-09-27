package dev.mw19.core.gui;

import dev.mw19.api.util.Colors;
import dev.mw19.core.render.Gfx;

/**
 * The animated backdrops behind menus, one per theme ({@link Theme#scene}). Each is drawn in whole GUI pixels from rects
 * and gradients only, with nothing allocated per frame: every star, ember or flake is a pure function of its index and
 * the time. Particle counts follow the area, so a Themes-page preview card costs a fraction of a full screen.
 *
 * <p>{@code overWorld}: a world runs behind the menu, so the sky is see-through and the landscape (hills, trees, sun,
 * moon) is left out; the particles stay.
 */
public final class Scenes {
    private Scenes() {}

    private static final long START = System.currentTimeMillis();
    private static final int WHITE = 0xFFFFFFFF;

    /** Draws {@code t}'s scene over (x, y, w, h). {@code still}: animations are off, so nothing moves. */
    public static void draw(Gfx g, Theme t, float x, float y, float w, float h, boolean overWorld, long now, boolean still) {
        double time = still ? 0 : (now - START) / 1000.0;
        float area = Math.max(0.12f, Math.min(2f, w * h / (427f * 240f)));
        int a = overWorld ? 0xB4 : 0xFF;
        g.gradient(x, y, x + w, y + h, Colors.withAlpha(t.skyTop, a), Colors.withAlpha(t.skyBottom, a));
        int glow = MenuStyle.glow(t.accent);
        switch (t.scene) {
            case EMBERS: embers(g, x, y, w, h, overWorld, glow, time, area); break;
            case AURORA: aurora(g, t, x, y, w, h, overWorld, glow, time, area); break;
            case NEBULA: nebula(g, t, x, y, w, h, overWorld, glow, time, area); break;
            case SNOW: snow(g, t, x, y, w, h, overWorld, time, area); break;
            case PETALS: petals(g, t, x, y, w, h, overWorld, glow, time, area); break;
            case CLOUDS: clouds(g, t, x, y, w, h, overWorld, time, area); break;
            default: stars(g, t, x, y, w, h, overWorld, glow, time, area); break;
        }
        g.gradient(x, y, x + w, y + h * 0.18f, overWorld ? 0x40000000 : 0x55000000, 0x00000000); // shade at the top
    }

    // ------------------------------------------------------------------ Starlight: the night sky

    private static void stars(Gfx g, Theme t, float x, float y, float w, float h, boolean overWorld, int glow, double time, float area) {
        g.gradient(x, y + h * 0.55f, x + w, y + h, Colors.withAlpha(glow, 0), Colors.withAlpha(glow, overWorld ? 0x10 : 0x1C)); // light low on the horizon
        // The Milky Way: faint dust along a diagonal band
        int dust = Math.round(150 * area);
        for (int i = 0; i < dust; i++) {
            int s = hash(i + 5000);
            float u = unit(s), spread = (unit(s >>> 7) + unit(s >>> 14) + unit(s >>> 21) - 1.5f) * h * 0.09f;
            float px = x + u * w, py = y + h * 0.82f - u * h * 0.62f + spread;
            int al = 0x0C + (s >>> 3 & 0x1F);
            dot(g, px, py, 1, Colors.withAlpha(0xDCE6FF, al));
        }
        starField(g, x, y, w, h, Math.round(120 * area), glow, time, 0);
        if (!overWorld) {
            moon(g, x + w * 0.82f, y + h * 0.16f, Math.max(4, Math.round(Math.min(w, h) * 0.035f)), t.skyTop);
            hills(g, x, y, w, h, Colors.lerp(t.skyBottom, 0xFF000000, 0.55f), Colors.lerp(t.skyBottom, 0xFF000000, 0.78f), Colors.withAlpha(0xCFE0FF, 0x38), Colors.withAlpha(0xCFE0FF, 0x55));
        }
        shootingStar(g, x, y, w, h, time);
    }

    /** Twinkling stars; a few bright ones are pixel sparkles (a plus). {@code seed} separates star fields. */
    private static void starField(Gfx g, float x, float y, float w, float h, int n, int glow, double time, int seed) {
        for (int i = 0; i < n; i++) {
            int s = hash(i + seed * 7919);
            float px = x + unit(s) * w, py = y + unit(s >>> 11) * h * 0.92f;
            int kind = s >>> 22 & 31; // 0..31: 0 sparkle, 1..8 bright, rest faint
            double tw = 0.62 + 0.38 * Math.sin(time * (0.7 + (s >>> 5 & 15) * 0.15) + (s & 63));
            int color = (s >>> 9 & 7) == 0 ? Colors.lerp(WHITE, glow, 0.55f) : (s >>> 9 & 7) == 1 ? 0xFFC4D6FF : WHITE;
            if (kind == 0) {
                int al = (int) (0xFF * tw);
                dot(g, px, py, 1, Colors.withAlpha(color, al));
                int arm = Colors.withAlpha(color, al / 2);
                dot(g, px - 1, py, 1, arm);
                dot(g, px + 1, py, 1, arm);
                dot(g, px, py - 1, 1, arm);
                dot(g, px, py + 1, 1, arm);
            } else if (kind <= 8) {
                dot(g, px, py, 1, Colors.withAlpha(color, (int) (0xE0 * tw)));
            } else {
                dot(g, px, py, 1, Colors.withAlpha(color, (int) ((0x40 + (s >>> 17 & 0x3F)) * tw)));
            }
        }
    }

    /** One shooting star every few seconds, from a place picked per period. */
    private static void shootingStar(Gfx g, float x, float y, float w, float h, double time) {
        final double period = 6.5, flight = 0.9;
        long k = (long) Math.floor(time / period);
        double phase = time - k * period;
        if (time <= 0 || phase > flight) return;
        int s = hash((int) k + 99991);
        float p = (float) (phase / flight), fade = (float) Math.sin(p * Math.PI);
        float sx = x + w * (0.25f + 0.7f * unit(s)), sy = y + h * (0.05f + 0.3f * unit(s >>> 12));
        float len = Math.max(12, w * 0.3f), hx = sx - p * len * 1.6f, hy = sy + p * len * 0.55f;
        for (int i = 0; i < 16; i++) {
            float f = i / 16f;
            dot(g, hx + f * 22, hy - f * 7.5f, 1, Colors.withAlpha(0xFFFFFF, (int) (0xE0 * (1 - f) * fade)));
        }
        dot(g, hx - 1, hy, 2, Colors.withAlpha(0xFFFFFF, (int) (0xFF * fade)));
    }

    /** A pixel crescent: a disc, a sky-coloured disc over it, and a faint halo. */
    private static void moon(Gfx g, float cx, float cy, int r, int sky) {
        disc(g, cx, cy, r + 3, Colors.withAlpha(0xFFF6D8, 0x10));
        disc(g, cx, cy, r + 1, Colors.withAlpha(0xFFF6D8, 0x18));
        disc(g, cx, cy, r, 0xFFF4EEDC);
        disc(g, cx + r * 0.45f, cy - r * 0.25f, r * 0.85f, sky | 0xFF000000);
    }

    // ------------------------------------------------------------------ Ember: the forge

    private static void embers(Gfx g, float x, float y, float w, float h, boolean overWorld, int glow, double time, float area) {
        g.gradient(x, y + h * 0.4f, x + w, y + h * 0.92f, Colors.withAlpha(glow, 0), Colors.withAlpha(glow, overWorld ? 0x1E : 0x3C)); // the fire below
        int n = Math.round(96 * area);
        float span = h + 24;
        for (int i = 0; i < n; i++) {
            int s = hash(i);
            float depth = (s & 7) / 7f; // 0 far away .. 1 close
            float size = depth > 0.85f ? 3 : depth > 0.4f ? 2 : 1;
            double speed = 6 + depth * 18; // GUI pixels a second: close embers rise faster
            float rise = (float) ((time * speed + ((s >>> 3) & 1023) / 1023.0 * span) % span);
            float ey = y + h + 12 - rise;
            float ex = x + ((s >>> 13) & 1023) / 1023f * w + (float) Math.sin(time * (0.35 + depth * 0.5) + i * 1.7) * (2 + depth * 7);
            float life = Math.max(0f, Math.min(1f, (ey - y) / h * 1.2f)); // they fade out on the way up
            boolean spark = ((s >>> 23) % 3) == 0; // a third glow in the accent, the rest is ash
            int c = spark ? Colors.withAlpha(glow, Math.round((0x60 + 0x9F * depth) * life))
                    : Colors.withAlpha(0xFFFFFF, Math.round((0x10 + 0x24 * depth) * life));
            dot(g, ex, ey, size, c);
        }
        if (!overWorld) skyline(g, x, y, w, h, 0xFF16161C, 0xFF09090C, Colors.withAlpha(glow, 0x40), Colors.withAlpha(glow, 0x70));
    }

    // ------------------------------------------------------------------ Aurora: northern lights over pines

    private static void aurora(Gfx g, Theme t, float x, float y, float w, float h, boolean overWorld, int glow, double time, float area) {
        starField(g, x, y, w, h * 0.7f, Math.round(60 * area), glow, time, 3);
        int tip = Colors.lerp(glow, 0xFF9BE8FF, 0.5f);
        for (float c = 0; c < w; c += 2) {
            double cc = c / Math.max(1f, w) * 427;
            float mid = (float) (h * 0.3 + Math.sin(cc * 0.018 + time * 0.35) * h * 0.06 + Math.sin(cc * 0.041 - time * 0.22) * h * 0.03);
            float tall = (float) (h * (0.1 + 0.09 * (0.5 + 0.5 * Math.sin(cc * 0.027 + time * 0.5))));
            double s = Math.sin(cc * 0.013 - time * 0.6 + Math.sin(cc * 0.004 + time * 0.1) * 2);
            float bright = (float) (0.25 + 0.75 * s * s);
            float bottom = (float) Math.floor(y + mid), top = bottom - (float) Math.floor(tall);
            g.gradient(x + c, top, x + c + 2, bottom, Colors.withAlpha(tip, 0), Colors.withAlpha(glow, Math.round(0x78 * bright)));
            g.rect(x + c, bottom, x + c + 2, bottom + 1, Colors.withAlpha(tip, Math.round(0xA0 * bright)));
        }
        if (!overWorld) {
            hills(g, x, y, w, h, Colors.lerp(t.skyBottom, 0xFF000000, 0.5f), Colors.lerp(t.skyBottom, 0xFF000000, 0.75f), Colors.withAlpha(glow, 0x20), Colors.withAlpha(glow, 0x30));
            pines(g, x, y, w, h, Colors.lerp(t.skyBottom, 0xFF000000, 0.82f));
        }
    }

    /** Pixel pine trees along the bottom: stacked rows that widen downwards. */
    private static void pines(Gfx g, float x, float y, float w, float h, int color) {
        int n = Math.max(4, Math.round(w / 22));
        float ground = y + h * 0.93f;
        g.rect(x, ground, x + w, y + h, color);
        for (int i = 0; i < n; i++) {
            int s = hash(i + 31337);
            float cx = (float) Math.floor(x + (i + unit(s) * 0.8f) * w / n), tall = (float) Math.floor(h * (0.06f + 0.09f * unit(s >>> 10)));
            float top = ground - tall;
            for (float r = 0; r < tall; r += 2) {
                float half = (float) Math.floor(1 + (r / tall) * tall * 0.32f);
                g.rect(cx - half, top + r, cx + half + 1, top + r + 2, color);
            }
        }
    }

    // ------------------------------------------------------------------ Nebula: clouds of colour among stars

    private static void nebula(Gfx g, Theme t, float x, float y, float w, float h, boolean overWorld, int glow, double time, float area) {
        int second = Colors.lerp(glow, 0xFFFF5FA8, 0.6f), third = 0xFF4F7BFF;
        // Each cloud is a clump of overlapping puffs in one colour, drifting slowly: soft and uneven, never one disc
        for (int b = 0; b < 6; b++) {
            int s = hash(b + 777);
            float cx = x + w * (0.08f + 0.84f * unit(s)) + (float) Math.sin(time * 0.05 + b) * w * 0.03f;
            float cy = y + h * (0.1f + 0.55f * unit(s >>> 10)) + (float) Math.cos(time * 0.04 + b * 2) * h * 0.025f;
            float r = Math.min(w, h) * (0.08f + 0.08f * unit(s >>> 20));
            int c = b % 3 == 0 ? glow : b % 3 == 1 ? second : third;
            for (int p = 0; p < 7; p++) {
                int q = hash(b * 31 + p + 991);
                float px = cx + (unit(q) - 0.5f) * r * 3.2f, py = cy + (unit(q >>> 10) - 0.5f) * r * 1.6f;
                float pr = r * (0.45f + 0.7f * unit(q >>> 20));
                for (int ring = 3; ring >= 0; ring--) disc(g, px, py, pr * (0.45f + ring * 0.2f), Colors.withAlpha(c, overWorld ? 0x05 : 0x09));
            }
        }
        starField(g, x, y, w, h, Math.round(130 * area), glow, time, 5);
        if (!overWorld) hills(g, x, y, w, h, Colors.lerp(t.skyBottom, 0xFF000000, 0.55f), Colors.lerp(t.skyBottom, 0xFF000000, 0.8f), Colors.withAlpha(second, 0x30), Colors.withAlpha(second, 0x50));
    }

    // ------------------------------------------------------------------ Glacier: snowfall over icy peaks

    private static void snow(Gfx g, Theme t, float x, float y, float w, float h, boolean overWorld, double time, float area) {
        if (!overWorld) mountains(g, x, y, w, h, Colors.lerp(t.skyBottom, 0xFF000000, 0.45f), 0xFFE6F2FF);
        int n = Math.round(110 * area);
        float span = h + 16;
        for (int i = 0; i < n; i++) {
            int s = hash(i + 4242);
            float depth = (s & 7) / 7f;
            double fall = 7 + depth * 16;
            float py = y - 8 + (float) ((time * fall + unit(s >>> 3) * span) % span);
            float px = x + unit(s >>> 13) * w + (float) Math.sin(time * (0.6 + depth) + i) * (2 + depth * 4);
            dot(g, px, py, depth > 0.75f ? 2 : 1, Colors.withAlpha(0xFFFFFF, Math.round(0x50 + 0x90 * depth)));
        }
    }

    /** Peaks (value noise with sharpened ridges), white from their tops down to a ragged snow line. */
    private static void mountains(Gfx g, float x, float y, float w, float h, int rock, int snowCap) {
        float col = 4, base = y + h * 0.95f, rise = h * 0.32f, snowLine = base - rise * 0.58f;
        int n = (int) Math.ceil(w / col);
        for (int i = 0; i < n; i++) {
            float k = i / 7f, f = k - (float) Math.floor(k);
            float v = lerp(noise((int) Math.floor(k) + 500), noise((int) Math.floor(k) + 501), f);
            v = 1 - Math.abs(2 * v - 1); // ridges
            float top = (float) Math.floor((base - v * rise) / 2) * 2, px = x + i * col;
            g.rect(px, top, px + col, y + h, rock);
            float line = (float) Math.floor(snowLine + (hash(i + 612) & 3) * 2); // ragged
            if (top < line - 1) g.rect(px, top, px + col, line, snowCap);
            else if (v > 0.5f) g.rect(px, top, px + col, top + 2, Colors.withAlpha(snowCap, 0x90)); // a dusting lower down
        }
    }

    // ------------------------------------------------------------------ Sakura: petals at dusk

    private static void petals(Gfx g, Theme t, float x, float y, float w, float h, boolean overWorld, int glow, double time, float area) {
        if (!overWorld) {
            float r = Math.min(w, h) * 0.12f, sx = x + w * 0.24f, sy = y + h * 0.7f;
            disc(g, sx, sy, r * 1.6f, Colors.withAlpha(0xFFE1C8, 0x10));
            disc(g, sx, sy, r, Colors.withAlpha(0xFFE9D6, 0x9C)); // a low sun
            hills(g, x, y, w, h, Colors.lerp(t.skyBottom, 0xFF000000, 0.35f), Colors.lerp(t.skyBottom, 0xFF000000, 0.6f), Colors.withAlpha(glow, 0x30), Colors.withAlpha(glow, 0x48));
        }
        int n = Math.round(70 * area);
        float span = h + 20, drift = w + 20;
        for (int i = 0; i < n; i++) {
            int s = hash(i + 8080);
            float depth = (s & 7) / 7f;
            double fall = 9 + depth * 14, wind = 10 + depth * 12;
            float py = y - 10 + (float) ((time * fall + unit(s >>> 3) * span) % span);
            float px = x - 10 + (float) ((time * wind + unit(s >>> 13) * drift) % drift);
            boolean flat = Math.sin(time * (2 + depth * 3) + i) > 0; // the petal turns as it falls
            int c = Colors.withAlpha((s >>> 20 & 3) == 0 ? 0xFFFFE4EE : glow, Math.round(0x70 + 0x8F * depth));
            float pw = flat ? 2 : 1, ph = flat ? 1 : 2;
            px = (float) Math.floor(px);
            py = (float) Math.floor(py);
            g.rect(px, py, px + pw + (depth > 0.7f ? 1 : 0), py + ph, c);
        }
    }

    // ------------------------------------------------------------------ Daylight: a square sun and blocky clouds

    private static void clouds(Gfx g, Theme t, float x, float y, float w, float h, boolean overWorld, double time, float area) {
        if (!overWorld) {
            float s = Math.max(6, Math.round(Math.min(w, h) * 0.07f)), sx = x + w * 0.16f, sy = y + h * 0.14f;
            g.rect(sx - s * 0.9f, sy - s * 0.9f, sx + s * 0.9f, sy + s * 0.9f, 0x22FFF4C0); // Minecraft's sun is a square
            g.rect(sx - s / 2, sy - s / 2, sx + s / 2, sy + s / 2, 0xFFFFF4C0);
        }
        for (int layer = 0; layer < 2; layer++) {
            int n = Math.max(2, Math.round((layer == 0 ? 7 : 5) * (float) Math.sqrt(area)));
            float speed = layer == 0 ? 3 : 7, cell = Math.max(3, Math.round(Math.min(w, h) * (layer == 0 ? 0.018f : 0.028f)));
            int body = layer == 0 ? 0x9AFFFFFF : 0xE6FFFFFF, under = layer == 0 ? 0x60D8E4F0 : 0xC0D8E4F0;
            float drift = w + cell * 16;
            for (int i = 0; i < n; i++) {
                int s = hash(i + layer * 50 + 1234);
                float cx = x - cell * 8 + (float) ((time * speed + unit(s) * drift) % drift);
                float cy = y + h * (layer == 0 ? 0.1f + 0.25f * unit(s >>> 10) : 0.2f + 0.3f * unit(s >>> 10));
                cx = (float) Math.floor(cx / cell) * cell;
                cy = (float) Math.floor(cy / cell) * cell;
                int cw = 4 + (s >>> 20 & 7), ch = 2 + (s >>> 24 & 1);
                g.rect(cx, cy, cx + cw * cell, cy + ch * cell, body);
                g.rect(cx + cell * 2, cy - cell, cx + (cw - 1) * cell, cy, body);
                g.rect(cx, cy + ch * cell - 1, cx + cw * cell, cy + ch * cell, under);
            }
        }
        if (!overWorld) hills(g, x, y, w, h, Colors.lerp(t.skyBottom, 0xFF3F7A4A, 0.55f), Colors.lerp(t.skyBottom, 0xFF2F5E37, 0.75f), 0x40FFFFFF, 0x60B6F07A);
    }

    // ------------------------------------------------------------------ shared pieces

    /** Two rows of block columns, stepped like terrain, with their top edges lit. */
    private static void skyline(Gfx g, float x, float y, float w, float h, int far, int near, int farRim, int nearRim) {
        terrain(g, x, y, w, h, 8, 12, 0.82f, 0.9f, 0.15f, 0.10f, 3, far, near, farRim, nearRim);
    }

    /** Softer, rounder hills (thinner columns, smaller steps) for the night and dusk scenes. */
    private static void hills(Gfx g, float x, float y, float w, float h, int far, int near, int farRim, int nearRim) {
        terrain(g, x, y, w, h, 4, 6, 0.86f, 0.92f, 0.1f, 0.07f, 2, far, near, farRim, nearRim);
    }

    private static void terrain(Gfx g, float x, float y, float w, float h, float colFar, float colNear, float baseFar, float baseNear,
                                float riseFar, float riseNear, int step, int far, int near, int farRim, int nearRim) {
        for (int layer = 0; layer < 2; layer++) {
            float col = layer == 0 ? colFar : colNear, base = y + h * (layer == 0 ? baseFar : baseNear), rise = h * (layer == 0 ? riseFar : riseNear);
            int body = layer == 0 ? far : near, rim = layer == 0 ? farRim : nearRim;
            int n = (int) Math.ceil(w / col);
            for (int i = 0; i < n; i++) {
                float k = i / (col < 6 ? 8f : 4f), f = k - (float) Math.floor(k);
                int a = (int) Math.floor(k) + layer * 97;
                float v = lerp(noise(a), noise(a + 1), f * f * (3 - 2 * f));
                float top = (float) Math.floor((base - v * rise) / step) * step, px = x + i * col;
                g.rect(px, top, Math.min(px + col, x + w), y + h, body);
                g.rect(px, top, Math.min(px + col, x + w), top + 1, rim);
            }
        }
    }

    /** A filled pixel circle, one rect per row. */
    private static void disc(Gfx g, float cx, float cy, float r, int color) {
        int ri = Math.max(1, Math.round(r));
        for (int dy = -ri; dy < ri; dy++) {
            float yy = dy + 0.5f, half = (float) Math.floor(Math.sqrt(Math.max(0, ri * ri - yy * yy)));
            if (half <= 0) continue;
            float py = (float) Math.floor(cy) + dy;
            g.rect((float) Math.floor(cx) - half, py, (float) Math.floor(cx) + half, py + 1, color);
        }
    }

    private static void dot(Gfx g, float x, float y, float size, int color) {
        float px = (float) Math.floor(x), py = (float) Math.floor(y);
        g.rect(px, py, px + size, py + size, color);
    }

    private static float unit(int s) {
        return (s & 1023) / 1023f;
    }

    private static float noise(int i) {
        return (hash(i + 7919) & 1023) / 1023f;
    }

    private static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    static int hash(int i) {
        int x = i * 0x9E3779B9;
        x ^= x >>> 16;
        x *= 0x85EBCA6B;
        x ^= x >>> 13;
        x *= 0xC2B2AE35;
        x ^= x >>> 16;
        return x & 0x7FFFFFFF;
    }
}
