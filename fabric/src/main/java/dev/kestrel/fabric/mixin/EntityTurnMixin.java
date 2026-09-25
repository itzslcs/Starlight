package dev.kestrel.fabric.mixin;

import dev.kestrel.core.Hooks;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Freelook: mouse movement turns the camera instead of the player. */
@Mixin(Entity.class)
public abstract class EntityTurnMixin {
    @Inject(method = "turn", at = @At("HEAD"), cancellable = true)
    private void kestrel$turn(double yaw, double pitch, CallbackInfo ci) {
        if (Hooks.freelook && (Object) this == Minecraft.getInstance().player) {
            Hooks.freelookTurn(yaw, pitch);
            ci.cancel();
        }
    }
}
