package dev.mw19.fabric.mixin;

import dev.mw19.core.Mw19;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Observes (never changes) the player's attacks, for the combo counter. */
@Mixin(MultiPlayerGameMode.class)
public abstract class MultiPlayerGameModeMixin {
    @Inject(method = "attack", at = @At("HEAD"))
    private void mw19$attack(Player player, Entity target, CallbackInfo ci) {
        Mw19.onAttack(target.getId());
    }
}
