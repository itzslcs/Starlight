package dev.kestrel.forge.mixin;

import dev.kestrel.core.Kestrel;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
    /** Save before LWJGL tears down (the JVM shutdown hook is only a safety net). */
    @Inject(method = "shutdownMinecraftApplet", at = @At("HEAD"))
    private void kestrel$shutdown(CallbackInfo ci) {
        Kestrel.onShutdown();
    }
}
