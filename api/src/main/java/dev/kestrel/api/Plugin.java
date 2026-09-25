package dev.kestrel.api;

/**
 * Entry point of an external plugin jar (declared as "main" in plugin.json).
 * Plugins run inside the game process with the same privileges as the mod itself.
 */
public interface Plugin {
    void onEnable(PluginContext ctx) throws Exception;

    default void onDisable() {}
}
