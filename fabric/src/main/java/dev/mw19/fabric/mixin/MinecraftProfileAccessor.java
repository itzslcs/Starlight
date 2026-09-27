package dev.mw19.fabric.mixin;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Skin changes: the profile the game fetched once at start (FabricMedia.refreshOwnSkin puts the new one in). */
@Mixin(Minecraft.class)
public interface MinecraftProfileAccessor {
    @Accessor("profileFuture")
    java.util.concurrent.CompletableFuture<?> mw19$profileFuture();
}
