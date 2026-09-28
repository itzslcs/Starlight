package dev.starlight.core.platform;

import dev.starlight.api.Logger;
import dev.starlight.api.game.Game;

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

    Skins skins();

    Packs packs();

    Host host();

    ChatAccess chat();

    ModList mods();

    /** Server asked the client to hide coordinates (F3 reduced debug info). */
    boolean reducedDebugInfo();

    boolean hideGui();

    /** 0 first person, 1 third-person back, 2 third-person front. */
    int perspective();

    String clipboard();

    void setClipboard(String text);

    /** Minecraft's own key bindings in its order, for the Keybinds page and conflict display. */
    void vanillaBindings(BindingSink sink);

    /** Minecraft's own key bindings by id ("key.hotbar.1") with canonical codes, in its order (bind profiles). */
    java.util.Map<String, Integer> vanillaBindingMap();

    /**
     * Sets Minecraft's key bindings by id to canonical codes and saves its options. Ids this version does not have are
     * skipped. Returns how many bindings changed. Game thread.
     */
    int applyVanillaBindings(java.util.Map<String, Integer> binds);

    /** Canonical code currently bound to {@code b} (GLFW key or {@code Keys.mouse(button)}), or {@code Keys.NONE}. */
    int bindingKey(Binding b);

    /**
     * Sets vanilla video options (ids and values from {@link dev.starlight.core.perf.VideoPreset}; ids this version lacks are
     * skipped) and saves the options file. Returns the previous values of the options it changed, so
     * {@link #restoreOptions} can undo it, even after a restart.
     */
    java.util.Map<String, String> applyVideo(java.util.Map<String, String> values);

    /** Puts back values returned by {@link #applyVideo} (unknown ids are ignored). */
    void restoreOptions(java.util.Map<String, String> previous);

    /** Current value of an integer video option ("renderDistance", "simulationDistance"), or -1 if unknown. */
    int videoOption(String id);

    /** The graphics device's name as the game reports it ("" if unknown). Game thread. */
    String gpuName();

    /** "integrated", "discrete", "cpu" or "virtual" where the game reports the device type (26.2+), else "". */
    String gpuKind();

    /** The game's graphics API preference where it has one (26.2+: "default", "opengl", "vulkan"), else null. */
    String graphicsApi();

    /** Sets that preference; the game applies it on the next start and falls back to OpenGL if Vulkan fails. */
    void setGraphicsApi(String api);

    /** Credits for third-party code in this build (the ported optimizers, MIT; VulkanMod, LGPL-3.0), or "" (About page). */
    default String bundledNotice() {
        return "";
    }

    /** One line on what renders now and at the next start (bundled VulkanMod), or "" (Performance page). */
    default String rendererStatus() {
        return "";
    }

    /** Opens a folder in the OS file manager (best effort). */
    void openFolder(Path dir);

    /** Takes a screenshot of the current frame into the vanilla screenshots folder (smoke/tests). */
    void screenshot(String name);

    /** Asks the game to close normally (smoke test end). */
    void quit();

    /** Opens the singleplayer world {@code folder}, creating it with {@code seed} if missing (smoke/bench). */
    void openWorld(String folder, long seed);

    interface BindingSink {
        /** {@code id} as in bind profiles ("key.hotbar.1"), shown name and category, canonical key and default key. */
        void accept(String id, String name, String category, int key, int defaultKey);
    }

    // ---- data for HUD / utility modules (Phase 4) ----

    /** Active visible status effects. {@code durationTicks} is -1 for infinite. */
    void effects(EffectSink sink);

    interface EffectSink {
        void accept(String name, int amplifier, int durationTicks, int rgb, boolean beneficial);
    }

    /** A cached 1-count stack of {@code itemId} for drawing icons (empty ref if the id is unknown). */
    dev.starlight.api.render.ItemRef itemIcon(String itemId);

    /** Attack cooldown progress in [0,1] (always 1 on 1.8.9). */
    float attackCooldown();

    /** Remaining hurt-animation ticks of the local player (> 0 right after taking damage). */
    int hurtTime();

    boolean sprinting();

    /** Vanilla's entity hitboxes, as F3+B toggles them. */
    void setHitboxes(boolean on);

    /** TNT Timer (tick): while on, primed TNT within {@code range} blocks shows its fuse; off removes the labels Starlight set. */
    void tntTimers(boolean on, int range);

    /** Distance from the player's eyes to the nearest point of the entity's hitbox, or -1. */
    double reachTo(int entityId);

    /** The local player's health and food (0 without a world). */
    float health();

    float maxHealth();

    int food();

    float saturation();

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

    /**
     * Fast Chests: adds or removes the built-in pack that draws chests as blocks and reloads resources when that changes
     * ({@code supports("fast_chests")}). Game thread.
     */
    default void setFastChests(boolean on) {}

    /** Smoke only: a platform self-check by name ("exploit"); "n/a" where it does not apply. */
    default String selfTest(String what) {
        return "n/a";
    }

    /** Smoke/test only: feeds a line through the same path as a chat line received from the server. */
    void debugIncomingChat(String text);
}
