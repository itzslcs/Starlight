package dev.starlight.core.modules;

import dev.starlight.api.module.Category;
import dev.starlight.api.module.Module;
import dev.starlight.api.module.Rule;
import dev.starlight.core.Hooks;
import dev.starlight.core.Starlight;

/**
 * Hero's Anchor Optimizer by HerobaneNair (MIT), built in at the owner's request (DECISIONS D-028). Right-clicking a
 * charged respawn anchor that will explode turns it into a see-through ghost block at once, which the next block can
 * replace, instead of waiting for the server's explosion. Only on servers, as the original.
 * The code is in the Fabric tree (port/AnchorOptimizer).
 */
public final class AnchorOptimizerModule extends Module {
    public AnchorOptimizerModule() {
        super("anchor_optimizer", "Hero's Anchor Optimizer",
                "By HerobaneNair: a respawn anchor you blow up turns into a ghost block at once, so you can build there right away",
                Category.UTILITY, Rule.GRAY, false);
    }

    @Override
    public boolean available() {
        return Starlight.get() == null || Starlight.get().platform.supports("anchor_optimizer");
    }

    @Override
    public void onEnable() {
        Hooks.anchorOptimizer = true;
    }

    @Override
    public void onDisable() {
        Hooks.anchorOptimizer = false;
    }
}
