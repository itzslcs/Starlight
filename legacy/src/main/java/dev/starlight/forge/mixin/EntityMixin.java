package dev.starlight.forge.mixin;

import dev.starlight.core.Hooks;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Freelook: mouse movement turns the camera instead of the player (setAngles subtracts pitch on 1.8.9). */
@Mixin(Entity.class)
public abstract class EntityMixin {
    @Inject(method = "setAngles", at = @At("HEAD"), cancellable = true)
    private void starlight$turn(float yaw, float pitch, CallbackInfo ci) {
        if (Hooks.freelook && (Object) this == Minecraft.getMinecraft().thePlayer) {
            Hooks.freelookTurn(yaw, -pitch);
            ci.cancel();
        }
    }
}
