package dev.kestrel.forge.mixin;

import dev.kestrel.core.Hooks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Damage tilt (the 14° hurt-cam constant) and freelook camera angles (orientCamera's rotation reads). */
@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin {
    @ModifyConstant(method = "hurtCameraEffect", constant = @Constant(floatValue = 14.0F))
    private float kestrel$tilt(float v) {
        return v * Hooks.damageTilt;
    }

    private static boolean kestrel$free(Entity e) {
        return Hooks.freelook && e == Minecraft.getMinecraft().thePlayer;
    }

    @Redirect(method = "orientCamera", at = @At(value = "FIELD", target = "Lnet/minecraft/entity/Entity;rotationYaw:F", opcode = 180))
    private float kestrel$yaw(Entity e) {
        return kestrel$free(e) ? Hooks.freelookYaw : e.rotationYaw;
    }

    @Redirect(method = "orientCamera", at = @At(value = "FIELD", target = "Lnet/minecraft/entity/Entity;prevRotationYaw:F", opcode = 180))
    private float kestrel$prevYaw(Entity e) {
        return kestrel$free(e) ? Hooks.freelookYaw : e.prevRotationYaw;
    }

    @Redirect(method = "orientCamera", at = @At(value = "FIELD", target = "Lnet/minecraft/entity/Entity;rotationPitch:F", opcode = 180))
    private float kestrel$pitch(Entity e) {
        return kestrel$free(e) ? Hooks.freelookPitch : e.rotationPitch;
    }

    @Redirect(method = "orientCamera", at = @At(value = "FIELD", target = "Lnet/minecraft/entity/Entity;prevRotationPitch:F", opcode = 180))
    private float kestrel$prevPitch(Entity e) {
        return kestrel$free(e) ? Hooks.freelookPitch : e.prevRotationPitch;
    }
}
