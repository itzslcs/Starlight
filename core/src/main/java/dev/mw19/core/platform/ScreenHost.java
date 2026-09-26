package dev.mw19.core.platform;

public interface ScreenHost {
    enum Kind { NONE, TITLE, PAUSE, CHAT, INVENTORY, OURS, OTHER }

    /** Show our GUI screen (hosts {@link dev.mw19.core.gui.GuiRoot}). */
    void openGui();

    /** Close our screen if it is open, returning to {@code parent} (or the game). */
    void closeGui();

    Kind current();

    /** Scaled (GUI-unit) screen size and physical pixels per unit. */
    int width();

    int height();

    float guiScale();
}
