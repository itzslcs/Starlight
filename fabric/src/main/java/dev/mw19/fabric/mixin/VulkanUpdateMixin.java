package dev.mw19.fabric.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The bundled VulkanMod asks api.modrinth.com for a newer version at every start (its UpdateChecker). MW19 only uses
 * the network when the player asks (README, Privacy), and MW19 ships the VulkanMod version it bundles, so that request
 * is skipped. Only present when VulkanMod runs (@Pseudo: no VulkanMod, nothing to apply).
 */
@Pseudo
@Mixin(targets = "net.vulkanmod.config.UpdateChecker", remap = false)
public abstract class VulkanUpdateMixin {
    @Inject(method = "checkForUpdates", at = @At("HEAD"), cancellable = true, remap = false)
    private static void mw19$offline(CallbackInfo ci) {
        ci.cancel();
    }
}
