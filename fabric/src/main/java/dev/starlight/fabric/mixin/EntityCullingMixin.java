package dev.starlight.fabric.mixin;

import dev.starlight.fabric.StarlightFabric;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Entity Culling: after vanilla's frustum test passes, entities fully hidden behind blocks are skipped. */
@Mixin(EntityRenderDispatcher.class)
public abstract class EntityCullingMixin {
    //? if >=26.3 {
    /*@Inject(method = "shouldRender", at = @At("RETURN"), cancellable = true)
    private <E extends Entity> void starlight$cull(E entity, Frustum frustum, double camX, double camY, double camZ, float partialTick,
                                             CallbackInfoReturnable<Boolean> cir) {
    *///?} else {
    @Inject(method = "shouldRender", at = @At("RETURN"), cancellable = true)
    private <E extends Entity> void starlight$cull(E entity, Frustum frustum, double camX, double camY, double camZ,
                                             CallbackInfoReturnable<Boolean> cir) {
    //?}
        if (cir.getReturnValueZ() && !StarlightFabric.drawEntity(entity, camX, camY, camZ)) cir.setReturnValue(false);
    }
}
