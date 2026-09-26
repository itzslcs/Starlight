package dev.mw19.forge.mixin;

import dev.mw19.core.Hooks;
import net.minecraft.client.particle.EffectRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.util.EnumParticleTypes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Particle multiplier: extra client-side crit / sharpness bursts on hits. */
@Mixin(EffectRenderer.class)
public abstract class EffectRendererMixin {
    private boolean mw19$repeating;

    @Shadow
    public abstract void emitParticleAtEntity(Entity entity, EnumParticleTypes type);

    @Inject(method = "emitParticleAtEntity", at = @At("TAIL"))
    private void mw19$more(Entity entity, EnumParticleTypes type, CallbackInfo ci) {
        int extra = Hooks.extraHitParticles;
        if (extra <= 0 || mw19$repeating || (type != EnumParticleTypes.CRIT && type != EnumParticleTypes.CRIT_MAGIC)) return;
        mw19$repeating = true;
        try {
            for (int i = 0; i < extra; i++) emitParticleAtEntity(entity, type);
        } finally {
            mw19$repeating = false;
        }
    }
}
