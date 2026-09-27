package dev.mw19.fabric.mixin;

import dev.mw19.core.Hooks;
import dev.mw19.fabric.port.CrystalOptimizer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.function.Predicate;

/** Crystal Optimizer (port of Marlow's Crystal Optimizer, upstream LevelMixin): targeting skips crystals a hit broke. */
@Mixin(Level.class)
public abstract class CrystalLevelMixin {
    // The parameter is named `predicate` up to 1.21.11 and `selector` from 26.1, so it is matched by type (upstream's note).
    @ModifyVariable(method = "getEntities(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;)Ljava/util/List;",
            at = @At("HEAD"), argsOnly = true)
    private Predicate<? super Entity> mw19$hideKeptCrystals(Predicate<? super Entity> predicate) {
        return Hooks.crystalOptimizer && ((Level) (Object) this).isClientSide() ? CrystalOptimizer.hide(predicate) : predicate;
    }
}
