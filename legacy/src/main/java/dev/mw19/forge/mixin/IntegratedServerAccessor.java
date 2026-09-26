package dev.mw19.forge.mixin;

import net.minecraft.client.multiplayer.ThreadLanServerPing;
import net.minecraft.server.integrated.IntegratedServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Host World: the LAN announcer holds the port the world was opened on. */
@Mixin(IntegratedServer.class)
public interface IntegratedServerAccessor {
    @Accessor("lanServerPing")
    ThreadLanServerPing mw19$lanServerPing();
}
