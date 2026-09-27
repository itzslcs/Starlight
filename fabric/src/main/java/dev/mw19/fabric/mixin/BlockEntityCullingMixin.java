package dev.mw19.fabric.mixin;

import dev.mw19.fabric.Mw19Fabric;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;

/** Entity Culling for block entities: hidden ones are not drawn (1.21.9+: no render state is extracted for them). */
@Mixin(BlockEntityRenderDispatcher.class)
public abstract class BlockEntityCullingMixin {
    //? if >=26.2 {
    /*@Inject(method = "tryExtractRenderState", at = @At("HEAD"), cancellable = true)
    private void mw19$cull(BlockEntity be, float partialTick, net.minecraft.client.renderer.feature.ModelFeatureRenderer.CrumblingOverlay crumbling,
                           boolean flag, org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<Object> cir) {
        if (!Mw19Fabric.drawBlockEntity((BlockEntityRenderDispatcher) (Object) this, be)) cir.setReturnValue(null);
    }
    *///?} elif >=1.21.9 {
    @Inject(method = "tryExtractRenderState", at = @At("HEAD"), cancellable = true)
    private void mw19$cull(BlockEntity be, float partialTick, net.minecraft.client.renderer.feature.ModelFeatureRenderer.CrumblingOverlay crumbling,
                           org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<Object> cir) {
        if (!Mw19Fabric.drawBlockEntity((BlockEntityRenderDispatcher) (Object) this, be)) cir.setReturnValue(null);
    }
    //?} else {
    /*@Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void mw19$cull(BlockEntity be, float partialTick, com.mojang.blaze3d.vertex.PoseStack pose, net.minecraft.client.renderer.MultiBufferSource buffers,
                           org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        if (!Mw19Fabric.drawBlockEntity((BlockEntityRenderDispatcher) (Object) this, be)) ci.cancel();
    }
    *///?}
}
