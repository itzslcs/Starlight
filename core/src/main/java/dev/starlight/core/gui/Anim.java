package dev.starlight.core.gui;

/** Eased value (ease-out cubic) that animates toward a target over a fixed duration. Allocation-free. */
public final class Anim {
    /** Global speed multiplier from the Themes page; 0 disables animations. */
    public static float speed = 1f;

    private float from, to, value;
    private long start;
    private float duration;

    public Anim(float initial) {
        from = to = value = initial;
    }

    /** Animate to {@code target} over {@code ms} (scaled by the global speed). No-op if already heading there. */
    public void to(float target, float ms, long now) {
        if (target == to) return;
        from = get(now);
        to = target;
        start = now;
        duration = speed <= 0 ? 0 : ms / speed;
    }

    public void snap(float v) {
        from = to = value = v;
        duration = 0;
    }

    public float get(long now) {
        if (duration <= 0) return value = to;
        float t = (now - start) / duration;
        if (t >= 1f) return value = to;
        if (t < 0f) t = 0f;
        float e = 1f - (1f - t) * (1f - t) * (1f - t);
        return value = from + (to - from) * e;
    }

    public float target() {
        return to;
    }
}
