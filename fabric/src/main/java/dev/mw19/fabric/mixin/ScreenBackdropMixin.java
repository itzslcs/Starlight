package dev.mw19.fabric.mixin;

import dev.mw19.fabric.Mw19Fabric;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** "MW19 game menus": a vanilla screen's background (panorama, blur, dark overlay) becomes the MW19 backdrop. */
@Mixin(Screen.class)
public abstract class ScreenBackdropMixin {
    //? if >=26.1 {
    /*@Inject(method = "extractBackground", at = @At("HEAD"), cancellable = true)
    private void mw19$backdrop(net.minecraft.client.gui.GuiGraphicsExtractor g, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (Mw19Fabric.backdrop((Screen) (Object) this, g)) ci.cancel();
    }
    *///?} else {
    @Inject(method = "renderBackground", at = @At("HEAD"), cancellable = true)
    private void mw19$backdrop(net.minecraft.client.gui.GuiGraphics g, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (Mw19Fabric.backdrop((Screen) (Object) this, g)) ci.cancel();
    }
    //?}
}
