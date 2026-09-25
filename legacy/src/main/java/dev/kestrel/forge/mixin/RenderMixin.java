package dev.kestrel.forge.mixin;

import dev.kestrel.core.Kestrel;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Plugin name decorations on 1.8.9 nametags. The scoreboard line under a name uses the same method, so only the label
 * that contains the player's name is decorated. Re-calls itself with the new text (Mixin 0.7 has no @Local).
 */
@Mixin(Render.class)
public abstract class RenderMixin {
    private static boolean kestrel$inside;

    @Shadow
    protected abstract void renderLivingLabel(Entity entity, String str, double x, double y, double z, int maxDistance);

    @Inject(method = "renderLivingLabel", at = @At("HEAD"), cancellable = true)
    private void kestrel$label(Entity entity, String str, double x, double y, double z, int maxDistance, CallbackInfo ci) {
        if (kestrel$inside || !(entity instanceof EntityPlayer) || str == null || !str.contains(entity.getName())) return;
        String suffix = Kestrel.nameSuffix(entity.getUniqueID(), entity.getName(), false);
        if (suffix == null) return;
        ci.cancel();
        kestrel$inside = true;
        try {
            renderLivingLabel(entity, str + suffix, x, y, z, maxDistance);
        } finally {
            kestrel$inside = false;
        }
    }
}
