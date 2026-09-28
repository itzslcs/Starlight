package dev.starlight.core.platform;

import java.util.UUID;

/** Hosting the open singleplayer world for other players (game thread). */
public interface Host {
    /** A singleplayer world is open (its built-in server runs). */
    boolean available();

    /** The port the world is open on, or -1 while it is not. */
    int port();

    /** Opens the world to other players ("Open to LAN"). {@code gameMode} 0 survival .. 3 spectator. Returns the port or -1. */
    int open(int gameMode, boolean commands);

    /** Only whitelisted players (and the host) may join while this is on. */
    void setWhitelist(boolean on);

    void allow(UUID id, String name);

    void disallow(UUID id, String name);
}
