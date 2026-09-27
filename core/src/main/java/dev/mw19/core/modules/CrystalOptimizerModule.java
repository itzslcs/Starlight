package dev.mw19.core.modules;

import dev.mw19.api.module.Category;
import dev.mw19.api.module.Module;
import dev.mw19.api.module.Rule;
import dev.mw19.api.setting.BoolSetting;
import dev.mw19.core.Hooks;
import dev.mw19.core.Mw19;

/**
 * Marlow's Crystal Optimizer by Bram and Marlow (MIT), built in at the owner's request (DECISIONS D-028). An end
 * crystal your hit breaks stops blocking your next click at once, instead of after the server's reply arrives. It
 * announces itself to servers the way the original mod does, so a server can switch it off for its players.
 * The code is in the Fabric tree (port/CrystalOptimizer).
 */
public final class CrystalOptimizerModule extends Module {
    private final BoolSetting keepRender = add(new BoolSetting("keep_render", "Keep render",
            "Keep drawing a broken crystal until the server removes it (it stops blocking your crosshair either way)", false));

    public CrystalOptimizerModule() {
        super("crystal_optimizer", "Marlow's Crystal Optimizer",
                "By Bram & Marlow: a crystal you break stops blocking your next click at once. Servers can turn it off",
                Category.UTILITY, Rule.GRAY, false);
    }

    @Override
    public boolean available() {
        return Mw19.get() == null || Mw19.get().platform.supports("crystal_optimizer");
    }

    @Override
    public void onEnable() {
        Hooks.crystalKeepRender = keepRender.on();
        Hooks.crystalOptimizer = true;
    }

    @Override
    public void onTick() {
        Hooks.crystalKeepRender = keepRender.on();
    }

    @Override
    public void onDisable() {
        Hooks.crystalOptimizer = false;
    }
}
