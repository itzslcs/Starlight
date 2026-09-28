package dev.starlight.fabric;

import dev.starlight.core.platform.Host;
import net.minecraft.client.Minecraft;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.server.players.UserWhiteListEntry;
import net.minecraft.util.HttpUtil;
import net.minecraft.world.level.GameType;

import java.util.UUID;

/** Opens the singleplayer world to other players and edits its whitelist (changes run on the server thread). */
final class FabricHost implements Host {
    static final FabricHost INSTANCE = new FabricHost();
    private final Minecraft mc = Minecraft.getInstance();

    private FabricHost() {}

    private IntegratedServer server() {
        return mc.hasSingleplayerServer() ? mc.getSingleplayerServer() : null;
    }

    @Override
    public boolean available() {
        return server() != null;
    }

    @Override
    public int port() {
        IntegratedServer s = server();
        return s != null && s.isPublished() ? s.getPort() : -1;
    }

    @Override
    public int open(int gameMode, boolean commands) {
        IntegratedServer s = server();
        if (s == null) return -1;
        if (s.isPublished()) return s.getPort();
        int port = HttpUtil.getAvailablePort();
        GameType type = GameType.byId(gameMode);
        //? if >=26.3 {
        /*s.setDefaultGameType(type);
        boolean ok = s.publishServer(net.minecraft.server.MinecraftServer.MultiplayerScope.LAN, commands, port);
        *///?} elif >=26.2 {
        /*boolean ok = s.publishServer(net.minecraft.server.MinecraftServer.MultiplayerScope.LAN, type, commands, port);
        *///?} else {
        boolean ok = s.publishServer(type, commands, port);
        //?}
        return ok ? s.getPort() : -1;
    }

    @Override
    public void setWhitelist(boolean on) {
        IntegratedServer s = server();
        if (s == null) return;
        s.execute(() -> {
            //? if >=1.21.9 {
            s.setUsingWhitelist(on);
            //?} else {
            /*s.getPlayerList().setUsingWhiteList(on);
            *///?}
        });
    }

    @Override
    public void allow(UUID id, String name) {
        IntegratedServer s = server();
        if (s == null) return;
        //? if >=1.21.9 {
        net.minecraft.server.players.NameAndId who = new net.minecraft.server.players.NameAndId(id, name);
        //?} else {
        /*com.mojang.authlib.GameProfile who = new com.mojang.authlib.GameProfile(id, name);
        *///?}
        s.execute(() -> s.getPlayerList().getWhiteList().add(new UserWhiteListEntry(who)));
    }

    @Override
    public void disallow(UUID id, String name) {
        IntegratedServer s = server();
        if (s == null) return;
        //? if >=1.21.9 {
        net.minecraft.server.players.NameAndId who = new net.minecraft.server.players.NameAndId(id, name);
        //?} else {
        /*com.mojang.authlib.GameProfile who = new com.mojang.authlib.GameProfile(id, name);
        *///?}
        s.execute(() -> s.getPlayerList().getWhiteList().remove(who));
    }
}
