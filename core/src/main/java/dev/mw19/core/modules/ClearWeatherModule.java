package dev.mw19.core.modules;

import dev.mw19.api.module.Category;
import dev.mw19.api.module.Module;
import dev.mw19.api.module.Rule;
import dev.mw19.core.Hooks;

/** Client-side clear skies: no rain or thunder rendering or sounds. The server's weather is unchanged. */
public final class ClearWeatherModule extends Module {
    public ClearWeatherModule() {
        super("clear_weather", "Clear Weather", "Hide rain and thunder on your screen only (also saves a few frames)", Category.VISUAL, Rule.ALLOWED, false);
    }

    @Override
    public void onEnable() {
        Hooks.clearWeather = true;
    }

    @Override
    public void onDisable() {
        Hooks.clearWeather = false;
    }
}
