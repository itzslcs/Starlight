package dev.mw19.fabric.mixin;

import dev.mw19.fabric.Mw19Fabric;
import net.minecraft.client.resources.ClientPackSource;
import net.minecraft.server.packs.repository.BuiltInPackSource;
import net.minecraft.server.packs.repository.Pack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Consumer;

/** The client's built-in pack scan also offers MW19's own packs (Fast Chests). */
@Mixin(BuiltInPackSource.class)
public abstract class PackSourceMixin {
    @Inject(method = "loadPacks", at = @At("TAIL"))
    private void mw19$packs(Consumer<Pack> out, CallbackInfo ci) {
        if ((Object) this instanceof ClientPackSource) Mw19Fabric.clientPacks(out);
    }
}
