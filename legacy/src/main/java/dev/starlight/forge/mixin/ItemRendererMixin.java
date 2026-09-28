package dev.starlight.forge.mixin;

import dev.starlight.core.Hooks;
import net.minecraft.client.renderer.ItemRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/** Low fire: renderFireInFirstPerson's vertical offset (-0.3) and alpha (0.9). */
@Mixin(ItemRenderer.class)
public abstract class ItemRendererMixin {
    @ModifyConstant(method = "renderFireInFirstPerson", constant = @Constant(floatValue = -0.3F))
    private float starlight$fireY(float v) {
        return v - Hooks.fireOffset;
    }

    @ModifyConstant(method = "renderFireInFirstPerson", constant = @Constant(floatValue = 0.9F))
    private float starlight$fireAlpha(float v) {
        return v * Hooks.fireOpacity;
    }
}
