package dev.kestrel.forge.mixin;

import dev.kestrel.core.Kestrel;
import net.minecraft.client.gui.GuiPlayerTabOverlay;
import net.minecraft.client.network.NetworkPlayerInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Plugin name decorations in the 1.8.9 tab list. */
@Mixin(GuiPlayerTabOverlay.class)
public abstract class GuiPlayerTabOverlayMixin {
    @Inject(method = "getPlayerName", at = @At("RETURN"), cancellable = true)
    private void kestrel$decorate(NetworkPlayerInfo info, CallbackInfoReturnable<String> cir) {
        String suffix = Kestrel.nameSuffix(info.getGameProfile().getId(), info.getGameProfile().getName(), true);
        if (suffix != null) cir.setReturnValue(cir.getReturnValue() + suffix);
    }
}
