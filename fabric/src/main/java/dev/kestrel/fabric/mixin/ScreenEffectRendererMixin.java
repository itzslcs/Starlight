package dev.kestrel.fabric.mixin;

import dev.kestrel.core.Hooks;
import net.minecraft.client.renderer.ScreenEffectRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/** Low fire: the overlay's vertical offset (-0.3) and alpha (0.9) are plain constants in every 1.21.x renderFire. */
@Mixin(ScreenEffectRenderer.class)
public abstract class ScreenEffectRendererMixin {
    @ModifyConstant(method = "renderFire", constant = @Constant(floatValue = -0.3F))
    private static float kestrel$fireY(float v) {
        return v - Hooks.fireOffset;
    }

    @ModifyConstant(method = "renderFire", constant = @Constant(floatValue = 0.9F))
    private static float kestrel$fireAlpha(float v) {
        return v * Hooks.fireOpacity;
    }
}
