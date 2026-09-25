package dev.kestrel.core.render;

import dev.kestrel.api.render.ItemRef;

/**
 * Per-version drawing primitives. All coordinates are ABSOLUTE GUI units (core applies translate/scale itself),
 * so backends never depend on how a given version transforms scissor rects or poses.
 * Implementations must not allocate per call in steady state.
 */
public interface RenderBackend {
    void fill(float x1, float y1, float x2, float y2, int argb);

    void gradient(float x1, float y1, float x2, float y2, int topArgb, int bottomArgb);

    /** Draws text with its top-left at (x,y), scaled by {@code scale} around that point. */
    void text(String text, float x, float y, float scale, int argb, boolean shadow);

    /** Unscaled width in GUI units. */
    float textWidth(String text);

    float lineHeight();

    void item(ItemRef item, float x, float y, float scale);

    /** Replace the current clip with this absolute rect (core owns the stack). */
    void scissor(float x1, float y1, float x2, float y2);

    void endScissor();

    float guiScale();

    /** Submit anything batched (called at the end of every pass). */
    default void flush() {}
}
