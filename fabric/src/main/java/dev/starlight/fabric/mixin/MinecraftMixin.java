package dev.starlight.fabric.mixin;

import dev.starlight.core.Starlight;
import dev.starlight.fabric.StarlightFabric;
import net.minecraft.CrashReport;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
    @Inject(method = "tick", at = @At("HEAD"))
    private void starlight$tickStart(CallbackInfo ci) {
        StarlightFabric.tick(false);
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void starlight$tickEnd(CallbackInfo ci) {
        StarlightFabric.tick(true);
    }

    @Inject(method = "close", at = @At("HEAD"))
    private void starlight$close(CallbackInfo ci) {
        Starlight.onShutdown();
    }

    @Inject(method = "fillReport(Lnet/minecraft/CrashReport;)Lnet/minecraft/CrashReport;", at = @At("RETURN"))
    private void starlight$crashReport(CrashReport report, CallbackInfoReturnable<CrashReport> cir) {
        try {
            report.addCategory("Starlight").setDetail("State", Starlight.crashReportDetails());
        } catch (Throwable ignored) {
            // never make a crash worse
        }
    }
}
