package dev.starlight.core.account;

import dev.starlight.api.util.Json;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The accounts Prism Launcher signed in (DECISIONS D-034). Starlight reads a copy of Prism's {@code accounts.json} that
 * the owner puts in the Starlight folder himself; it never writes that file, never refreshes a token and never signs
 * anyone in. The session tokens in it go to the game and nowhere else: they are not logged or copied anywhere.
 */
public final class Accounts {
    public static final class Entry {
        public final String name;
        public final UUID id;
        /** Prism's session token, or "" for an offline account. Never log this. */
        public final String token;
        public final boolean offline;
        /** When the token stops working (epoch ms), or 0 when the file does not say. */
        public final long expires;

        Entry(String name, UUID id, String token, boolean offline, long expires) {
            this.name = name;
            this.id = id;
            this.token = token;
            this.offline = offline;
            this.expires = expires;
        }

        public boolean expired() {
            return !offline && expires > 0 && expires < System.currentTimeMillis();
        }
    }

    private final Path file;
    private final List<Entry> list = new ArrayList<Entry>();
    private String error = "";

    public Accounts(Path file) {
        this.file = file;
    }

    public Path file() {
        return file;
    }

    /** Why the last read found nothing, for the page to show ("" when it worked). */
    public String error() {
        return error;
    }

    public List<Entry> all() {
        return list;
    }

    /** Reads the file again. Never throws: a missing or broken file leaves an empty list and {@link #error()} set. */
    public void reload() {
        list.clear();
        error = "";
        if (!Files.isRegularFile(file)) {
            error = "No accounts.json in " + file.getParent() + " yet";
            return;
        }
        try {
            list.addAll(parse(new String(Files.readAllBytes(file), StandardCharsets.UTF_8)));
            if (list.isEmpty()) error = "accounts.json has no accounts in it";
        } catch (Exception e) {
            error = "accounts.json could not be read (" + e.getMessage() + ")";
        }
    }

    /** Copies a Prism {@code accounts.json} the owner dropped on the window into place, then reads it. */
    public void importFrom(Path source) throws IOException {
        Files.createDirectories(file.getParent());
        Files.copy(source, file, StandardCopyOption.REPLACE_EXISTING);
        reload();
    }

    /** Prism's format (formatVersion 3): {@code accounts[].profile.{id,name}}, {@code .type}, {@code .ygg.{token,exp}}. */
    public static List<Entry> parse(String json) {
        List<Entry> out = new ArrayList<Entry>();
        Object parsed = Json.parse(json);
        if (!(parsed instanceof Map)) throw new IllegalArgumentException("not a JSON object");
        Map<String, Object> root = Json.obj(parsed);
        if (!(root.get("accounts") instanceof List)) throw new IllegalArgumentException("no \"accounts\" list");
        for (Object o : Json.arr(root.get("accounts"))) {
            Map<String, Object> a = Json.obj(o);
            Map<String, Object> profile = Json.obj(a.get("profile"));
            String name = Json.str(profile, "name", "");
            UUID id = uuid(Json.str(profile, "id", ""));
            if (name.isEmpty() || id == null) continue;
            Map<String, Object> ygg = Json.obj(a.get("ygg"));
            String token = Json.str(ygg, "token", "");
            boolean offline = !"MSA".equalsIgnoreCase(Json.str(a, "type", "")) || token.isEmpty() || "0".equals(token);
            out.add(new Entry(name, id, offline ? "" : token, offline, offline ? 0 : expiry(Json.num(ygg, "exp", 0))));
        }
        return out;
    }

    /** Prism writes the expiry in seconds; a value big enough to be milliseconds is taken as milliseconds. */
    private static long expiry(double exp) {
        if (exp <= 0) return 0;
        return (long) (exp > 1e12 ? exp : exp * 1000);
    }

    /** Prism stores ids as 32 hex digits without dashes; a dashed one is read too. Null when it is neither. */
    public static UUID uuid(String s) {
        try {
            if (s.length() == 32) {
                s = s.substring(0, 8) + '-' + s.substring(8, 12) + '-' + s.substring(12, 16) + '-' + s.substring(16, 20) + '-' + s.substring(20);
            }
            return UUID.fromString(s);
        } catch (RuntimeException e) {
            return null;
        }
    }
}
