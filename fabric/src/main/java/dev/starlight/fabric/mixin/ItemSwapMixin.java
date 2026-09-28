package dev.starlight.fabric.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.starlight.core.Hooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** 1.8-style "no cooldown dip": the held item ignores the swap/attack-strength scale. The tick moved to FirstPersonHandsAndItems in 26.3. */
//? if >=26.3 {
/*@Mixin(net.minecraft.client.player.FirstPersonHandsAndItems.class)
*///?} else {
@Mixin(net.minecraft.client.renderer.ItemInHandRenderer.class)
//?}
public abstract class ItemSwapMixin {
    //? if >=1.21.11 {
    @ModifyExpressionValue(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;getItemSwapScale(F)F"))
    //?} else {
    /*@ModifyExpressionValue(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;getAttackStrengthScale(F)F"))
    *///?}
    private float starlight$noDip(float original) {
        return Hooks.noCooldownDip ? 1f : original;
    }
}
