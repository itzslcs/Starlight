package dev.starlight.core.perf;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Graphics presets for the vanilla video options, fastest first. Starlight's focus is frame rate, so even High stays at
 * vanilla's default view distance and leaves the costliest extras (improved transparency, VSync) off. Values use
 * platform-neutral ids; a version without an option skips it. Ranges follow vanilla's own (javap on 1.21.11: weather
 * radius 3-10, cloud range 2-128, menu blur 0-10, mipmaps 0-4, biome blend 0-7, simulation distance 5-32).
 */
public enum VideoPreset {
    POTATO("Potato", "Integrated graphics and old laptops: everything that costs frames is off"),
    LOW("Low", "Entry-level graphics cards: fast graphics, short view distance"),
    MEDIUM("Medium", "Mid-range PCs: fancy graphics at a moderate view distance"),
    HIGH("High", "Gaming PCs: vanilla's look at its default view distance, still no VSync");

    public final String label, blurb;
    private final Map<String, String> values;

    VideoPreset(String label, String blurb) {
        this.label = label;
        this.blurb = blurb;
        int i = ordinal();
        Map<String, String> v = new LinkedHashMap<String, String>();
        v.put("renderDistance", pick(i, "5", "8", "10", "12"));
        v.put("simulationDistance", pick(i, "5", "6", "8", "10"));
        v.put("entityDistance", pick(i, "0.5", "0.75", "1.0", "1.0"));
        v.put("particles", pick(i, "MINIMAL", "DECREASED", "DECREASED", "ALL"));
        v.put("clouds", pick(i, "OFF", "OFF", "FAST", "FANCY"));
        v.put("cloudRange", pick(i, "16", "16", "64", "128"));
        v.put("graphics", pick(i, "FAST", "FAST", "FANCY", "FANCY"));
        v.put("cutoutLeaves", pick(i, "false", "false", "true", "true"));
        v.put("improvedTransparency", "false");
        v.put("vignette", pick(i, "false", "false", "true", "true"));
        v.put("weatherRadius", pick(i, "3", "5", "7", "10"));
        v.put("smoothLighting", pick(i, "false", "false", "true", "true"));
        v.put("biomeBlend", pick(i, "0", "0", "1", "2"));
        v.put("entityShadows", pick(i, "false", "false", "true", "true"));
        v.put("mipmaps", pick(i, "0", "2", "4", "4"));
        v.put("menuBlur", pick(i, "0", "0", "2", "5"));
        v.put("chunkUpdates", "NONE"); // "threaded": the fastest way to rebuild chunks
        v.put("vsync", "false");
        v.put("maxFps", "260"); // unlimited
        this.values = Collections.unmodifiableMap(v);
    }

    private static String pick(int i, String... byPreset) {
        return byPreset[i];
    }

    /** Option id to value, in a fixed order. */
    public Map<String, String> settings() {
        return values;
    }

    public static VideoPreset byName(String name) {
        for (VideoPreset p : values()) if (p.name().equalsIgnoreCase(name)) return p;
        return null;
    }

    /** One step faster, or this one if it is already the fastest. */
    public VideoPreset lower() {
        return this == POTATO ? POTATO : values()[ordinal() - 1];
    }
}
