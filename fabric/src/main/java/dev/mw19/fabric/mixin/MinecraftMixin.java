package dev.mw19.fabric.mixin;

import dev.mw19.core.Mw19;
import dev.mw19.fabric.Mw19Fabric;
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
    private void mw19$tickStart(CallbackInfo ci) {
        Mw19Fabric.tick(false);
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void mw19$tickEnd(CallbackInfo ci) {
        Mw19Fabric.tick(true);
    }

    @Inject(method = "close", at = @At("HEAD"))
    private void mw19$close(CallbackInfo ci) {
        Mw19.onShutdown();
    }

    @Inject(method = "fillReport(Lnet/minecraft/CrashReport;)Lnet/minecraft/CrashReport;", at = @At("RETURN"))
    private void mw19$crashReport(CrashReport report, CallbackInfoReturnable<CrashReport> cir) {
        try {
            report.addCategory("MW19").setDetail("State", Mw19.crashReportDetails());
        } catch (Throwable ignored) {
            // never make a crash worse
        }
    }
}
