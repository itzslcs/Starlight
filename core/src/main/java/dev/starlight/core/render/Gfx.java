package dev.starlight.core.render;

import dev.starlight.api.render.ItemRef;
import dev.starlight.api.render.Renderer;
import dev.starlight.api.util.Colors;

/**
 * Renderer used by the GUI, HUD and plugins. Tracks translate/scale, alpha and clip stacks itself and hands
 * absolute GUI coordinates to the per-version {@link RenderBackend}. Rounded shapes are scanline fills at
 * physical-pixel resolution with one alpha-weighted edge pixel per row (DECISIONS D-008).
 * Allocation-free after construction; one instance per render pass, reused every frame.
 */
public final class Gfx implements Renderer {
    private static final int DEPTH = 32;

    private RenderBackend b;
    /**
     * The last backend, kept after {@link #end()}: widgets measure text while handling input too, which happens
     * between render passes. Drawing still requires an active pass ({@code b}). (debug-log 2026-09-26: every click on
     * the Mods page threw because measuring used {@code b}.)
     */
    private RenderBackend measure;
    private long millis;
    private float gs = 1f;
    private float px = 1f; // GUI units per physical pixel

    private final float[] txStack = new float[DEPTH], tyStack = new float[DEPTH], sStack = new float[DEPTH];
    private int tDepth;
    private float tx, ty, s = 1f;

    private final float[] alphaStack = new float[DEPTH];
    private int aDepth;
    private float alpha = 1f;

    private final float[] clip = new float[DEPTH * 4];
    private int cDepth;

    /** Starts a pass. Must be paired with {@link #end()}. */
    public Gfx begin(RenderBackend backend, long frameMillis) {
        this.b = backend;
        this.measure = backend;
        this.millis = frameMillis;
        this.gs = Math.max(0.25f, backend.guiScale());
        this.px = 1f / gs;
        tx = ty = 0;
        s = 1f;
        tDepth = 0;
        alpha = 1f;
        aDepth = 0;
        cDepth = 0;
        return this;
    }

    /** Ends a pass, leaving no scissor behind even if a caller forgot to pop. */
    public void end() {
        b.flush();
        if (cDepth > 0) b.endScissor();
        cDepth = 0;
        b = null;
    }

    public RenderBackend backend() {
        return b;
    }

    // ---------------------------------------------------------------- transforms / alpha

    @Override
    public void push() {
        if (tDepth == DEPTH) throw new IllegalStateException("transform stack overflow");
        txStack[tDepth] = tx;
        tyStack[tDepth] = ty;
        sStack[tDepth] = s;
        tDepth++;
    }

    @Override
    public void pop() {
        if (tDepth == 0) throw new IllegalStateException("transform stack underflow");
        tDepth--;
        tx = txStack[tDepth];
        ty = tyStack[tDepth];
        s = sStack[tDepth];
    }

    @Override
    public void translate(float x, float y) {
        tx += x * s;
        ty += y * s;
    }

    @Override
    public void scale(float f) {
        s *= f;
    }

    public void pushAlpha(float f) {
        if (aDepth == DEPTH) throw new IllegalStateException("alpha stack overflow");
        alphaStack[aDepth++] = alpha;
        alpha *= Math.max(0f, Math.min(1f, f));
    }

    public void popAlpha() {
        alpha = alphaStack[--aDepth];
    }

    public float alpha() {
        return alpha;
    }

    private int c(int argb) {
        return alpha >= 0.999f ? argb : Colors.fade(argb, alpha);
    }

    private float ax(float x) {
        return tx + x * s;
    }

    private float ay(float y) {
        return ty + y * s;
    }

    private float snap(float v) {
        return Math.round(v * gs) * px;
    }

    // ---------------------------------------------------------------- shapes

    @Override
    public void rect(float x1, float y1, float x2, float y2, int argb) {
        int col = c(argb);
        if ((col >>> 24) == 0) return;
        b.fill(snap(ax(x1)), snap(ay(y1)), snap(ax(x2)), snap(ay(y2)), col);
    }

    @Override
    public void gradient(float x1, float y1, float x2, float y2, int topArgb, int bottomArgb) {
        b.gradient(snap(ax(x1)), snap(ay(y1)), snap(ax(x2)), snap(ay(y2)), c(topArgb), c(bottomArgb));
    }

    /** Horizontal gradient built from 1-physical-pixel columns (colour picker only, not hot). */
    public void gradientH(float x1, float y1, float x2, float y2, int leftArgb, int rightArgb) {
        float X1 = snap(ax(x1)), X2 = snap(ax(x2)), Y1 = snap(ay(y1)), Y2 = snap(ay(y2));
        int cols = Math.max(1, Math.round((X2 - X1) * gs));
        for (int i = 0; i < cols; i++) {
            float t = cols == 1 ? 0 : i / (float) (cols - 1);
            b.fill(X1 + i * px, Y1, X1 + (i + 1) * px, Y2, c(Colors.lerp(leftArgb, rightArgb, t)));
        }
    }

    /**
     * Box with softened corners: the corner pixels are cut (a chamfer of at most 2 physical pixels), so any box costs 3
     * fills. Menus and HUD boxes used to be scanline-filled per pixel row (dozens of fills each; the whole menu ran into
     * thousands per frame), which is what made the menu slow (2026-09-26).
     */
    @Override
    public void roundRect(float x, float y, float w, float h, float radius, int argb) {
        int col = c(argb);
        if ((col >>> 24) == 0 || w <= 0 || h <= 0) return;
        float X0 = snap(ax(x)), Y0 = snap(ay(y)), X1 = snap(ax(x + w)), Y1 = snap(ay(y + h));
        int k = chamfer(radius, X1 - X0, Y1 - Y0);
        if (k == 0) {
            b.fill(X0, Y0, X1, Y1, col);
            return;
        }
        float c = k * px;
        b.fill(X0 + c, Y0, X1 - c, Y1, col);
        b.fill(X0, Y0 + c, X0 + c, Y1 - c, col);
        b.fill(X1 - c, Y0 + c, X1, Y1 - c, col);
    }

    /** Outline with the same cut corners: 4 fills. */
    @Override
    public void roundOutline(float x, float y, float w, float h, float radius, float thickness, int argb) {
        int col = c(argb);
        if ((col >>> 24) == 0 || w <= 0 || h <= 0) return;
        float X0 = snap(ax(x)), Y0 = snap(ay(y)), X1 = snap(ax(x + w)), Y1 = snap(ay(y + h));
        float t = Math.max(1, Math.round(thickness * s * gs)) * px, c = chamfer(radius, X1 - X0, Y1 - Y0) * px;
        b.fill(X0 + c, Y0, X1 - c, Y0 + t, col);
        b.fill(X0 + c, Y1 - t, X1 - c, Y1, col);
        b.fill(X0, Y0 + c, X0 + t, Y1 - c, col);
        b.fill(X1 - t, Y0 + c, X1, Y1 - c, col);
    }

    /** Corner cut in physical pixels: none for radius 0, 1 for small radii, 2 for larger ones (never over a third of the box). */
    private int chamfer(float radius, float wGui, float hGui) {
        if (radius <= 0) return 0;
        int k = radius * s * gs >= 5 ? 2 : 1;
        int limit = (int) (Math.min(wGui, hGui) * gs / 3);
        return Math.min(k, Math.max(0, limit));
    }

    /** True circular corners, scanline-filled per physical pixel row (costly: only for shapes that must be round). */
    public void roundRectSmooth(float x, float y, float w, float h, float radius, int argb) {
        int col = c(argb);
        if ((col >>> 24) == 0 || w <= 0 || h <= 0) return;
        float X0 = snap(ax(x)), Y0 = snap(ay(y)), X1 = snap(ax(x + w)), Y1 = snap(ay(y + h));
        int wPx = Math.round((X1 - X0) * gs), hPx = Math.round((Y1 - Y0) * gs);
        int r = Math.min(Math.round(radius * s * gs), Math.min(wPx, hPx) / 2);
        if (r <= 0) {
            b.fill(X0, Y0, X1, Y1, col);
            return;
        }
        b.fill(X0, Y0 + r * px, X1, Y1 - r * px, col);
        int a = col >>> 24, rgb = col & 0xFFFFFF;
        // Edge anti-aliasing only where it is visible (radius >= 7 physical px); below that, rows with the same
        // inset are merged into one fill. A HUD key box drops from ~25 fills to ~5.
        boolean aa = r >= 7;
        int runStart = 0, runSolid = -1;
        for (int i = 0; i <= r; i++) {
            int solid = 0;
            float cover = 1f;
            int inset = 0;
            if (i < r) {
                float dy = r - i - 0.5f;
                float dx = r - (float) Math.sqrt(r * (float) r - dy * dy);
                inset = (int) dx;
                cover = 1f - (dx - inset);
                solid = cover >= 0.999f || !aa ? (cover >= 0.5f || aa ? inset : inset + 1) : inset + 1;
            }
            if (!aa) {
                if (i < r && solid == runSolid) continue;
                if (runSolid >= 0) {
                    int rows = i - runStart;
                    float l = X0 + runSolid * px, rr = X1 - runSolid * px;
                    if (rr > l) {
                        b.fill(l, Y0 + runStart * px, rr, Y0 + (runStart + rows) * px, col);
                        b.fill(l, Y1 - (runStart + rows) * px, rr, Y1 - runStart * px, col);
                    }
                }
                runStart = i;
                runSolid = solid;
                continue;
            }
            if (i == r) break;
            float top = Y0 + i * px, bot = Y1 - (i + 1) * px;
            float l = X0 + solid * px, rr = X1 - solid * px;
            if (rr > l) {
                b.fill(l, top, rr, top + px, col);
                b.fill(l, bot, rr, bot + px, col);
            }
            if (solid != inset) {
                int edge = (Math.round(a * cover) << 24) | rgb;
                if ((edge >>> 24) != 0) {
                    float el = X0 + inset * px, er = X1 - (inset + 1) * px;
                    b.fill(el, top, el + px, top + px, edge);
                    b.fill(er, top, er + px, top + px, edge);
                    b.fill(el, bot, el + px, bot + px, edge);
                    b.fill(er, bot, er + px, bot + px, edge);
                }
            }
        }
    }

    /** Round outline, per physical pixel row (the Circle crosshair). */
    public void roundOutlineSmooth(float x, float y, float w, float h, float radius, float thickness, int argb) {
        int col = c(argb);
        if ((col >>> 24) == 0 || w <= 0 || h <= 0) return;
        float X0 = snap(ax(x)), Y0 = snap(ay(y)), X1 = snap(ax(x + w)), Y1 = snap(ay(y + h));
        int wPx = Math.round((X1 - X0) * gs), hPx = Math.round((Y1 - Y0) * gs);
        int t = Math.max(1, Math.round(thickness * s * gs));
        int r = Math.min(Math.round(radius * s * gs), Math.min(wPx, hPx) / 2);
        // straight edges
        b.fill(X0 + r * px, Y0, X1 - r * px, Y0 + t * px, col);
        b.fill(X0 + r * px, Y1 - t * px, X1 - r * px, Y1, col);
        b.fill(X0, Y0 + r * px, X0 + t * px, Y1 - r * px, col);
        b.fill(X1 - t * px, Y0 + r * px, X1, Y1 - r * px, col);
        // corner arcs, row by row: span between the outer and inner circle
        int ri = r - t;
        for (int i = 0; i < r; i++) {
            float dy = r - i - 0.5f;
            int outer = Math.round(r - (float) Math.sqrt(Math.max(0f, r * (float) r - dy * dy)));
            int inner;
            if (ri <= 0 || dy >= ri) inner = r; // row above the inner circle: span reaches the straight part
            else inner = Math.round(r - (float) Math.sqrt(ri * (float) ri - dy * dy));
            if (inner <= outer) inner = outer + 1;
            float top = Y0 + i * px, bot = Y1 - (i + 1) * px;
            float l0 = X0 + outer * px, l1 = X0 + inner * px, r0 = X1 - inner * px, r1 = X1 - outer * px;
            b.fill(l0, top, l1, top + px, col);
            b.fill(r0, top, r1, top + px, col);
            b.fill(l0, bot, l1, bot + px, col);
            b.fill(r0, bot, r1, bot + px, col);
        }
    }

    // ---------------------------------------------------------------- text / items

    @Override
    public void text(String text, float x, float y, int argb, boolean shadow) {
        if (text == null || text.isEmpty()) return;
        int col = c(argb);
        if ((col >>> 24) < 5) return; // vanilla font treats alpha < 4 as opaque
        b.text(text, snap(ax(x)), snap(ay(y)), s, col, shadow);
    }

    public void textCentered(String text, float cx, float y, int argb, boolean shadow) {
        text(text, cx - textWidth(text) / 2f, y, argb, shadow);
    }

    public void textRight(String text, float right, float y, int argb, boolean shadow) {
        text(text, right - textWidth(text), y, argb, shadow);
    }

    /** Text scaled by {@code scale} relative to the current transform. */
    public void text(String text, float x, float y, float scale, int argb, boolean shadow) {
        push();
        translate(x, y);
        scale(scale);
        text(text, 0, 0, argb, shadow);
        pop();
    }

    @Override
    public float textWidth(String text) {
        if (text == null) return 0;
        return measure != null ? measure.textWidth(text) : text.length() * 6f; // estimate before the first frame
    }

    @Override
    public float lineHeight() {
        return measure != null ? measure.lineHeight() : 9f;
    }

    /** Shortens {@code text} with an ellipsis to fit {@code maxWidth}. Allocates: GUI use only, never per HUD frame. */
    public String ellipsize(String text, float maxWidth) {
        if (textWidth(text) <= maxWidth) return text;
        String dots = "…";
        float dw = textWidth(dots);
        int end = text.length();
        while (end > 0 && textWidth(text.substring(0, end)) + dw > maxWidth) end--;
        return text.substring(0, end) + dots;
    }

    @Override
    public void item(ItemRef item, float x, float y) {
        if (item == null || item.isEmpty() || alpha < 0.5f) return;
        b.item(item, ax(x), ay(y), s);
    }

    /** Part of a loaded image (UVs 0..1) stretched over the rect. */
    public void image(int handle, float x, float y, float w, float h, float u0, float v0, float u1, float v1) {
        if (handle == 0 || alpha < 0.02f) return;
        b.flush();
        b.image(handle, ax(x), ay(y), ax(x + w), ay(y + h), u0, v0, u1, v1, alpha);
    }

    public void image(int handle, float x, float y, float w, float h) {
        image(handle, x, y, w, h, 0, 0, 1, 1);
    }

    /** The player model fitted into the rect; {@code skin} 0 is the signed-in player's own skin. */
    public void player(float x, float y, float w, float h, int skin, boolean slim, float yaw, float pitch) {
        if (alpha < 0.5f) return;
        b.flush();
        b.player(ax(x), ay(y), ax(x + w), ay(y + h), skin, slim, yaw, pitch);
    }

    // ---------------------------------------------------------------- clipping

    @Override
    public void pushClip(float x, float y, float w, float h) {
        if (cDepth == DEPTH) throw new IllegalStateException("clip stack overflow");
        float x1 = ax(x), y1 = ay(y), x2 = ax(x + w), y2 = ay(y + h);
        if (cDepth > 0) {
            int p = (cDepth - 1) * 4;
            x1 = Math.max(x1, clip[p]);
            y1 = Math.max(y1, clip[p + 1]);
            x2 = Math.min(x2, clip[p + 2]);
            y2 = Math.min(y2, clip[p + 3]);
        }
        if (x2 < x1) x2 = x1;
        if (y2 < y1) y2 = y1;
        int o = cDepth * 4;
        clip[o] = x1;
        clip[o + 1] = y1;
        clip[o + 2] = x2;
        clip[o + 3] = y2;
        cDepth++;
        b.scissor(x1, y1, x2, y2);
    }

    @Override
    public void popClip() {
        if (cDepth == 0) throw new IllegalStateException("clip stack underflow");
        cDepth--;
        if (cDepth == 0) {
            b.endScissor();
        } else {
            int p = (cDepth - 1) * 4;
            b.scissor(clip[p], clip[p + 1], clip[p + 2], clip[p + 3]);
        }
    }

    @Override
    public float guiScale() {
        return gs;
    }

    @Override
    public long millis() {
        return millis;
    }
}
