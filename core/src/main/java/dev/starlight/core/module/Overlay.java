package dev.starlight.core.module;

import dev.starlight.core.render.Gfx;

/** A module that draws directly on the HUD layer (not a positionable element), e.g. the crosshair. */
public interface Overlay {
    void renderOverlay(Gfx g, float screenW, float screenH);
}
