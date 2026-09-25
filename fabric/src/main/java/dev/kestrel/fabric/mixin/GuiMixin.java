package dev.kestrel.fabric.mixin;

import dev.kestrel.core.Hooks;
import dev.kestrel.fabric.KestrelFabric;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public abstract class GuiMixin {
    @Inject(method = "render", at = @At("TAIL"))
    private void kestrel$hud(GuiGraphics graphics, DeltaTracker delta, CallbackInfo ci) {
        KestrelFabric.hud(graphics);
    }

    @Inject(method = "renderCrosshair", at = @At("HEAD"), cancellable = true)
    private void kestrel$crosshair(GuiGraphics graphics, DeltaTracker delta, CallbackInfo ci) {
        if (Hooks.hideCrosshair) ci.cancel();
    }
}
