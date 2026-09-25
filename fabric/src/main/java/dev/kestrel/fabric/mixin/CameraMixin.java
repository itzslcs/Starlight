package dev.kestrel.fabric.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.kestrel.core.Hooks;
import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Freelook: the camera takes its angles from Hooks while active (every getView*Rot read in setup). */
@Mixin(Camera.class)
public abstract class CameraMixin {
    @ModifyExpressionValue(method = "setup", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;getViewYRot(F)F"))
    private float kestrel$yaw(float original) {
        return Hooks.freelook ? Hooks.freelookYaw : original;
    }

    @ModifyExpressionValue(method = "setup", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;getViewXRot(F)F"))
    private float kestrel$pitch(float original) {
        return Hooks.freelook ? Hooks.freelookPitch : original;
    }
}
