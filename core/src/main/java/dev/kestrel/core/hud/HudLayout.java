package dev.kestrel.core.hud;

import dev.kestrel.api.hud.Anchor;

/** Pure layout math for HUD elements (unit-tested; no Minecraft, no state). */
public final class HudLayout {
    private HudLayout() {}

    /** Box size for content of size (cw, ch). */
    public static float boxW(HudElement e, float cw) {
        return (cw + 2 * e.padding) * e.scale;
    }

    public static float boxH(HudElement e, float ch) {
        return (ch + 2 * e.padding) * e.scale;
    }

    private static float dir(float f) {
        return f >= 1f ? -1f : 1f;
    }

    /** Computes the absolute box into e.bx/by/bw/bh, clamped to the screen. */
    public static void place(HudElement e, float cw, float ch, float sw, float sh) {
        float w = boxW(e, cw), h = boxH(e, ch);
        Anchor a = e.anchor;
        float ox = e.percent ? e.x * sw : e.x, oy = e.percent ? e.y * sh : e.y;
        float x = sw * a.fx - w * a.fx + dir(a.fx) * ox;
        float y = sh * a.fy - h * a.fy + dir(a.fy) * oy;
        e.bw = w;
        e.bh = h;
        e.bx = clamp(x, 0, Math.max(0, sw - w));
        e.by = clamp(y, 0, Math.max(0, sh - h));
    }

    /** Inverse of {@link #place}: sets offsets so the box lands at (x,y). Re-anchors first when autoAnchor. */
    public static void moveTo(HudElement e, float x, float y, float sw, float sh) {
        float w = e.bw, h = e.bh;
        x = clamp(x, 0, Math.max(0, sw - w));
        y = clamp(y, 0, Math.max(0, sh - h));
        if (e.autoAnchor) e.anchor = nearestAnchor(x + w / 2, y + h / 2, sw, sh);
        Anchor a = e.anchor;
        float ox = (x - (sw * a.fx - w * a.fx)) * dir(a.fx);
        float oy = (y - (sh * a.fy - h * a.fy)) * dir(a.fy);
        e.x = e.percent ? (sw > 0 ? ox / sw : 0) : ox;
        e.y = e.percent ? (sh > 0 ? oy / sh : 0) : oy;
        e.bx = x;
        e.by = y;
    }

    /** Anchor whose screen third contains the point. */
    public static Anchor nearestAnchor(float cx, float cy, float sw, float sh) {
        int col = cx < sw / 3f ? 0 : cx > sw * 2f / 3f ? 2 : 1;
        int row = cy < sh / 3f ? 0 : cy > sh * 2f / 3f ? 2 : 1;
        return Anchor.values()[row * 3 + col];
    }

    /**
     * Snaps a candidate position against guide lines. {@code xs}/{@code ys} hold candidate guide coordinates
     * (screen edges/centre and other elements' edges/centres). Returns the snapped value; writes the matched
     * guide (or NaN) into {@code hit[0]}.
     */
    public static float snap1(float pos, float size, float[] guides, int count, float threshold, float[] hit) {
        float best = Float.NaN, bestDist = threshold + 1e-4f, bestGuide = Float.NaN;
        for (int i = 0; i < count; i++) {
            float g = guides[i];
            float dStart = Math.abs(pos - g), dEnd = Math.abs(pos + size - g), dMid = Math.abs(pos + size / 2 - g);
            if (dStart < bestDist) {
                bestDist = dStart;
                best = g;
                bestGuide = g;
            }
            if (dEnd < bestDist) {
                bestDist = dEnd;
                best = g - size;
                bestGuide = g;
            }
            if (dMid < bestDist) {
                bestDist = dMid;
                best = g - size / 2;
                bestGuide = g;
            }
        }
        hit[0] = bestGuide;
        return Float.isNaN(best) ? pos : best;
    }

    public static float grid(float pos, float cell) {
        return cell <= 0 ? pos : Math.round(pos / cell) * cell;
    }

    static float clamp(float v, float lo, float hi) {
        return Math.max(lo, Math.min(hi, v));
    }
}
