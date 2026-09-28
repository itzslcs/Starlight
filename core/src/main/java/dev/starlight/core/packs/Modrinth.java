package dev.starlight.core.packs;

import dev.starlight.api.util.Json;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Modrinth's public API (v2) for resource packs: search URLs and the parts of the responses the pack browser needs.
 * No account or token; only called while the player uses the pack browser (README "Network use").
 */
public final class Modrinth {
    public static final String API = "https://api.modrinth.com/v2";

    public enum Sort {
        RELEVANCE("relevance", "Relevance"), DOWNLOADS("downloads", "Downloads"), NEWEST("newest", "Newest"), UPDATED("updated", "Updated");

        final String index;
        public final String label;

        Sort(String index, String label) {
            this.index = index;
            this.label = label;
        }
    }

    /** One search result. */
    public static final class Hit {
        public final String id, slug, title, author, description, iconUrl;
        public final long downloads;

        Hit(Map<String, Object> m) {
            id = Json.str(m, "project_id", "");
            slug = Json.str(m, "slug", id);
            title = Json.str(m, "title", slug);
            author = Json.str(m, "author", "");
            description = Json.str(m, "description", "");
            iconUrl = Json.str(m, "icon_url", "");
            downloads = (long) Json.num(m, "downloads", 0);
        }
    }

    /** The file to download for a project on one game version. */
    public static final class PackFile {
        public final String url, fileName, sha512, versionName;
        public final long size;

        PackFile(String url, String fileName, String sha512, long size, String versionName) {
            this.url = url;
            this.fileName = fileName;
            this.sha512 = sha512;
            this.size = size;
            this.versionName = versionName;
        }
    }

    private Modrinth() {}

    /** Resource packs matching {@code query}; {@code gameVersion} null for any version. */
    public static String searchUrl(String query, String gameVersion, Sort sort, int offset, int limit) {
        StringBuilder facets = new StringBuilder("[[\"project_type:resourcepack\"]");
        if (gameVersion != null) facets.append(",[\"versions:").append(gameVersion).append("\"]");
        facets.append(']');
        return API + "/search?query=" + enc(query.trim()) + "&facets=" + enc(facets.toString()) + "&index=" + sort.index
                + "&offset=" + offset + "&limit=" + limit;
    }

    public static List<Hit> hits(Object json) {
        List<Hit> out = new ArrayList<Hit>();
        for (Object o : Json.arr(Json.obj(json).get("hits"))) {
            Hit h = new Hit(Json.obj(o));
            if (!h.id.isEmpty()) out.add(h);
        }
        return out;
    }

    public static int totalHits(Object json) {
        return (int) Json.num(Json.obj(json), "total_hits", 0);
    }

    /** Versions of {@code projectId} for {@code gameVersion} (newest first); null asks for all versions. */
    public static String versionsUrl(String projectId, String gameVersion) {
        String q = gameVersion == null ? "" : "?game_versions=" + enc("[\"" + gameVersion + "\"]");
        return API + "/project/" + enc(projectId) + "/version" + q;
    }

    /** The primary file of the newest release (else the newest version) in a versions response, or null. */
    public static PackFile newestFile(Object json) {
        List<Object> versions = Json.arr(json);
        Map<String, Object> pick = null;
        for (Object o : versions) {
            Map<String, Object> v = Json.obj(o);
            if (pick == null) pick = v;
            if ("release".equals(Json.str(v, "version_type", ""))) {
                pick = v;
                break;
            }
        }
        if (pick == null) return null;
        Map<String, Object> file = null;
        for (Object o : Json.arr(pick.get("files"))) {
            Map<String, Object> f = Json.obj(o);
            if (file == null || Json.bool(f, "primary", false)) file = f;
        }
        if (file == null) return null;
        String name = safeFileName(Json.str(file, "filename", ""));
        String url = Json.str(file, "url", "");
        if (name == null || !url.startsWith("https://")) return null;
        return new PackFile(url, name, Json.str(Json.obj(file.get("hashes")), "sha512", null), (long) Json.num(file, "size", 0),
                Json.str(pick, "version_number", ""));
    }

    /**
     * A file name that is safe to create in the resource pack folder: a plain name (no path), ending in .zip, made of
     * letters, digits and {@code ._-+()[] }, at most 120 characters. Null when the name cannot be made safe.
     */
    public static String safeFileName(String name) {
        if (name == null) return null;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < name.length() && sb.length() < 120; i++) {
            char c = name.charAt(i);
            boolean ok = (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || "._-+()[] ".indexOf(c) >= 0;
            sb.append(ok ? c : '_');
        }
        String s = sb.toString().trim();
        while (s.startsWith(".")) s = s.substring(1);
        if (!s.toLowerCase(java.util.Locale.ROOT).endsWith(".zip") || s.length() <= 4) return null;
        return s;
    }

    private static String enc(String s) {
        try {
            return URLEncoder.encode(s, "UTF-8").replace("+", "%20");
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
    }
}
