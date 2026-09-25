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

    // ---- data for HUD / utility modules (Phase 4) ----

    /** Active visible status effects. {@code durationTicks} is -1 for infinite. */
    void effects(EffectSink sink);

    interface EffectSink {
        void accept(String name, int amplifier, int durationTicks, int rgb, boolean beneficial);
    }

    /** A cached 1-count stack of {@code itemId} for drawing icons (empty ref if the id is unknown). */
    dev.kestrel.api.render.ItemRef itemIcon(String itemId);

    /** Attack cooldown progress in [0,1] (always 1 on 1.8.9). */
    float attackCooldown();

    /** Remaining hurt-animation ticks of the local player (> 0 right after taking damage). */
    int hurtTime();

    boolean sprinting();

    boolean sneaking();

    /**
     * Toggle sprint/sneak. Modern targets switch vanilla's own "Toggle" key mode; 1.8.9 latches the key itself.
     * Only called by the GRAY toggle modules.
     */
    void setToggle(Binding binding, boolean enabled);

    /** Whether the toggle for {@code binding} is currently latched on. */
    boolean toggleLatched(Binding binding);

    void setSmoothCamera(boolean on);

    /** Sets the perspective (0 first, 1 back, 2 front). */
    void setPerspective(int perspective);

    /** World day time in ticks, or -1 without a world. */
    long dayTime();

    java.nio.file.Path screenshotsDir();

    /** Local UI "ding" (never sent anywhere). */
    void playPing();

    /** Smoke/test only: feeds a line through the same path as a chat line received from the server. */
    void debugIncomingChat(String text);
}
