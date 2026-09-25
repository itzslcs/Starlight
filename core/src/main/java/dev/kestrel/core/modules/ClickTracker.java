package dev.kestrel.core.modules;

/** Counts real mouse presses (never generates any) for CPS displays. Ring buffers, no allocation. */
public final class ClickTracker {
    private static final int CAP = 64;
    private final long[][] times = new long[2][CAP];
    private final int[] head = new int[2];

    /** Game thread, from the mouse hook. */
    public void press(int button, long nowMs) {
        if (button < 0 || button > 1) return;
        times[button][head[button]] = nowMs;
        head[button] = (head[button] + 1) % CAP;
    }

    /** Presses in the last second. */
    public int cps(int button, long nowMs) {
        if (button < 0 || button > 1) return 0;
        int n = 0;
        long[] t = times[button];
        for (long v : t) if (v != 0 && nowMs - v < 1000) n++;
        return n;
    }
}
