package dev.starlight.fabric.mixin;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Account switch (D-034): the signed-in session and the two things the game derives from it, all final fields. The chat
 * signing key belongs to the account that started the game, so a switch has to replace it too, or servers refuse the
 * join with "Invalid signature for profile public key".
 */
@Mixin(Minecraft.class)
public interface MinecraftUserAccessor {
    @Mutable
    @Accessor("user")
    void starlight$setUser(net.minecraft.client.User user);

    @Mutable
    @Accessor("profileKeyPairManager")
    void starlight$setProfileKeyPairManager(net.minecraft.client.multiplayer.ProfileKeyPairManager manager);

    @Mutable
    @Accessor("userApiService")
    void starlight$setUserApiService(com.mojang.authlib.minecraft.UserApiService service);
}
