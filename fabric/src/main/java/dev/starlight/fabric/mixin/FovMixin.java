package dev.starlight.fabric.mixin;

import dev.starlight.core.Hooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Zoom: scales the final field of view. GameRenderer.getFov until 1.21.11, Camera.calculateFov from 26.1. */
//? if >=26.1 {
/*@Mixin(net.minecraft.client.Camera.class)
*///?} else {
@Mixin(net.minecraft.client.renderer.GameRenderer.class)
//?}
public abstract class FovMixin {
    //? if >=26.1 {
    /*@Inject(method = "calculateFov", at = @At("RETURN"), cancellable = true)
    private void starlight$zoom(float partialTick, CallbackInfoReturnable<Float> cir) {
        float m = Hooks.fov();
        if (m != 1f) cir.setReturnValue(cir.getReturnValue() * m);
    }
    *///?} elif >=1.21.2 {
    @Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
    private void starlight$zoom(net.minecraft.client.Camera camera, float partialTick, boolean useFovSetting, CallbackInfoReturnable<Float> cir) {
        float m = Hooks.fov();
        if (m != 1f) cir.setReturnValue(cir.getReturnValue() * m);
    }
    //?} else {
    /*@Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
    private void starlight$zoom(net.minecraft.client.Camera camera, float partialTick, boolean useFovSetting, CallbackInfoReturnable<Double> cir) {
        float m = Hooks.fov();
        if (m != 1f) cir.setReturnValue(cir.getReturnValue() * m);
    }
    *///?}
}
