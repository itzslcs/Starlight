package dev.starlight.forge.mixin;

import net.minecraft.client.multiplayer.ThreadLanServerPing;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ThreadLanServerPing.class)
public interface ThreadLanServerPingAccessor {
    @Accessor("address")
    String starlight$address();
}
