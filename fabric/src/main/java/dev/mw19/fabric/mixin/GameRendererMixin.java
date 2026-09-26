package dev.mw19.fabric.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.mw19.core.Hooks;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
//? if >=26.1 {
/*import org.objectweb.asm.Opcodes;
*///?} else {
import org.spongepowered.asm.mixin.injection.Slice;
//?}

/**
 * Damage tilt strength (zoom lives in FovMixin). Until 1.21.11 bobHurt reads Options.damageTiltStrength().get();
 * from 26.1 it reads the value extracted into OptionsRenderState.
 */
@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    //? if >=26.1 {
    /*@ModifyExpressionValue(method = "bobHurt", at = @At(value = "FIELD", opcode = Opcodes.GETFIELD,
            target = "Lnet/minecraft/client/renderer/state/OptionsRenderState;damageTiltStrength:D"))
    private double mw19$tilt(double original) {
        return original * Hooks.damageTilt;
    }
    *///?} else {
    @ModifyExpressionValue(method = "bobHurt", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/OptionInstance;get()Ljava/lang/Object;", ordinal = 0),
            slice = @Slice(from = @At(value = "INVOKE", target = "Lnet/minecraft/client/Options;damageTiltStrength()Lnet/minecraft/client/OptionInstance;")))
    private Object mw19$tilt(Object original) {
        float k = Hooks.damageTilt;
        return k == 1f || !(original instanceof Double) ? original : (Object) ((Double) original * k);
    }
    //?}
}
