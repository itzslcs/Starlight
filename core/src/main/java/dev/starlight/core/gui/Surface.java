package dev.starlight.core.gui;

import dev.starlight.core.render.RenderBackend;

/**
 * A full-screen UI the platform's screen class hosts: the Starlight menu ({@link GuiRoot}) or the home screen
 * ({@link TitleUi}). One host class per platform forwards render and input here (vanilla GUI units).
 */
public interface Surface {
    void render(RenderBackend backend, int screenW, int screenH, float mouseX, float mouseY);

    boolean mouseClicked(float mx, float my, int button);

    boolean mouseReleased(float mx, float my, int button);

    boolean mouseDragged(float mx, float my, int button);

    boolean mouseScrolled(float mx, float my, double amount);

    boolean keyPressed(int key, int mods);

    boolean charTyped(char c);

    void onOpen();

    void onClose();

    /** Escape closes the menu; the home screen has nowhere to go back to. */
    boolean closesOnEscape();

    /** Whether the host should draw vanilla's background (blur/dim) behind this surface. */
    boolean wantsVanillaBackground();

    /** Whether the host should draw the title-screen panorama (no blur) behind this surface. */
    default boolean wantsPanorama() {
        return false;
    }

    /** Files dropped onto the game window while this surface is open (platforms that support it). */
    default void filesDropped(java.util.List<java.nio.file.Path> files) {}
}
