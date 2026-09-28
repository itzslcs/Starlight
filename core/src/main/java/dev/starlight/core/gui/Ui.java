package dev.starlight.core.gui;

import dev.starlight.core.Starlight;
import dev.starlight.core.render.Gfx;

/** Per-frame GUI context handed to every widget. One instance, reused. */
public final class Ui {
    public Gfx g;
    public Theme t;
    public Starlight k;
    public GuiRoot root;
    /** Mouse in GUI space (after menu scale). */
    public float mx, my;
    public long now;
    /** Tooltip requested this frame (the hovered widget sets it). */
    public String tooltip;

    /** Hit test that ignores popup blocking (used by the popup itself). */
    public boolean hoverPopup(float x, float y, float w, float h) {
        return mx >= x && my >= y && mx < x + w && my < y + h;
    }

    public boolean hover(float x, float y, float w, float h) {
        return mx >= x && my >= y && mx < x + w && my < y + h && (root == null || !root.blockedAt(mx, my));
    }
}
