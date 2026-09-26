package dev.mw19.forge;

import com.mojang.authlib.GameProfile;
import dev.mw19.core.platform.Host;
import dev.mw19.forge.mixin.IntegratedServerAccessor;
import dev.mw19.forge.mixin.ThreadLanServerPingAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ThreadLanServerPing;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.world.WorldSettings;

import java.util.UUID;

/** 1.8.9: opens the singleplayer world to LAN and edits its whitelist (on the server thread). */
final class ForgeHost implements Host {
    static final ForgeHost INSTANCE = new ForgeHost();
    private final Minecraft mc = Minecraft.getMinecraft();

    private ForgeHost() {}

    private IntegratedServer server() {
        return mc.isIntegratedServerRunning() ? mc.getIntegratedServer() : null;
    }

    @Override
    public boolean available() {
        return server() != null;
    }

    @Override
    public int port() {
        IntegratedServer s = server();
        if (s == null || !s.getPublic()) return -1;
        try {
            ThreadLanServerPing ping = ((IntegratedServerAccessor) s).mw19$lanServerPing();
            return ping == null ? 0 : Integer.parseInt(((ThreadLanServerPingAccessor) ping).mw19$address());
        } catch (RuntimeException e) {
            return 0; // open, port unknown
        }
    }

    @Override
    public int open(int gameMode, boolean commands) {
        IntegratedServer s = server();
        if (s == null) return -1;
        if (s.getPublic()) return port();
        String port = s.shareToLAN(WorldSettings.GameType.getByID(gameMode), commands);
        try {
            return port == null ? -1 : Integer.parseInt(port);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    @Override
    public void setWhitelist(final boolean on) {
        final IntegratedServer s = server();
        if (s == null) return;
        s.addScheduledTask(new Runnable() {
            @Override
            public void run() {
                s.getConfigurationManager().setWhiteListEnabled(on);
            }
        });
    }

    @Override
    public void allow(UUID id, String name) {
        final IntegratedServer s = server();
        if (s == null) return;
        final GameProfile who = new GameProfile(id, name);
        s.addScheduledTask(new Runnable() {
            @Override
            public void run() {
                s.getConfigurationManager().addWhitelistedPlayer(who);
            }
        });
    }

    @Override
    public void disallow(UUID id, String name) {
        final IntegratedServer s = server();
        if (s == null) return;
        final GameProfile who = new GameProfile(id, name);
        s.addScheduledTask(new Runnable() {
            @Override
            public void run() {
                s.getConfigurationManager().removePlayerFromWhitelist(who);
            }
        });
    }
}
