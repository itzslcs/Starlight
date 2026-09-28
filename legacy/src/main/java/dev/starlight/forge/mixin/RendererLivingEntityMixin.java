package dev.starlight.forge.mixin;

import dev.starlight.core.Hooks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.RendererLivingEntity;
import net.minecraft.entity.EntityLivingBase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Hit colour (the four puts of the hurt tint: 1, 0, 0, 0.3) and show-own-nametag. */
@Mixin(RendererLivingEntity.class)
public abstract class RendererLivingEntityMixin {
    @ModifyArg(method = "setBrightness", at = @At(value = "INVOKE", target = "Ljava/nio/FloatBuffer;put(F)Ljava/nio/FloatBuffer;", ordinal = 0))
    private float starlight$r(float v) {
        return Hooks.hitColor == 0 ? v : ((Hooks.hitColor >> 16) & 255) / 255f;
    }

    @ModifyArg(method = "setBrightness", at = @At(value = "INVOKE", target = "Ljava/nio/FloatBuffer;put(F)Ljava/nio/FloatBuffer;", ordinal = 1))
    private float starlight$g(float v) {
        return Hooks.hitColor == 0 ? v : ((Hooks.hitColor >> 8) & 255) / 255f;
    }

    @ModifyArg(method = "setBrightness", at = @At(value = "INVOKE", target = "Ljava/nio/FloatBuffer;put(F)Ljava/nio/FloatBuffer;", ordinal = 2))
    private float starlight$b(float v) {
        return Hooks.hitColor == 0 ? v : (Hooks.hitColor & 255) / 255f;
    }

    @ModifyArg(method = "setBrightness", at = @At(value = "INVOKE", target = "Ljava/nio/FloatBuffer;put(F)Ljava/nio/FloatBuffer;", ordinal = 3))
    private float starlight$a(float v) {
        return Hooks.hitColor == 0 ? v : (Hooks.hitColor >>> 24) / 255f;
    }

    @Inject(method = "canRenderName(Lnet/minecraft/entity/EntityLivingBase;)Z", at = @At("HEAD"), cancellable = true)
    private void starlight$own(EntityLivingBase entity, CallbackInfoReturnable<Boolean> cir) {
        if (!Hooks.ownNametag) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (entity == mc.thePlayer && mc.gameSettings.thirdPersonView != 0 && !mc.gameSettings.hideGUI) cir.setReturnValue(true);
    }
}
