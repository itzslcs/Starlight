package dev.starlight.core.modules;

import dev.starlight.api.hud.Anchor;
import dev.starlight.api.module.Rule;
import dev.starlight.core.Starlight;

/** The server you are on (as typed in the server list), or "Singleplayer". */
public final class ServerAddressModule extends TextHud {
    private String address;

    public ServerAddressModule() {
        super("server_address", "Server Address", "Shows which server you are playing on", Rule.ALLOWED, false, Anchor.BOTTOM_RIGHT, 4, 16);
    }

    @Override
    protected boolean hasContent() {
        return Starlight.get().platform.inWorld();
    }

    @Override
    protected long key() {
        address = Starlight.get().platform.serverAddress();
        return address == null ? 0 : address.hashCode() | 1L << 40;
    }

    @Override
    protected String build() {
        return address == null ? "Singleplayer" : address;
    }

    @Override
    protected String previewText() {
        return "play.example.net";
    }
}
