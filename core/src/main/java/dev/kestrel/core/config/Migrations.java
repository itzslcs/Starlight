package dev.kestrel.core.config;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Ordered, pure schema migrations over the JSON tree. Each step upgrades exactly one version.
 * Never delete a step: old files must always load.
 */
public final class Migrations {
    public static final int GLOBAL = 1;
    public static final int PROFILE = 1;

    private Migrations() {}

    public static int schemaOf(Map<String, Object> root) {
        Object s = root.get("schema");
        return s instanceof Number ? ((Number) s).intValue() : 0;
    }

    /** Throws IllegalStateException for files from a newer client (never silently downgrade). */
    public static Map<String, Object> profile(Map<String, Object> root) {
        int v = schemaOf(root);
        if (v > PROFILE) throw new IllegalStateException("profile schema " + v + " is newer than this client (" + PROFILE + ")");
        if (v == 0) root = profile0to1(root);
        root.put("schema", (double) PROFILE);
        return root;
    }

    public static Map<String, Object> global(Map<String, Object> root) {
        int v = schemaOf(root);
        if (v > GLOBAL) throw new IllegalStateException("config schema " + v + " is newer than this client (" + GLOBAL + ")");
        root.put("schema", (double) GLOBAL);
        return root;
    }

    /**
     * v0 (pre-release format): {"enabled": ["fps", ...], "settings": {"fps.color": ...}}
     * v1: {"modules": {"fps": {"enabled": true, "settings": {"color": ...}}}}
     */
    @SuppressWarnings("unchecked")
    static Map<String, Object> profile0to1(Map<String, Object> old) {
        Map<String, Object> modules = new LinkedHashMap<String, Object>();
        Object en = old.get("enabled");
        if (en instanceof List) {
            for (Object id : (List<Object>) en) {
                if (id instanceof String) module(modules, (String) id).put("enabled", Boolean.TRUE);
            }
        }
        Object st = old.get("settings");
        if (st instanceof Map) {
            for (Map.Entry<String, Object> e : ((Map<String, Object>) st).entrySet()) {
                int dot = e.getKey().indexOf('.');
                if (dot <= 0) continue;
                Map<String, Object> m = module(modules, e.getKey().substring(0, dot));
                Map<String, Object> s = (Map<String, Object>) m.get("settings");
                if (s == null) m.put("settings", s = new LinkedHashMap<String, Object>());
                s.put(e.getKey().substring(dot + 1), e.getValue());
            }
        }
        Map<String, Object> out = new LinkedHashMap<String, Object>();
        out.put("schema", 1.0);
        out.put("name", old.containsKey("name") ? old.get("name") : "Default");
        out.put("modules", modules);
        return out;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> module(Map<String, Object> modules, String id) {
        Map<String, Object> m = (Map<String, Object>) modules.get(id);
        if (m == null) {
            m = new LinkedHashMap<String, Object>();
            m.put("enabled", Boolean.FALSE);
            modules.put(id, m);
        }
        return m;
    }
}
