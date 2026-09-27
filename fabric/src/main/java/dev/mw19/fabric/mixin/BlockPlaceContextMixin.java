package dev.mw19.fabric.mixin;

import dev.mw19.fabric.port.AnchorOptimizer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Anchor Optimizer (port of Hero's Anchor Optimizer): its ghost block takes a placed block like a fern does. */
@Mixin(BlockPlaceContext.class)
public abstract class BlockPlaceContextMixin {
    @Shadow
    protected boolean replaceClicked;

    @Inject(method = "<init>(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/phys/BlockHitResult;)V",
            at = @At("TAIL"))
    private void mw19$anchorGhost(Level level, Player player, InteractionHand hand, ItemStack stack, BlockHitResult hit, CallbackInfo ci) {
        if (!replaceClicked && AnchorOptimizer.replaceable(level, hit.getBlockPos())) replaceClicked = true;
    }
}
