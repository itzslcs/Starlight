package dev.mw19.forge.mixin;

import dev.mw19.forge.Mw19Forge;
import net.minecraft.client.gui.GuiScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** "MW19 game menus" on 1.8.9: a vanilla screen's background (dirt, or the dimmed world) becomes the MW19 backdrop. */
@Mixin(GuiScreen.class)
public abstract class GuiScreenMixin {
    @Inject(method = "drawWorldBackground", at = @At("HEAD"), cancellable = true)
    private void mw19$backdrop(int tint, CallbackInfo ci) {
        if (Mw19Forge.backdrop((GuiScreen) (Object) this)) ci.cancel();
    }
}
