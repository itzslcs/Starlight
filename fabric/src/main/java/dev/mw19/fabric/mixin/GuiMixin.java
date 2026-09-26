package dev.mw19.fabric.mixin;

import dev.mw19.core.Hooks;
import dev.mw19.fabric.Mw19Fabric;
import net.minecraft.client.DeltaTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//? if >=26.2 {
/*import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
*///?} elif >=26.1 {
/*import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
*///?} else {
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
//?}

/** HUD pass (after vanilla) and crosshair replacement. The HUD moved from Gui to Hud in 26.2; render -> extract in 26.1. */
//? if >=26.2 {
/*@Mixin(Hud.class)
*///?} else {
@Mixin(Gui.class)
//?}
public abstract class GuiMixin {
    //? if >=26.1 {
    /*@Inject(method = "extractRenderState", at = @At("TAIL"))
    private void mw19$hud(GuiGraphicsExtractor graphics, DeltaTracker delta, CallbackInfo ci) {
        Mw19Fabric.hud(graphics);
    }

    @Inject(method = "extractCrosshair", at = @At("HEAD"), cancellable = true)
    private void mw19$crosshair(GuiGraphicsExtractor graphics, DeltaTracker delta, CallbackInfo ci) {
        if (Hooks.hideCrosshair) ci.cancel();
    }
    *///?} else {
    @Inject(method = "render", at = @At("TAIL"))
    private void mw19$hud(GuiGraphics graphics, DeltaTracker delta, CallbackInfo ci) {
        Mw19Fabric.hud(graphics);
    }

    @Inject(method = "renderCrosshair", at = @At("HEAD"), cancellable = true)
    private void mw19$crosshair(GuiGraphics graphics, DeltaTracker delta, CallbackInfo ci) {
        if (Hooks.hideCrosshair) ci.cancel();
    }
    //?}
}
