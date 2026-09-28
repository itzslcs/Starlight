package dev.starlight.core;

import java.util.Arrays;

/** Frame-time and own-cost ring buffers for the Performance page and the benchmark harness. */
public final class PerfStats {
    /** Time the open menu takes to draw one frame, microseconds (moving average; the menu is not part of the HUD budget). */
    public volatile double menuUs;
    public static final int N = 480;
    public final float[] frameMs = new float[N];
    public final float[] ownUs = new float[N];
    public int idx, count;
    private long lastFrame;
    /** Nanos spent in our hooks since the last frame boundary. */
    long frameOwn;
    /** EMA of our per-tick cost (µs). */
    public double tickUs;
    private final float[] scratch = new float[N];

    void frame(long now) {
        if (lastFrame != 0) {
            frameMs[idx] = (now - lastFrame) / 1_000_000f;
            ownUs[idx] = frameOwn / 1000f;
            idx = (idx + 1) % N;
            if (count < N) count++;
        }
        lastFrame = now;
        frameOwn = 0;
    }

    public float avgFrameMs() {
        if (count == 0) return 0;
        float s = 0;
        for (int i = 0; i < count; i++) s += frameMs[i];
        return s / count;
    }

    public float avgOwnUs() {
        if (count == 0) return 0;
        float s = 0;
        for (int i = 0; i < count; i++) s += ownUs[i];
        return s / count;
    }

    /** Frame time at the given percentile (e.g. 0.99), ms. */
    public float percentile(float p) {
        if (count == 0) return 0;
        System.arraycopy(frameMs, 0, scratch, 0, count);
        Arrays.sort(scratch, 0, count);
        return scratch[Math.min(count - 1, (int) Math.floor(p * (count - 1)))];
    }

    /** "1% low" FPS: 1000 / average of the slowest 1% frames. */
    public float lowFps(float fraction) {
        if (count == 0) return 0;
        System.arraycopy(frameMs, 0, scratch, 0, count);
        Arrays.sort(scratch, 0, count);
        int n = Math.max(1, Math.round(count * fraction));
        float s = 0;
        for (int i = count - n; i < count; i++) s += scratch[i];
        return s <= 0 ? 0 : 1000f / (s / n);
    }
}
