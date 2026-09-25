package dev.kestrel.fabric.mixin;

import dev.kestrel.core.Kestrel;
import dev.kestrel.fabric.KestrelFabric;
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
    private void kestrel$tickStart(CallbackInfo ci) {
        KestrelFabric.tick(false);
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void kestrel$tickEnd(CallbackInfo ci) {
        KestrelFabric.tick(true);
    }

    @Inject(method = "close", at = @At("HEAD"))
    private void kestrel$close(CallbackInfo ci) {
        Kestrel.onShutdown();
    }

    @Inject(method = "fillReport(Lnet/minecraft/CrashReport;)Lnet/minecraft/CrashReport;", at = @At("RETURN"))
    private void kestrel$crashReport(CrashReport report, CallbackInfoReturnable<CrashReport> cir) {
        try {
            report.addCategory("Kestrel").setDetail("State", Kestrel.crashReportDetails());
        } catch (Throwable ignored) {
            // never make a crash worse
        }
    }
}
