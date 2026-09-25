package dev.kestrel.fabric.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.kestrel.core.Hooks;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Zoom (FOV multiplier) and damage tilt strength. */
@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    //? if >=1.21.2 {
    @Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
    private void kestrel$zoom(Camera camera, float partialTick, boolean useFovSetting, CallbackInfoReturnable<Float> cir) {
        float m = Hooks.fov();
        if (m != 1f) cir.setReturnValue(cir.getReturnValue() * m);
    }
    //?} else {
    /*@Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
    private void kestrel$zoom(Camera camera, float partialTick, boolean useFovSetting, CallbackInfoReturnable<Double> cir) {
        float m = Hooks.fov();
        if (m != 1f) cir.setReturnValue(cir.getReturnValue() * m);
    }
    *///?}

    @ModifyExpressionValue(method = "bobHurt", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/OptionInstance;get()Ljava/lang/Object;", ordinal = 0),
            slice = @Slice(from = @At(value = "INVOKE", target = "Lnet/minecraft/client/Options;damageTiltStrength()Lnet/minecraft/client/OptionInstance;")))
    private Object kestrel$tilt(Object original) {
        float k = Hooks.damageTilt;
        return k == 1f || !(original instanceof Double) ? original : (Object) ((Double) original * k);
    }
}
