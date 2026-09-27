package dev.mw19.core.modules;

import dev.mw19.api.module.Category;
import dev.mw19.api.module.Module;
import dev.mw19.api.module.Rule;
import dev.mw19.core.Mw19;

/**
 * Chests, trapped chests, ender chests and copper chests are drawn as ordinary blocks (baked into the world mesh once)
 * instead of by a block entity renderer every frame. Their lids no longer animate. The platform adds or removes a
 * built-in resource pack with the models, which reloads resources once when toggled.
 */
public final class FastChestsModule extends Module {
    public FastChestsModule() {
        super("fast_chests", "Fast Chests", "Draw chests as normal blocks: much faster in storage rooms, but lids don't open visibly",
                Category.PERFORMANCE, Rule.ALLOWED, true);
    }

    @Override
    public boolean available() {
        return Mw19.get() == null || Mw19.get().platform.supports("fast_chests");
    }

    @Override
    public void onEnable() {
        Mw19.get().platform.setFastChests(true);
    }

    @Override
    public void onDisable() {
        Mw19.get().platform.setFastChests(false);
    }
}
