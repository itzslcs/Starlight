package dev.kestrel.fabric.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.kestrel.core.Hooks;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Shield overlay (lower a raised shield) and 1.8-style "no cooldown dip". Visual only. Arguments are taken by type
 * (@Local argsOnly) because the trailing parameters of renderArmWithItem differ between versions.
 */
@Mixin(ItemInHandRenderer.class)
public abstract class ItemInHandRendererMixin {
    @Inject(method = "renderArmWithItem", at = @At("HEAD"))
    private void kestrel$shieldDown(CallbackInfo ci, @Local(argsOnly = true) AbstractClientPlayer player,
                                    @Local(argsOnly = true) InteractionHand hand, @Local(argsOnly = true) ItemStack stack,
                                    @Local(argsOnly = true) PoseStack pose) {
        pose.pushPose();
        if (Hooks.shieldOffset > 0 && stack.is(Items.SHIELD) && player.isUsingItem() && player.getUsedItemHand() == hand) {
            pose.translate(0f, -Hooks.shieldOffset, 0f);
        }
    }

    @Inject(method = "renderArmWithItem", at = @At("RETURN"))
    private void kestrel$shieldRestore(CallbackInfo ci, @Local(argsOnly = true) PoseStack pose) {
        pose.popPose();
    }

    //? if >=1.21.11 {
    @ModifyExpressionValue(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;getItemSwapScale(F)F"))
    //?} else {
    /*@ModifyExpressionValue(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;getAttackStrengthScale(F)F"))
    *///?}
    private float kestrel$noDip(float original) {
        return Hooks.noCooldownDip ? 1f : original;
    }
}
