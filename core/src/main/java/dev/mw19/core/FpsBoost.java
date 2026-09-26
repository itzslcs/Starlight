package dev.mw19.core;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * FPS Boost (Performance page): one click switches the costliest vanilla options to fast values. Undo restores what
 * the player had, from a snapshot kept in config.json (so it survives restarts). Applying twice keeps the first snapshot.
 */
public final class FpsBoost {
    private final Mw19 k;

    FpsBoost(Mw19 k) {
        this.k = k;
    }

    public boolean active() {
        return !backup().isEmpty();
    }

    /** Returns how many options were set. */
    public int apply() {
        Map<String, String> previous = k.platform.applyFpsBoost();
        Map<String, Object> b = backup();
        for (Map.Entry<String, String> e : previous.entrySet()) {
            if (!b.containsKey(e.getKey())) b.put(e.getKey(), e.getValue());
        }
        k.config.markDirty();
        return previous.size();
    }

    public void undo() {
        Map<String, String> previous = new LinkedHashMap<String, String>();
        for (Map.Entry<String, Object> e : backup().entrySet()) previous.put(e.getKey(), String.valueOf(e.getValue()));
        k.platform.restoreOptions(previous);
        backup().clear();
        k.config.markDirty();
    }

    private Map<String, Object> backup() {
        return k.config.section("fpsBoostBackup");
    }
}
