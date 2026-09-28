package dev.starlight.fabric.mixin;

import dev.starlight.fabric.StarlightFabric;
import net.minecraft.client.resources.ClientPackSource;
import net.minecraft.server.packs.repository.BuiltInPackSource;
import net.minecraft.server.packs.repository.Pack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Consumer;

/** The client's built-in pack scan also offers Starlight's own packs (Fast Chests). */
@Mixin(BuiltInPackSource.class)
public abstract class PackSourceMixin {
    @Inject(method = "loadPacks", at = @At("TAIL"))
    private void starlight$packs(Consumer<Pack> out, CallbackInfo ci) {
        if ((Object) this instanceof ClientPackSource) StarlightFabric.clientPacks(out);
    }
}
