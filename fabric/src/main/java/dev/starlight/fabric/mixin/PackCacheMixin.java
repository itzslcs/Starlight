package dev.starlight.fabric.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.starlight.fabric.ExploitGuard;
import net.minecraft.client.Minecraft;
import net.minecraft.client.main.GameConfig;
import net.minecraft.client.resources.server.DownloadedPackSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.nio.file.Path;

/** Exploit Protection: server resource packs are cached per account (ExploitGuard.packCache). */
@Mixin(Minecraft.class)
public abstract class PackCacheMixin {
    @WrapOperation(method = "<init>", at = @At(value = "NEW", target = "Lnet/minecraft/client/resources/server/DownloadedPackSource;"))
    private DownloadedPackSource starlight$perAccount(Minecraft mc, Path dir, GameConfig.UserData user, Operation<DownloadedPackSource> original) {
        return original.call(mc, ExploitGuard.packCache(dir, user.user.getProfileId()), user);
    }
}
