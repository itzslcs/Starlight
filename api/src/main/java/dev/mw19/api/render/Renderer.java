package dev.mw19.api.render;

/**
 * 2D drawing in GUI units (vanilla scaled pixels, floats allowed). Colours are ARGB.
 * Text accepts legacy '§' formatting codes. Implementations never allocate per call.
 */
public interface Renderer {
    void rect(float x1, float y1, float x2, float y2, int argb);

    /** Vertical gradient. */
    void gradient(float x1, float y1, float x2, float y2, int topArgb, int bottomArgb);

    void roundRect(float x, float y, float w, float h, float radius, int argb);

    void roundOutline(float x, float y, float w, float h, float radius, float thickness, int argb);

    void text(String text, float x, float y, int argb, boolean shadow);

    float textWidth(String text);

    /** Height of one line of text (9 on every target). */
    float lineHeight();

    /** 16x16 item icon with count/durability decorations. */
    void item(ItemRef item, float x, float y);

    void pushClip(float x, float y, float w, float h);

    void popClip();

    void push();

    void pop();

    void translate(float x, float y);

    void scale(float s);

    /** Physical pixels per GUI unit. */
    float guiScale();

    /** Animation clock (ms). */
    long millis();
}
