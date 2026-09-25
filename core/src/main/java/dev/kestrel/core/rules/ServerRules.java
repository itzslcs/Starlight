package dev.kestrel.core.rules;

import dev.kestrel.api.util.Json;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Data-driven server → disallowed-module rules (serverrules.json). The bundled default is merged with the user's
 * file: entries with the same name replace the bundled one; "replaceDefaults": true drops all bundled entries.
 */
public final class ServerRules {
    public static final class Rule {
        public final String name;
        public final List<String> match;
        public final Set<String> disallow;
        public final String note;

        Rule(String name, List<String> match, Set<String> disallow, String note) {
            this.name = name;
            this.match = Collections.unmodifiableList(match);
            this.disallow = Collections.unmodifiableSet(disallow);
            this.note = note;
        }
    }

    private List<Rule> rules = Collections.emptyList();

    public List<Rule> rules() {
        return rules;
    }

    /** @param user may be null; parse errors in either document throw IllegalArgumentException */
    public void load(String bundled, String user) {
        Map<String, Rule> byName = new LinkedHashMap<String, Rule>();
        Map<String, Object> u = user == null ? null : Json.obj(Json.parse(user));
        if (u == null || !Json.bool(u, "replaceDefaults", false)) addAll(byName, Json.obj(Json.parse(bundled)));
        if (u != null) addAll(byName, u);
        rules = Collections.unmodifiableList(new ArrayList<Rule>(byName.values()));
    }

    private static void addAll(Map<String, Rule> into, Map<String, Object> doc) {
        int schema = (int) Json.num(doc, "schema", 1);
        if (schema != 1) throw new IllegalArgumentException("unsupported serverrules schema " + schema);
        for (Object o : Json.arr(doc.get("servers"))) {
            Map<String, Object> m = Json.obj(o);
            String name = Json.str(m, "name", null);
            if (name == null) continue;
            List<String> match = new ArrayList<String>();
            for (Object p : Json.arr(m.get("match"))) if (p instanceof String) match.add(((String) p).toLowerCase(Locale.ROOT));
            Set<String> dis = new LinkedHashSet<String>();
            for (Object p : Json.arr(m.get("disallow"))) if (p instanceof String) dis.add((String) p);
            into.put(name, new Rule(name, match, dis, Json.str(m, "note", "")));
        }
    }

    /** Rules matching the address (empty for singleplayer/null). */
    public List<Rule> matching(String address) {
        List<Rule> out = new ArrayList<Rule>();
        if (address == null) return out;
        for (Rule r : rules) {
            for (String p : r.match) {
                if (matches(p, address)) {
                    out.add(r);
                    break;
                }
            }
        }
        return out;
    }

    public Set<String> disallowedFor(String address) {
        Set<String> out = new LinkedHashSet<String>();
        for (Rule r : matching(address)) out.addAll(r.disallow);
        return out;
    }

    /** "host" matches exactly (any port); "*.host" matches any subdomain of host (not host itself). */
    public static boolean matches(String pattern, String address) {
        String host = normalize(address), p = pattern.toLowerCase(Locale.ROOT).trim();
        if (host.isEmpty() || p.isEmpty()) return false;
        if (p.startsWith("*.")) {
            String suffix = p.substring(1); // ".host"
            return host.endsWith(suffix) && host.length() > suffix.length();
        }
        return host.equals(p);
    }

    /** Lowercase, no port, no trailing dot, no Forge/FML handshake markers. */
    public static String normalize(String address) {
        if (address == null) return "";
        String a = address.trim().toLowerCase(Locale.ROOT);
        int nul = a.indexOf('\0');
        if (nul >= 0) a = a.substring(0, nul);
        if (a.startsWith("[")) { // IPv6 literal
            int end = a.indexOf(']');
            return end > 0 ? a.substring(1, end) : a;
        }
        int colon = a.lastIndexOf(':');
        if (colon > 0 && a.indexOf(':') == colon) a = a.substring(0, colon);
        while (a.endsWith(".")) a = a.substring(0, a.length() - 1);
        return a;
    }
}
