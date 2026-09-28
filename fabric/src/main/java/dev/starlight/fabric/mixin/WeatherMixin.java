package dev.starlight.fabric.mixin;

import dev.starlight.core.Hooks;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Clear Weather: the client's level reports no rain or thunder (a singleplayer server keeps its real weather). */
@Mixin(Level.class)
public abstract class WeatherMixin {
    @Inject(method = "getRainLevel", at = @At("HEAD"), cancellable = true)
    private void starlight$rain(float partialTick, CallbackInfoReturnable<Float> cir) {
        if (Hooks.clearWeather && (Object) this instanceof ClientLevel) cir.setReturnValue(0f);
    }

    @Inject(method = "getThunderLevel", at = @At("HEAD"), cancellable = true)
    private void starlight$thunder(float partialTick, CallbackInfoReturnable<Float> cir) {
        if (Hooks.clearWeather && (Object) this instanceof ClientLevel) cir.setReturnValue(0f);
    }
}
