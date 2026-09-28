package dev.starlight.fabric.mixin;

import net.minecraft.client.multiplayer.PlayerInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.function.Supplier;

/** Skin changes: the player's list entry draws the new skin (vanilla's own lookup for the refreshed profile). */
@Mixin(PlayerInfo.class)
public interface PlayerInfoAccessor {
    @Accessor("skinLookup")
    Supplier<?> starlight$skinLookup();

    //? if <1.21.9 {
    /*@org.spongepowered.asm.mixin.Mutable
    *///?}
    @Accessor("skinLookup")
    void starlight$setSkinLookup(Supplier<?> lookup);

    @Invoker("createSkinLookup")
    static Supplier<?> starlight$createSkinLookup(com.mojang.authlib.GameProfile profile) {
        throw new AssertionError();
    }
}
