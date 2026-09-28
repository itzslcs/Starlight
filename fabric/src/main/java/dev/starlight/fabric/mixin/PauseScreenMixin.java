package dev.starlight.fabric.mixin;

import dev.starlight.core.Starlight;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PauseScreen.class)
public abstract class PauseScreenMixin extends Screen {
    protected PauseScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void starlight$button(CallbackInfo ci) {
        if (Starlight.wantMenuButton()) dev.starlight.fabric.StarlightFabric.pauseRow(this, b -> addRenderableWidget(b));
    }

    //? if >=26.2 {
    /*@org.spongepowered.asm.mixin.Shadow @org.spongepowered.asm.mixin.Final private boolean showPauseMenu;

    // 26.2+: the pause menu draws its own background instead of calling Screen's (ScreenBackdropMixin), so it gets its own hook
    @Inject(method = "extractBackground", at = @At("HEAD"), cancellable = true)
    private void starlight$backdrop(net.minecraft.client.gui.GuiGraphicsExtractor g, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (showPauseMenu && dev.starlight.fabric.StarlightFabric.backdrop(this, g)) ci.cancel();
    }
    *///?}
}
