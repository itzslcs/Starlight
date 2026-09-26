package dev.mw19.addons.tiertags;

import dev.mw19.api.Plugin;
import dev.mw19.api.PluginContext;

/** Entry point: one module that is also the name decorator. */
public final class TierTagsPlugin implements Plugin {
    @Override
    public void onEnable(PluginContext ctx) {
        TierTagsModule module = new TierTagsModule(ctx);
        ctx.registerModule(module);
        ctx.nameTags().register(module);
        ctx.gui().registerPanel("Tier Tags", module.panel());
    }
}
