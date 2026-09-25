package dev.kestrel.fabric.mixin;

import dev.kestrel.core.Kestrel;
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
    private void kestrel$attack(Player player, Entity target, CallbackInfo ci) {
        Kestrel.onAttack(target.getId());
    }
}
