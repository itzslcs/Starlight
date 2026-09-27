package dev.mw19.fabric.mixin;

import net.minecraft.client.multiplayer.PlayerInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.function.Supplier;

/** Skin changes: the player's list entry draws the new skin (vanilla's own lookup for the refreshed profile). */
@Mixin(PlayerInfo.class)
public interface PlayerInfoAccessor {
    @Accessor("skinLookup")
    Supplier<?> mw19$skinLookup();

    //? if <1.21.9 {
    /*@org.spongepowered.asm.mixin.Mutable
    *///?}
    @Accessor("skinLookup")
    void mw19$setSkinLookup(Supplier<?> lookup);

    @Invoker("createSkinLookup")
    static Supplier<?> mw19$createSkinLookup(com.mojang.authlib.GameProfile profile) {
        throw new AssertionError();
    }
}
