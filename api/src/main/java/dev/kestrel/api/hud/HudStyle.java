package dev.kestrel.api.hud;

/** Per-element colours chosen by the user in the HUD editor. */
public interface HudStyle {
    int textColor();

    int accentColor();

    boolean textShadow();
}
