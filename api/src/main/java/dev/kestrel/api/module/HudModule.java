package dev.kestrel.api.module;

import dev.kestrel.api.hud.Anchor;
import dev.kestrel.api.hud.HudStyle;
import dev.kestrel.api.render.Renderer;

/**
 * A module that draws a HUD element. The host positions, scales, fades and draws the background;
 * the module draws its content in local space (0,0)-(width(),height()).
 */
public abstract class HudModule extends Module {
    private final Anchor defaultAnchor;
    private final float defaultX;
    private final float defaultY;

    protected HudModule(String id, String name, String description, Rule rule, boolean defaultEnabled,
                        Anchor defaultAnchor, float defaultX, float defaultY) {
        super(id, name, description, Category.HUD, rule, defaultEnabled);
        this.defaultAnchor = defaultAnchor;
        this.defaultX = defaultX;
        this.defaultY = defaultY;
    }

    /** Content width in GUI units at scale 1. Must be cheap and allocation-free: called every frame. */
    public abstract float width(Renderer r);

    public abstract float height(Renderer r);

    /**
     * Draws the content. {@code preview} is true in the HUD editor, where modules without live data
     * (e.g. no armor equipped) should draw placeholder content.
     */
    public abstract void render(Renderer r, HudStyle style, boolean preview);

    /**
     * False hides the element this frame (no box, no size). Called before width/height every frame;
     * return true in {@code preview} so the HUD editor can show a placeholder.
     */
    public boolean visible(boolean preview) {
        return true;
    }

    /** Whether the host should draw the configurable background box. */
    public boolean wantsBackground() {
        return true;
    }

    public final Anchor defaultAnchor() {
        return defaultAnchor;
    }

    public final float defaultX() {
        return defaultX;
    }

    public final float defaultY() {
        return defaultY;
    }
}
