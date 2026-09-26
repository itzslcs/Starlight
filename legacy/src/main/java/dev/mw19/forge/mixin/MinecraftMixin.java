package dev.mw19.forge.mixin;

import dev.mw19.core.Mw19;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
    /** Save before LWJGL tears down (the JVM shutdown hook is only a safety net). */
    @Inject(method = "shutdownMinecraftApplet", at = @At("HEAD"))
    private void mw19$shutdown(CallbackInfo ci) {
        Mw19.onShutdown();
    }
}
