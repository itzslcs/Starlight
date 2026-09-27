package dev.mw19.core.modules;

import dev.mw19.api.module.Category;
import dev.mw19.api.module.Module;
import dev.mw19.api.module.Rule;
import dev.mw19.core.Hooks;

/**
 * Skips drawing mobs, items and other entities that are fully hidden behind solid blocks (Occlusion). Players, glowing
 * entities and anything showing a name tag are always drawn, because vanilla shows those through walls. Block entities
 * (chests, signs, banners, heads, beds...) are culled the same way, with a box one block larger than the block (banners
 * and beds reach past it); ones the game draws even off screen (beacon beams, end gateways) are never skipped.
 */
public final class EntityCullingModule extends Module {
    private final dev.mw19.api.setting.BoolSetting blocks = add(new dev.mw19.api.setting.BoolSetting("block_entities", "Block entities",
            "Also skip chests, signs, banners and heads hidden behind blocks", true));

    public EntityCullingModule() {
        super("entity_culling", "Entity Culling", "Skip drawing mobs, items, chests and signs hidden behind blocks (more FPS in busy areas)",
                Category.PERFORMANCE, Rule.ALLOWED, true);
    }

    @Override
    public void onEnable() {
        Hooks.entityCulling = true;
        Hooks.blockEntityCulling = blocks.on();
    }

    @Override
    public void onTick() {
        Hooks.blockEntityCulling = blocks.on();
    }

    @Override
    public void onDisable() {
        Hooks.entityCulling = false;
        Hooks.blockEntityCulling = false;
    }
}
