package dev.starlight.core.modules;

import dev.starlight.api.hud.Anchor;
import dev.starlight.api.module.Rule;
import dev.starlight.core.Starlight;

/** Your own latency to the server (the tab list shows the same as bars). */
public final class PingModule extends TextHud {
    public PingModule() {
        super("ping", "Ping", "Your latency to the server", Rule.ALLOWED, false, Anchor.TOP_LEFT, 4, 28);
    }

    private int ping;

    @Override
    protected boolean hasContent() {
        return Starlight.get().platform.inWorld() && !Starlight.get().platform.singleplayer();
    }

    @Override
    protected long key() {
        ping = Starlight.get().platform.ping();
        return ping;
    }

    @Override
    protected String build() {
        return ping < 0 ? "? ms" : ping + " ms";
    }

    @Override
    protected String previewText() {
        return "42 ms";
    }
}
