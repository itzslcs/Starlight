package dev.kestrel.fabric.mixin;

import dev.kestrel.core.Hooks;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Particle multiplier: extra client-side crit / enchanted-hit bursts on hits. */
@Mixin(ParticleEngine.class)
public abstract class ParticleEngineMixin {
    @Unique
    private boolean kestrel$repeating;

    @Shadow
    public abstract void createTrackingEmitter(Entity entity, ParticleOptions options);

    @Inject(method = "createTrackingEmitter(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/core/particles/ParticleOptions;)V", at = @At("TAIL"))
    private void kestrel$more(Entity entity, ParticleOptions options, CallbackInfo ci) {
        int extra = Hooks.extraHitParticles;
        if (extra <= 0 || kestrel$repeating || (options != ParticleTypes.CRIT && options != ParticleTypes.ENCHANTED_HIT)) return;
        kestrel$repeating = true;
        try {
            for (int i = 0; i < extra; i++) createTrackingEmitter(entity, options);
        } finally {
            kestrel$repeating = false;
        }
    }
}
