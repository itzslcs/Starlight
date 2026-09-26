package dev.mw19.fabric.mixin;

import dev.mw19.core.Mw19;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Plugin name decorations in the tab list. GameProfile became a record (id()/name()) with authlib 7 (1.21.9). */
@Mixin(PlayerTabOverlay.class)
public abstract class PlayerTabOverlayMixin {
    @Inject(method = "getNameForDisplay", at = @At("RETURN"), cancellable = true)
    private void mw19$decorate(PlayerInfo info, CallbackInfoReturnable<Component> cir) {
        //? if >=1.21.9 {
        String suffix = Mw19.nameSuffix(info.getProfile().id(), info.getProfile().name(), true);
        //?} else {
        /*String suffix = Mw19.nameSuffix(info.getProfile().getId(), info.getProfile().getName(), true);
        *///?}
        if (suffix != null) cir.setReturnValue(cir.getReturnValue().copy().append(Component.literal(suffix)));
    }
}
