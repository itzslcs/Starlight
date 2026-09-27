package dev.mw19.fabric.mixin;

import org.spongepowered.asm.mixin.Mixin;

/**
 * Fast Chests, 1.21.4 - 1.21.11: a chest shown as a block outside the world mesh (chest minecart, block display) is
 * drawn from its block model plus this special renderer. With the pack on the model is the whole chest, so the special
 * renderer would draw it twice. (Before 1.21.4 the MODEL shape skips it; 26.x draws those from the special model only.)
 */
//? if >=1.21.9 && <26.1 {
@Mixin(net.minecraft.client.renderer.SpecialBlockModelRenderer.class)
public abstract class SpecialChestMixin {
    @org.spongepowered.asm.mixin.injection.Inject(method = "renderByBlock", at = @org.spongepowered.asm.mixin.injection.At("HEAD"), cancellable = true)
    private void mw19$baked(net.minecraft.world.level.block.Block block, net.minecraft.world.item.ItemDisplayContext context,
                            com.mojang.blaze3d.vertex.PoseStack pose, net.minecraft.client.renderer.SubmitNodeCollector nodes, int light, int overlay,
                            int outline, org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        if (dev.mw19.fabric.Mw19Fabric.bakedChest(block)) ci.cancel();
    }
}
//?} elif >=1.21.4 && <26.1 {
/*@Mixin(net.minecraft.client.renderer.SpecialBlockModelRenderer.class)
public abstract class SpecialChestMixin {
    @org.spongepowered.asm.mixin.injection.Inject(method = "renderByBlock", at = @org.spongepowered.asm.mixin.injection.At("HEAD"), cancellable = true)
    private void mw19$baked(net.minecraft.world.level.block.Block block, net.minecraft.world.item.ItemDisplayContext context,
                            com.mojang.blaze3d.vertex.PoseStack pose, net.minecraft.client.renderer.MultiBufferSource buffers, int light, int overlay,
                            org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        if (dev.mw19.fabric.Mw19Fabric.bakedChest(block)) ci.cancel();
    }
}
*///?} else {
/*@Mixin(net.minecraft.world.level.block.ChestBlock.class)
public abstract class SpecialChestMixin {}
*///?}
