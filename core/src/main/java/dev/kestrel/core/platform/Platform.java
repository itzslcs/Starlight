package dev.kestrel.core.platform;

import dev.kestrel.api.Logger;
import dev.kestrel.api.game.Game;

import java.nio.file.Path;

/**
 * Everything core needs from a Minecraft version. One implementation per target family
 * (fabric/src with Stonecutter conditionals; legacy/src for Forge 1.8.9). Game-thread only unless noted.
 */
public interface Platform extends Game {
    Logger logger();

    /** .minecraft (or the instance's game directory). Any thread. */
    Path gameDir();

    ScreenHost screens();

    ChatAccess chat();

    ModList mods();

    /** Server asked the client to hide coordinates (F3 reduced debug info). */
    boolean reducedDebugInfo();

    boolean hideGui();

    /** 0 first person, 1 third-person back, 2 third-person front. */
    int perspective();

    String clipboard();

    void setClipboard(String text);

    /** Vanilla key bindings (name, GLFW code) for conflict display. */
    void vanillaBindings(BindingSink sink);

    /** Opens a folder in the OS file manager (best effort). */
    void openFolder(Path dir);

    /** Takes a screenshot of the current frame into the vanilla screenshots folder (smoke/tests). */
    void screenshot(String name);

    /** Asks the game to close normally (smoke test end). */
    void quit();

    /** Opens the singleplayer world {@code folder}, creating it with {@code seed} if missing (smoke/bench). */
    void openWorld(String folder, long seed);

    interface BindingSink {
        void accept(String name, int key);
    }
}
