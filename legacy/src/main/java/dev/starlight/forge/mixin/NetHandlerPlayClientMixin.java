package dev.starlight.forge.mixin;

import dev.starlight.forge.StarlightForge;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.network.play.server.S48PacketResourcePackSend;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Exploit Protection: server resource pack requests are checked first (ForgeExploitGuard). */
@Mixin(NetHandlerPlayClient.class)
public abstract class NetHandlerPlayClientMixin {
    @Inject(method = "handleResourcePack", at = @At("HEAD"), cancellable = true)
    private void starlight$guard(S48PacketResourcePackSend packet, CallbackInfo ci) {
        if (StarlightForge.guardResourcePack((NetHandlerPlayClient) (Object) this, packet)) ci.cancel();
    }
}
