package dev.mw19.fabric.mixin;

import dev.mw19.fabric.port.CrystalOptimizer;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.DiscardedPayload;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Crystal Optimizer: vanilla reads a payload on a channel nobody registered with this codec, which drops the bytes.
 * For the two channels Marlow's Crystal Optimizer receives on, MW19's codec keeps them (only while the module is on).
 */
@Mixin(DiscardedPayload.class)
public abstract class DiscardedPayloadMixin {
    //? if >=1.21.11 {
    @SuppressWarnings({"rawtypes", "unchecked"})
    @Inject(method = "codec", at = @At("HEAD"), cancellable = true)
    private static void mw19$crystalChannels(net.minecraft.resources.Identifier id, int max, CallbackInfoReturnable<StreamCodec> cir) {
        StreamCodec codec = CrystalOptimizer.codec(id, max);
        if (codec != null) cir.setReturnValue(codec);
    }
    //?} else {
    /*@SuppressWarnings({"rawtypes", "unchecked"})
    @Inject(method = "codec", at = @At("HEAD"), cancellable = true)
    private static void mw19$crystalChannels(net.minecraft.resources.ResourceLocation id, int max, CallbackInfoReturnable<StreamCodec> cir) {
        StreamCodec codec = CrystalOptimizer.codec(id, max);
        if (codec != null) cir.setReturnValue(codec);
    }
    *///?}
}
