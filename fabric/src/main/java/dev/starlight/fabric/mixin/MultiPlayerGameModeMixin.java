package dev.starlight.fabric.mixin;

import dev.starlight.core.Starlight;
import dev.starlight.fabric.port.AnchorOptimizer;
import dev.starlight.fabric.port.CrystalOptimizer;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * The player's attacks and block uses: observed for the combo counter; the Crystal Optimizer acts after a hit went to
 * the server, the Anchor Optimizer on a use inside its block prediction (both off by default, see port/).
 */
@Mixin(MultiPlayerGameMode.class)
public abstract class MultiPlayerGameModeMixin {
    @Inject(method = "attack", at = @At("HEAD"))
    private void starlight$attack(Player player, Entity target, CallbackInfo ci) {
        Starlight.onAttack(target.getId());
    }

    @Inject(method = "attack", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;attack(Lnet/minecraft/world/entity/Entity;)V", shift = At.Shift.AFTER))
    private void starlight$crystal(Player player, Entity target, CallbackInfo ci) {
        CrystalOptimizer.afterAttack(target);
    }

    @Inject(method = "performUseItemOn", at = @At("HEAD"), cancellable = true)
    private void starlight$anchor(LocalPlayer player, InteractionHand hand, BlockHitResult hit, CallbackInfoReturnable<InteractionResult> cir) {
        if (AnchorOptimizer.use(player, hand, hit, ((MultiPlayerGameMode) (Object) this).getPlayerMode())) cir.setReturnValue(InteractionResult.SUCCESS);
    }
}
