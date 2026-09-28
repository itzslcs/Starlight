package dev.starlight.fabric.mixin;

import dev.starlight.fabric.StarlightFabric;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * "Starlight game menus": vanilla sliders (options, volume, FOV) match the keycap buttons. Starlight draws the track and handle
 * first; vanilla then draws its two sprites with no width, and its own label and cursor as usual.
 */
@Mixin(AbstractSliderButton.class)
public abstract class SliderStyleMixin extends AbstractWidget {
    @Shadow
    protected double value;
    @Unique
    private boolean starlight$drawn;

    protected SliderStyleMixin(int x, int y, int w, int h, Component message) {
        super(x, y, w, h, message);
    }

    //? if >=26.1 {
    /*@Inject(method = "extractWidgetRenderState", at = @At("HEAD"))
    private void starlight$style(net.minecraft.client.gui.GuiGraphicsExtractor g, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        starlight$drawn = StarlightFabric.slider(g, getX(), getY(), getWidth(), getHeight(), (float) value, isHoveredOrFocused(), active, alpha);
    }
    *///?} else {
    @Inject(method = "renderWidget", at = @At("HEAD"))
    private void starlight$style(net.minecraft.client.gui.GuiGraphics g, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        starlight$drawn = StarlightFabric.slider(g, getX(), getY(), getWidth(), getHeight(), (float) value, isHoveredOrFocused(), active, alpha);
    }
    //?}

    //? if >=26.3 {
    /*@ModifyArg(method = "extractWidgetRenderState", index = 4, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blitSprite(Lcom/mojang/renderpearl/api/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIIII)V"))
    *///?} elif >=26.1 {
    /*@ModifyArg(method = "extractWidgetRenderState", index = 4, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blitSprite(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIIII)V"))
    *///?} elif >=1.21.11 {
    @ModifyArg(method = "renderWidget", index = 4, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphics;blitSprite(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIIII)V"))
    //?} elif >=1.21.6 {
    /*@ModifyArg(method = "renderWidget", index = 4, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphics;blitSprite(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/ResourceLocation;IIIII)V"))
    *///?} elif >=1.21.2 {
    /*@ModifyArg(method = "renderWidget", index = 4, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphics;blitSprite(Ljava/util/function/Function;Lnet/minecraft/resources/ResourceLocation;IIIII)V"))
    *///?} else {
    /*@ModifyArg(method = "renderWidget", index = 3, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphics;blitSprite(Lnet/minecraft/resources/ResourceLocation;IIII)V"))
    *///?}
    private int starlight$hideSprite(int width) {
        return starlight$drawn ? 0 : width;
    }
}
