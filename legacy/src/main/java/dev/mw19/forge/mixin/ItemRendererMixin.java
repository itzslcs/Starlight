package dev.mw19.forge.mixin;

import dev.mw19.core.Hooks;
import net.minecraft.client.renderer.ItemRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/** Low fire: renderFireInFirstPerson's vertical offset (-0.3) and alpha (0.9). */
@Mixin(ItemRenderer.class)
public abstract class ItemRendererMixin {
    @ModifyConstant(method = "renderFireInFirstPerson", constant = @Constant(floatValue = -0.3F))
    private float mw19$fireY(float v) {
        return v - Hooks.fireOffset;
    }

    @ModifyConstant(method = "renderFireInFirstPerson", constant = @Constant(floatValue = 0.9F))
    private float mw19$fireAlpha(float v) {
        return v * Hooks.fireOpacity;
    }
}
