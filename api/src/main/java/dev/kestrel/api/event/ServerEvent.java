package dev.kestrel.api.event;

/** Joined or left a server/world. {@code address} is null for singleplayer. */
public final class ServerEvent {
    public final boolean joined;
    public final String address;

    public ServerEvent(boolean joined, String address) {
        this.joined = joined;
        this.address = address;
    }
}
