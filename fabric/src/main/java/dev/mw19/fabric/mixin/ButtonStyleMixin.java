package dev.mw19.fabric.mixin;

import dev.mw19.fabric.Mw19Fabric;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * "MW19 game menus": vanilla buttons (pause menu, server list, options...) get the home screen's button look. Up to
 * 1.21.10 the button draws background and label in one method, so both are drawn here; from 1.21.11 only the
 * background sprite is replaced and vanilla draws the label. Buttons with their own drawing (icons, checkboxes) keep it.
 */
@Mixin(AbstractButton.class)
//? if >=1.21.11 {
public abstract class ButtonStyleMixin extends AbstractWidget.WithInactiveMessage {
    protected ButtonStyleMixin(int x, int y, int w, int h, Component message) {
        super(x, y, w, h, message);
    }
//?} else {
/*public abstract class ButtonStyleMixin extends AbstractWidget {
    protected ButtonStyleMixin(int x, int y, int w, int h, Component message) {
        super(x, y, w, h, message);
    }
*///?}

    //? if >=26.1 {
    /*@Inject(method = "extractDefaultSprite", at = @At("HEAD"), cancellable = true)
    private void mw19$style(net.minecraft.client.gui.GuiGraphicsExtractor g, CallbackInfo ci) {
        if (Mw19Fabric.button(g, getX(), getY(), getWidth(), getHeight(), isHoveredOrFocused(), active, alpha)) ci.cancel();
    }
    *///?} elif >=1.21.11 {
    @Inject(method = "renderDefaultSprite", at = @At("HEAD"), cancellable = true)
    private void mw19$style(net.minecraft.client.gui.GuiGraphics g, CallbackInfo ci) {
        if (Mw19Fabric.button(g, getX(), getY(), getWidth(), getHeight(), isHoveredOrFocused(), active, alpha)) ci.cancel();
    }
    //?} else {
    /*@org.spongepowered.asm.mixin.Shadow
    public abstract void renderString(net.minecraft.client.gui.GuiGraphics g, net.minecraft.client.gui.Font font, int color);

    @Inject(method = "renderWidget", at = @At("HEAD"), cancellable = true)
    private void mw19$style(net.minecraft.client.gui.GuiGraphics g, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        boolean hot = isHoveredOrFocused();
        if (!Mw19Fabric.button(g, getX(), getY(), getWidth(), getHeight(), hot, active, alpha)) return;
        int rgb = !active ? 0xA0A0A0 : hot ? 0xFFFFA0 : 0xE8E8E8;
        renderString(g, net.minecraft.client.Minecraft.getInstance().font, rgb | Math.round(alpha * 255f) << 24);
        ci.cancel();
    }
    *///?}
}
