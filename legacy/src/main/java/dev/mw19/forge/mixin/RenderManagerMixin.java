package dev.mw19.forge.mixin;

import dev.mw19.forge.Mw19Forge;
import net.minecraft.client.renderer.culling.ICamera;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Entity Culling on 1.8.9: after the frustum test passes, entities fully hidden behind blocks are skipped. */
@Mixin(RenderManager.class)
public abstract class RenderManagerMixin {
    @Inject(method = "shouldRender", at = @At("RETURN"), cancellable = true)
    private void mw19$cull(Entity entity, ICamera camera, double camX, double camY, double camZ, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ() && !Mw19Forge.drawEntity(entity, camX, camY, camZ)) cir.setReturnValue(false);
    }
}
