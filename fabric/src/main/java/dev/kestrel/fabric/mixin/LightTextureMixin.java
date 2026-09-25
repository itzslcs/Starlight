package dev.kestrel.fabric.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.kestrel.core.Hooks;
import net.minecraft.client.renderer.LightTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Slice;

/** Brightness: substitute the gamma read in updateLightTexture (the saved option is untouched). */
@Mixin(LightTexture.class)
public abstract class LightTextureMixin {
    @ModifyExpressionValue(method = "updateLightTexture", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/OptionInstance;get()Ljava/lang/Object;", ordinal = 0),
            slice = @Slice(from = @At(value = "INVOKE", target = "Lnet/minecraft/client/Options;gamma()Lnet/minecraft/client/OptionInstance;")))
    private Object kestrel$gamma(Object original) {
        double g = Hooks.gamma;
        return Double.isNaN(g) ? original : (Object) g;
    }
}
