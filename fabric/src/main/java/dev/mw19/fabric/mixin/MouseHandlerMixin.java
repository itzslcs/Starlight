package dev.mw19.fabric.mixin;

import dev.mw19.core.Mw19;
import dev.mw19.fabric.FabricCompat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
//? if >=1.21.9
import net.minecraft.client.input.MouseButtonInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public abstract class MouseHandlerMixin {
    @Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
    private void mw19$scroll(long window, double xOffset, double yOffset, CallbackInfo ci) {
        if (yOffset != 0 && Mw19.onScroll(yOffset)) ci.cancel();
    }

    //? if >=1.21.9 {
    @Inject(method = "onButton", at = @At("HEAD"))
    private void mw19$button(long window, MouseButtonInfo info, int action, CallbackInfo ci) {
        if (window == Minecraft.getInstance().getWindow().handle()) Mw19.onMouseButton(FabricCompat.button(info.button()), action);
    }
    //?} else {
    /*@Inject(method = "onPress", at = @At("HEAD"))
    private void mw19$button(long window, int button, int action, int mods, CallbackInfo ci) {
        if (window == Minecraft.getInstance().getWindow().getWindow()) Mw19.onMouseButton(button, action);
    }
    *///?}
}
