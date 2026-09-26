package dev.mw19.core.platform;

public interface ScreenHost {
    enum Kind { NONE, TITLE, PAUSE, CHAT, INVENTORY, OURS, OTHER }

    /** Show our GUI screen (hosts {@link dev.mw19.core.gui.GuiRoot}). */
    void openGui();

    /** Close our screen if it is open, returning to {@code parent} (or the game). */
    void closeGui();

    Kind current();

    /** Vanilla screens the home screen opens, with the current screen as their parent. */
    void openSingleplayer();

    void openMultiplayer();

    void openOptions();

    /** A mod list if one exists (Mod Menu on Fabric, Forge's own on 1.8.9); false when there is none. */
    boolean hasModList();

    void openModList();

    /** Shows Minecraft's own title screen once, bypassing the home screen (buttons other mods add live there). */
    void openVanillaTitle();

    /** Scaled (GUI-unit) screen size and physical pixels per unit. */
    int width();

    int height();

    float guiScale();
}
