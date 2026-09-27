package dev.mw19.fabric.mixin;

import dev.mw19.fabric.port.CrystalOptimizer;
import net.minecraft.client.multiplayer.ClientCommonPacketListenerImpl;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Crystal Optimizer: a server's marlowcrystal message (decoded by DiscardedPayloadMixin), in configuration or play. */
@Mixin(ClientCommonPacketListenerImpl.class)
public abstract class CrystalPacketListenerMixin {
    @Shadow
    @Final
    protected Connection connection;

    @Inject(method = "handleCustomPayload(Lnet/minecraft/network/protocol/common/ClientboundCustomPayloadPacket;)V", at = @At("HEAD"), cancellable = true)
    private void mw19$crystalMessage(ClientboundCustomPayloadPacket packet, CallbackInfo ci) {
        if (packet.payload() instanceof CrystalOptimizer.Message m) {
            CrystalOptimizer.received(m, connection);
            ci.cancel();
        }
    }
}
