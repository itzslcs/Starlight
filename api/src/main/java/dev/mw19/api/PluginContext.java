package dev.mw19.api;

import dev.mw19.api.event.Events;
import dev.mw19.api.game.Game;
import dev.mw19.api.module.Module;
import dev.mw19.api.name.NameTags;
import dev.mw19.api.net.Http;

/** Everything a plugin may touch. Deliberately has no access to the session/access token. */
public interface PluginContext {
    String pluginId();

    /** Host API version, e.g. "1.0". */
    String apiVersion();

    Logger logger();

    Events events();

    Game game();

    Scheduler scheduler();

    ConfigStore config();

    NameTags nameTags();

    Http http();

    Gui gui();

    /** Registers a module (or HudModule); it appears in the Mods page with its settings. */
    void registerModule(Module module);
}
