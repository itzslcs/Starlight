package dev.mw19.fabric.mixin;

import dev.mw19.fabric.ExploitGuard;
import net.minecraft.client.multiplayer.ClientCommonPacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.net.URL;

/** Exploit Protection: a server resource pack URL naming a local host is treated as invalid (vanilla then refuses it). */
@Mixin(ClientCommonPacketListenerImpl.class)
public abstract class PackUrlMixin {
    @Inject(method = "parseResourcePackUrl", at = @At("RETURN"), cancellable = true)
    private static void mw19$local(String url, CallbackInfoReturnable<URL> cir) {
        if (ExploitGuard.blockPackUrl(cir.getReturnValue())) cir.setReturnValue(null);
    }
}
