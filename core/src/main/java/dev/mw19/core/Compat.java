package dev.mw19.core;

import dev.mw19.core.platform.ModList;

import java.util.LinkedHashMap;
import java.util.Map;

/** Runtime compat.* flags from the loaded mod list. Modules consult these to step aside. */
public final class Compat {
    private static final String[] KNOWN = {
        "sodium", "iris", "lithium", "entityculling", "immediatelyfast", "moreculling", "badoptimizations",
        "ferritecore", "modernfix", "dynamic_fps", "dawn", "feather", "essential", "optifabric", "modmenu",
        "freelook", "perspectivemod", "zoomify", "okzoomer", "fullbright", "hypixel_mod_api", "patcher",
        "oneconfig", "polyblur", "polycrosshair", "polynametag", "overflowanimations", "tiers", "tiertagger",
        "featheropt", "chatting", "evergreenhud", "hytils-reborn", "keystrokesmod", "keycps"
    };
    private final Map<String, Boolean> flags = new LinkedHashMap<String, Boolean>();

    public Compat(ModList mods) {
        for (String id : KNOWN) {
            boolean on;
            try {
                on = mods.isLoaded(id);
            } catch (Throwable t) {
                on = false;
            }
            flags.put(id, on);
        }
    }

    public boolean has(String id) {
        Boolean b = flags.get(id);
        return b != null && b;
    }

    public Map<String, Boolean> all() {
        return flags;
    }

    public String describePresent() {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Boolean> e : flags.entrySet()) {
            if (!e.getValue()) continue;
            if (sb.length() > 0) sb.append(", ");
            sb.append(e.getKey());
        }
        return sb.length() == 0 ? "none" : sb.toString();
    }
}
