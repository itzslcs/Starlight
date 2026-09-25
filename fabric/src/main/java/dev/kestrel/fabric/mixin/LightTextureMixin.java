package dev.kestrel.fabric.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.kestrel.core.Hooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Slice;

/**
 * Brightness: substitute the gamma read (the saved option is untouched). 26.1 moved the lightmap maths into
 * LightmapRenderStateExtractor.extract; the Options.gamma().get() read is the same.
 */
//? if >=26.1 {
/*@Mixin(net.minecraft.client.renderer.LightmapRenderStateExtractor.class)
*///?} else {
@Mixin(net.minecraft.client.renderer.LightTexture.class)
//?}
public abstract class LightTextureMixin {
    //? if >=26.1 {
    /*@ModifyExpressionValue(method = "extract", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/OptionInstance;get()Ljava/lang/Object;", ordinal = 0),
    *///?} else {
    @ModifyExpressionValue(method = "updateLightTexture", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/OptionInstance;get()Ljava/lang/Object;", ordinal = 0),
    //?}
            slice = @Slice(from = @At(value = "INVOKE", target = "Lnet/minecraft/client/Options;gamma()Lnet/minecraft/client/OptionInstance;")))
    private Object kestrel$gamma(Object original) {
        double g = Hooks.gamma;
        return Double.isNaN(g) ? original : (Object) g;
    }
}
