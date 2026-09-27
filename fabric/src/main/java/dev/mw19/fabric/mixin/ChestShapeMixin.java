package dev.mw19.fabric.mixin;

import net.minecraft.world.level.block.ChestBlock;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Fast Chests before 1.21.4: chests report RenderShape.ENTITYBLOCK_ANIMATED, which the world mesh skips; with the
 * pack on they report MODEL so their (pack) block model is meshed. From 1.21.4 chests are MODEL already (empty model).
 */
//? if <1.21.4 {
/*@Mixin({ChestBlock.class, net.minecraft.world.level.block.EnderChestBlock.class})
public abstract class ChestShapeMixin {
    @org.spongepowered.asm.mixin.injection.Inject(method = "getRenderShape", at = @org.spongepowered.asm.mixin.injection.At("HEAD"), cancellable = true)
    private void mw19$model(net.minecraft.world.level.block.state.BlockState state,
                            org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<net.minecraft.world.level.block.RenderShape> cir) {
        if (dev.mw19.core.Hooks.fastChests) cir.setReturnValue(net.minecraft.world.level.block.RenderShape.MODEL);
    }
}
*///?} else {
@Mixin(ChestBlock.class)
public abstract class ChestShapeMixin {}
//?}
