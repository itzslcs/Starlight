package dev.kestrel.fabric.mixin;

import dev.kestrel.core.Hooks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Show own nametag in third person (vanilla hides the camera entity's name). */
@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin {
    //? if >=1.21.2 {
    @Inject(method = "shouldShowName(Lnet/minecraft/world/entity/LivingEntity;D)Z", at = @At("HEAD"), cancellable = true)
    private void kestrel$own(LivingEntity entity, double distanceSq, CallbackInfoReturnable<Boolean> cir) {
    //?} else {
    /*@Inject(method = "shouldShowName(Lnet/minecraft/world/entity/LivingEntity;)Z", at = @At("HEAD"), cancellable = true)
    private void kestrel$own(LivingEntity entity, CallbackInfoReturnable<Boolean> cir) {
    *///?}
        if (!Hooks.ownNametag) return;
        Minecraft mc = Minecraft.getInstance();
        if (entity == mc.player && !mc.options.getCameraType().isFirstPerson() && !mc.options.hideGui) cir.setReturnValue(true);
    }
}
