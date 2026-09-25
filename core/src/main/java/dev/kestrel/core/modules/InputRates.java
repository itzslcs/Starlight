package dev.kestrel.core.modules;

import dev.kestrel.api.game.Game;
import dev.kestrel.core.platform.Platform;

/**
 * Presses per second for each vanilla binding, counted from input events (KeyCPS semantics): a mouse press, a key
 * press and every OS key-repeat of a held key count for whichever binding that key is bound to. Only real input is
 * observed; nothing is ever generated. Ring buffers, no allocation. Game thread only.
 */
public final class InputRates {
    private static final Game.Binding[] BINDINGS = Game.Binding.values();
    private static final int CAP = 64;
    private final long[][] times = new long[BINDINGS.length][CAP];
    private final int[] head = new int[BINDINGS.length];

    /** One input event; {@code code} is canonical (GLFW key, or {@code Keys.mouse(button)}). */
    public void record(Platform platform, int code, long nowMs) {
        for (Game.Binding b : BINDINGS) {
            if (platform.bindingKey(b) != code) continue;
            int i = b.ordinal();
            times[i][head[i]] = nowMs;
            head[i] = (head[i] + 1) % CAP;
        }
    }

    /** Presses in the last second. */
    public int rate(Game.Binding b, long nowMs) {
        int n = 0;
        for (long t : times[b.ordinal()]) if (t != 0 && nowMs - t < 1000) n++;
        return n;
    }
}
