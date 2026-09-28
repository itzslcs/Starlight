package dev.starlight.fabric.mixin;

import dev.starlight.core.Hooks;
import dev.starlight.fabric.port.CrystalOptimizer;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.prediction.BlockStatePredictionHandler;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Crystal Optimizer (port of Marlow's Crystal Optimizer, upstream ClientLevelMixin). */
@Mixin(ClientLevel.class)
public abstract class CrystalClientLevelMixin implements CrystalOptimizer.Sequenced {
    @Shadow
    @Final
    private BlockStatePredictionHandler blockStatePredictionHandler;

    @Override
    public int starlight$blockSequence() {
        return blockStatePredictionHandler.currentSequence();
    }

    // Hidden instead of removed, so a crystal the server did not break comes back once it is released.
    @Inject(method = "entitiesForRendering", at = @At("RETURN"), cancellable = true)
    private void starlight$hideKeptCrystals(CallbackInfoReturnable<Iterable<Entity>> cir) {
        if (Hooks.crystalOptimizer && !Hooks.crystalKeepRender) cir.setReturnValue(CrystalOptimizer.hide(cir.getReturnValue()));
    }

    @Inject(method = "handleBlockChangedAck", at = @At("HEAD"))
    private void starlight$releaseKeptCrystals(int sequence, CallbackInfo ci) {
        CrystalOptimizer.acknowledged(sequence);
    }
}
