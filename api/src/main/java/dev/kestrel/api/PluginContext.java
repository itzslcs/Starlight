package dev.kestrel.api;

import dev.kestrel.api.event.Events;
import dev.kestrel.api.game.Game;
import dev.kestrel.api.module.Module;
import dev.kestrel.api.name.NameTags;
import dev.kestrel.api.net.Http;

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
