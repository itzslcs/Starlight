package dev.mw19.forge.mixin;

import dev.mw19.core.Mw19;
import net.minecraft.client.gui.GuiPlayerTabOverlay;
import net.minecraft.client.network.NetworkPlayerInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Tier Tagger in the 1.8.9 tab list. */
@Mixin(GuiPlayerTabOverlay.class)
public abstract class GuiPlayerTabOverlayMixin {
    @Inject(method = "getPlayerName", at = @At("RETURN"), cancellable = true)
    private void mw19$decorate(NetworkPlayerInfo info, CallbackInfoReturnable<String> cir) {
        String suffix = Mw19.nameSuffix(info.getGameProfile().getId(), true);
        if (suffix != null) cir.setReturnValue(cir.getReturnValue() + suffix);
    }
}
