package dev.starlight.forge.mixin;

import dev.starlight.forge.StarlightForge;
import net.minecraft.client.gui.GuiScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** "Starlight game menus" on 1.8.9: a vanilla screen's background (dirt, or the dimmed world) becomes the Starlight backdrop. */
@Mixin(GuiScreen.class)
public abstract class GuiScreenMixin {
    @Inject(method = "drawWorldBackground", at = @At("HEAD"), cancellable = true)
    private void starlight$backdrop(int tint, CallbackInfo ci) {
        if (StarlightForge.backdrop((GuiScreen) (Object) this)) ci.cancel();
    }
}
