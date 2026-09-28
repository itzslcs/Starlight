package dev.starlight.fabric.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.starlight.core.Hooks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Shield overlay: lower a raised shield in first person. Visual only. Arguments are taken by type (@Local argsOnly)
 * because the parameter lists differ between versions; first-person hands always belong to the local player.
 * renderArmWithItem became submitArmWithItem in 26.2 and moved to FirstPersonHandsAndItemsRenderer in 26.3.
 */
//? if >=26.3 {
/*@Mixin(net.minecraft.client.renderer.FirstPersonHandsAndItemsRenderer.class)
*///?} else {
@Mixin(net.minecraft.client.renderer.ItemInHandRenderer.class)
//?}
public abstract class ItemInHandRendererMixin {
    //? if >=26.2 {
    /*@Inject(method = "submitArmWithItem", at = @At("HEAD"))
    *///?} else {
    @Inject(method = "renderArmWithItem", at = @At("HEAD"))
    //?}
    private void starlight$shieldDown(CallbackInfo ci, @Local(argsOnly = true) InteractionHand hand,
                                    @Local(argsOnly = true) ItemStack stack, @Local(argsOnly = true) PoseStack pose) {
        pose.pushPose();
        LocalPlayer player = Minecraft.getInstance().player;
        if (Hooks.shieldOffset > 0 && player != null && stack.is(Items.SHIELD) && player.isUsingItem() && player.getUsedItemHand() == hand) {
            pose.translate(0f, -Hooks.shieldOffset, 0f);
        }
    }

    //? if >=26.2 {
    /*@Inject(method = "submitArmWithItem", at = @At("RETURN"))
    *///?} else {
    @Inject(method = "renderArmWithItem", at = @At("RETURN"))
    //?}
    private void starlight$shieldRestore(CallbackInfo ci, @Local(argsOnly = true) PoseStack pose) {
        pose.popPose();
    }
}
