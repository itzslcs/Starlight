package dev.mw19.forge.mixin;

import dev.mw19.core.Hooks;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Clear Weather on 1.8.9: the client world reports no rain or thunder. */
@Mixin(World.class)
public abstract class WorldMixin {
    @Inject(method = "getRainStrength", at = @At("HEAD"), cancellable = true)
    private void mw19$rain(float partialTicks, CallbackInfoReturnable<Float> cir) {
        if (Hooks.clearWeather && (Object) this instanceof WorldClient) cir.setReturnValue(0f);
    }

    @Inject(method = "getThunderStrength", at = @At("HEAD"), cancellable = true)
    private void mw19$thunder(float partialTicks, CallbackInfoReturnable<Float> cir) {
        if (Hooks.clearWeather && (Object) this instanceof WorldClient) cir.setReturnValue(0f);
    }
}
