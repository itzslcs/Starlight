package dev.starlight.forge.mixin;

import dev.starlight.core.Starlight;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
    /** Save before LWJGL tears down (the JVM shutdown hook is only a safety net). */
    @Inject(method = "shutdownMinecraftApplet", at = @At("HEAD"))
    private void starlight$shutdown(CallbackInfo ci) {
        Starlight.onShutdown();
    }
}
