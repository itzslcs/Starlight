package dev.mw19.core.modules;

import dev.mw19.api.hud.Anchor;
import dev.mw19.api.module.Rule;
import dev.mw19.core.Mw19;

/** Your own latency to the server (the tab list shows the same as bars). */
public final class PingModule extends TextHud {
    public PingModule() {
        super("ping", "Ping", "Your latency to the server", Rule.ALLOWED, false, Anchor.TOP_LEFT, 4, 28);
    }

    private int ping;

    @Override
    protected boolean hasContent() {
        return Mw19.get().platform.inWorld() && !Mw19.get().platform.singleplayer();
    }

    @Override
    protected long key() {
        ping = Mw19.get().platform.ping();
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
