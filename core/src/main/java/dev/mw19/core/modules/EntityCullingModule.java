package dev.mw19.core.modules;

import dev.mw19.api.module.Category;
import dev.mw19.api.module.Module;
import dev.mw19.api.module.Rule;
import dev.mw19.core.Hooks;

/**
 * Skips drawing mobs, items and other entities that are fully hidden behind solid blocks (Occlusion). Players, glowing
 * entities and anything showing a name tag are always drawn, because vanilla shows those through walls.
 */
public final class EntityCullingModule extends Module {
    public EntityCullingModule() {
        super("entity_culling", "Entity Culling", "Skip drawing mobs and items hidden behind blocks (more FPS in busy areas)",
                Category.PERFORMANCE, Rule.ALLOWED, true);
    }

    @Override
    public void onEnable() {
        Hooks.entityCulling = true;
    }

    @Override
    public void onDisable() {
        Hooks.entityCulling = false;
    }
}
