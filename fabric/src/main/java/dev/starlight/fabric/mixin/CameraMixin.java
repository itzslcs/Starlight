package dev.starlight.fabric.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.starlight.core.Hooks;
import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Freelook: the camera takes its angles from Hooks while active (every getView*Rot read). The reads live in
 * Camera.setup until 1.21.11 and in Camera.alignWithEntity from 26.1.
 */
@Mixin(Camera.class)
public abstract class CameraMixin {
    //? if >=26.1 {
    /*@ModifyExpressionValue(method = "alignWithEntity", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;getViewYRot(F)F"))
    *///?} else {
    @ModifyExpressionValue(method = "setup", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;getViewYRot(F)F"))
    //?}
    private float starlight$yaw(float original) {
        return Hooks.freelook ? Hooks.freelookYaw : original;
    }

    //? if >=26.1 {
    /*@ModifyExpressionValue(method = "alignWithEntity", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;getViewXRot(F)F"))
    *///?} else {
    @ModifyExpressionValue(method = "setup", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;getViewXRot(F)F"))
    //?}
    private float starlight$pitch(float original) {
        return Hooks.freelook ? Hooks.freelookPitch : original;
    }
}
