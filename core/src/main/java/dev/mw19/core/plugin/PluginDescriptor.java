package dev.mw19.core.plugin;

import dev.mw19.api.util.Json;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Parsed plugin.json. Validation errors throw IllegalArgumentException with a user-readable message. */
public final class PluginDescriptor {
    public final String id, name, version, api, main, description;
    public final List<String> authors;
    /** dependency id -> version requirement ("*", "1.2.0" exact, ">=1.2"). */
    public final Map<String, String> depends;

    private PluginDescriptor(String id, String name, String version, String api, String main, String description,
                             List<String> authors, Map<String, String> depends) {
        this.id = id;
        this.name = name;
        this.version = version;
        this.api = api;
        this.main = main;
        this.description = description;
        this.authors = authors;
        this.depends = depends;
    }

    public static PluginDescriptor parse(String json) {
        Map<String, Object> m;
        try {
            m = Json.obj(Json.parse(json));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("plugin.json is not valid JSON (" + e.getMessage() + ")");
        }
        String id = req(m, "id");
        if (!id.matches("[a-z0-9_-]{1,32}")) throw new IllegalArgumentException("plugin id must match [a-z0-9_-]{1,32}: " + id);
        String version = req(m, "version");
        String api = req(m, "api");
        if (!api.matches("\\d+\\.\\d+")) throw new IllegalArgumentException("\"api\" must look like 1.0");
        String main = req(m, "main");
        if (!main.matches("[A-Za-z_$][\\w$]*(\\.[A-Za-z_$][\\w$]*)+")) throw new IllegalArgumentException("\"main\" must be a fully-qualified class name");
        List<String> authors = new ArrayList<String>();
        for (Object o : Json.arr(m.get("authors"))) if (o instanceof String) authors.add((String) o);
        Map<String, String> deps = new LinkedHashMap<String, String>();
        for (Map.Entry<String, Object> e : Json.obj(m.get("depends")).entrySet()) {
            deps.put(e.getKey(), e.getValue() instanceof String ? (String) e.getValue() : "*");
        }
        return new PluginDescriptor(id, Json.str(m, "name", id), version, api, main, Json.str(m, "description", ""),
                Collections.unmodifiableList(authors), Collections.unmodifiableMap(deps));
    }

    private static String req(Map<String, Object> m, String key) {
        String v = Json.str(m, key, null);
        if (v == null || v.trim().isEmpty()) throw new IllegalArgumentException("plugin.json is missing \"" + key + "\"");
        return v.trim();
    }

    /** Same API major, and minor not newer than the host. */
    public boolean apiCompatible(int hostMajor, int hostMinor) {
        String[] p = api.split("\\.");
        int major = Integer.parseInt(p[0]), minor = Integer.parseInt(p[1]);
        return major == hostMajor && minor <= hostMinor;
    }

    /** "*", "x.y.z" (exact), ">=x.y[.z]". */
    public static boolean satisfies(String version, String requirement) {
        String r = requirement.trim();
        if (r.equals("*") || r.isEmpty()) return true;
        if (r.startsWith(">=")) return compare(version, r.substring(2).trim()) >= 0;
        return compare(version, r) == 0;
    }

    /** Numeric compare of dotted versions; non-numeric suffixes ("-beta") ignored. */
    public static int compare(String a, String b) {
        String[] x = a.split("[.+-]"), y = b.split("[.+-]");
        for (int i = 0; i < Math.max(x.length, y.length); i++) {
            int xi = i < x.length ? num(x[i]) : 0, yi = i < y.length ? num(y[i]) : 0;
            if (xi != yi) return xi < yi ? -1 : 1;
        }
        return 0;
    }

    private static int num(String s) {
        int n = 0;
        for (int i = 0; i < s.length() && Character.isDigit(s.charAt(i)); i++) n = n * 10 + (s.charAt(i) - '0');
        return n;
    }
}
