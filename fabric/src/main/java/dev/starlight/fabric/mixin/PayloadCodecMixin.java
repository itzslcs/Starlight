package dev.starlight.fabric.mixin;

import dev.starlight.fabric.port.CrystalOptimizer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Crystal Optimizer: vanilla's custom payload codec writes Starlight's outgoing messages as id + bytes before it looks up a
 * codec for the id. With Fabric API installed, minecraft:register has Fabric's own codec, which only takes Fabric's payload.
 */
@Mixin(targets = "net.minecraft.network.protocol.common.custom.CustomPacketPayload$1")
public abstract class PayloadCodecMixin {
    @Inject(method = "encode(Lnet/minecraft/network/FriendlyByteBuf;Lnet/minecraft/network/protocol/common/custom/CustomPacketPayload;)V",
            at = @At("HEAD"), cancellable = true)
    private void starlight$crystalMessage(FriendlyByteBuf buf, CustomPacketPayload payload, CallbackInfo ci) {
        if (CrystalOptimizer.write(buf, payload)) ci.cancel();
    }
}
