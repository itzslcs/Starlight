package dev.mw19.api.hud;

/** Nine anchor points; offsets are measured from the anchor inwards. */
public enum Anchor {
    TOP_LEFT(0, 0), TOP(0.5f, 0), TOP_RIGHT(1, 0),
    LEFT(0, 0.5f), CENTER(0.5f, 0.5f), RIGHT(1, 0.5f),
    BOTTOM_LEFT(0, 1), BOTTOM(0.5f, 1), BOTTOM_RIGHT(1, 1);

    /** Fractions of screen size (and of element size) this anchor sits at. */
    public final float fx;
    public final float fy;

    Anchor(float fx, float fy) {
        this.fx = fx;
        this.fy = fy;
    }
}
