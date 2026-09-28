package dev.starlight.fabric.mixin;

import dev.starlight.fabric.ExploitGuard;
import net.minecraft.util.HttpUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.net.Proxy;
import java.net.URL;
import java.nio.file.Path;
import java.util.Map;

/** Exploit Protection: server pack downloads (HttpUtil.downloadFile's only caller) fail when the host resolves to a local address. */
@Mixin(HttpUtil.class)
public abstract class PackDownloadMixin {
    @Inject(method = "downloadFile", at = @At("HEAD"))
    private static void starlight$local(Path dir, URL url, Map<String, String> headers, com.google.common.hash.HashFunction hashFunction,
                                   com.google.common.hash.HashCode hash, int maxSize, Proxy proxy, HttpUtil.DownloadProgressListener listener,
                                   CallbackInfoReturnable<Path> cir) {
        ExploitGuard.checkDownload(url);
    }
}
