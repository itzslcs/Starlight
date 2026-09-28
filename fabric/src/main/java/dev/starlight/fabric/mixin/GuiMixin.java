package dev.starlight.fabric.mixin;

import dev.starlight.core.Hooks;
import dev.starlight.fabric.StarlightFabric;
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
    private void starlight$hud(GuiGraphicsExtractor graphics, DeltaTracker delta, CallbackInfo ci) {
        StarlightFabric.hud(graphics);
    }

    @Inject(method = "extractCrosshair", at = @At("HEAD"), cancellable = true)
    private void starlight$crosshair(GuiGraphicsExtractor graphics, DeltaTracker delta, CallbackInfo ci) {
        if (Hooks.hideCrosshair) ci.cancel();
    }
    *///?} else {
    @Inject(method = "render", at = @At("TAIL"))
    private void starlight$hud(GuiGraphics graphics, DeltaTracker delta, CallbackInfo ci) {
        StarlightFabric.hud(graphics);
    }

    @Inject(method = "renderCrosshair", at = @At("HEAD"), cancellable = true)
    private void starlight$crosshair(GuiGraphics graphics, DeltaTracker delta, CallbackInfo ci) {
        if (Hooks.hideCrosshair) ci.cancel();
    }
    //?}
}
