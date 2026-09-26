package dev.mw19.fabric.mixin;

import dev.mw19.core.Hooks;
import net.minecraft.client.renderer.ScreenEffectRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/**
 * Low fire: the overlay's vertical offset (-0.3) and alpha are plain constants. Until 26.1 both sit in renderFire
 * (alpha as float 0.9); from 26.2 the offset is in the submitFire lambda and the alpha is the packed colour
 * 0xE5FFFFFF in buildFireQuad.
 */
@Mixin(ScreenEffectRenderer.class)
public abstract class ScreenEffectRendererMixin {
    //? if >=26.2 {
    /*@ModifyConstant(method = "lambda$submitFire$0", constant = @Constant(floatValue = -0.3F))
    private static float mw19$fireY(float v) {
        return v - Hooks.fireOffset;
    }

    @ModifyConstant(method = "buildFireQuad", constant = @Constant(intValue = 0xE5FFFFFF))
    private static int mw19$fireAlpha(int argb) {
        return (Math.round(0xE5 * Hooks.fireOpacity) << 24) | (argb & 0xFFFFFF);
    }
    *///?} else {
    @ModifyConstant(method = "renderFire", constant = @Constant(floatValue = -0.3F))
    private static float mw19$fireY(float v) {
        return v - Hooks.fireOffset;
    }

    @ModifyConstant(method = "renderFire", constant = @Constant(floatValue = 0.9F))
    private static float mw19$fireAlpha(float v) {
        return v * Hooks.fireOpacity;
    }
    //?}
}
