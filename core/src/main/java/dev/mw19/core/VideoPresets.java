package dev.mw19.core;

import dev.mw19.core.perf.HardwareTier;
import dev.mw19.core.perf.VideoPreset;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Graphics presets (Performance page). Applying one sets the vanilla video options; Undo restores what the player had,
 * from a snapshot kept in config.json (it survives restarts, and switching presets keeps the first snapshot). On a
 * fresh install MW19 picks a preset from the detected hardware once; it never raises the view distance then.
 */
public final class VideoPresets {
    private final Mw19 k;
    private HardwareTier.Suggestion suggestion;

    VideoPresets(Mw19 k) {
        this.k = k;
    }

    public boolean active() {
        return !backup().isEmpty();
    }

    /** The preset last applied, or null (none, or undone). */
    public VideoPreset current() {
        Object v = state().get("preset");
        return v == null ? null : VideoPreset.byName(String.valueOf(v));
    }

    /** Game thread (reads the graphics device). */
    public HardwareTier.Suggestion suggestion() {
        if (suggestion == null) {
            suggestion = HardwareTier.suggest(k.platform.gpuName(), k.platform.gpuKind(),
                    Runtime.getRuntime().availableProcessors(), Runtime.getRuntime().maxMemory() >> 20);
        }
        return suggestion;
    }

    /**
     * Applies {@code p}. With {@code onlyLower} the view and simulation distances are only ever lowered (automatic
     * choices and the benchmark), so a preset never makes a lighter setup heavier there. Returns the options changed.
     */
    public int apply(VideoPreset p, boolean onlyLower) {
        Map<String, String> want = new LinkedHashMap<String, String>(p.settings());
        if (onlyLower) {
            for (String id : new String[]{"renderDistance", "simulationDistance"}) {
                int now = k.platform.videoOption(id);
                if (now >= 0 && now <= Integer.parseInt(want.get(id))) want.remove(id);
            }
        }
        Map<String, String> previous = k.platform.applyVideo(want);
        Map<String, Object> b = backup();
        for (Map.Entry<String, String> e : previous.entrySet()) {
            if (!b.containsKey(e.getKey())) b.put(e.getKey(), e.getValue());
        }
        state().put("preset", p.name());
        k.config.markDirty();
        return previous.size();
    }

    public void undo() {
        Map<String, String> previous = new LinkedHashMap<String, String>();
        for (Map.Entry<String, Object> e : backup().entrySet()) previous.put(e.getKey(), String.valueOf(e.getValue()));
        k.platform.restoreOptions(previous);
        backup().clear();
        state().remove("preset");
        k.config.markDirty();
    }

    /**
     * First start of the game thread. A fresh install gets the suggested preset (distances only lowered); an install
     * from before presets existed gets a one-time hint instead of changed settings.
     */
    void firstRun(boolean freshInstall) {
        if (state().containsKey("autoDone")) return;
        state().put("autoDone", Boolean.TRUE);
        k.config.markDirty();
        HardwareTier.Suggestion s = suggestion();
        Log.info("graphics: " + k.platform.gpuName() + " (" + k.platform.gpuKind() + ") -> " + s.preset.label + " (" + s.reason + ")");
        if (!freshInstall) {
            k.toast("Graphics presets", "New on the Performance page. For this PC MW19 suggests " + s.preset.label + " (" + s.reason + ").", k.theme.accent);
            return;
        }
        int n = apply(s.preset, true);
        k.toast("Graphics: " + s.preset.label, "Picked for " + s.reason + " (" + n + " settings). Change it or undo it on the Performance page.", k.theme.good);
    }

    private Map<String, Object> backup() {
        return k.config.section("fpsBoostBackup"); // name kept from 0.2.0 so an existing snapshot still undoes
    }

    private Map<String, Object> state() {
        return k.config.section("video");
    }
}
