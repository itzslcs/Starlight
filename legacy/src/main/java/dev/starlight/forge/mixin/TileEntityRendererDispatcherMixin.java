package dev.starlight.forge.mixin;

import dev.starlight.forge.StarlightForge;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.tileentity.TileEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Entity Culling for tile entities on 1.8.9: hidden chests, signs, banners and skulls are not drawn. */
@Mixin(TileEntityRendererDispatcher.class)
public abstract class TileEntityRendererDispatcherMixin {
    @Inject(method = "renderTileEntity", at = @At("HEAD"), cancellable = true)
    private void starlight$cull(TileEntity te, float partialTicks, int destroyStage, CallbackInfo ci) {
        if (!StarlightForge.drawTileEntity(te)) ci.cancel();
    }
}
