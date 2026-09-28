package dev.starlight.core.binds;

import dev.starlight.api.util.Json;
import dev.starlight.core.Log;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Bind profiles: named sets of Minecraft's own key bindings, by binding id ("key.hotbar.1") with canonical key codes, so
 * a profile saved on 1.21 applies on 26.x and 1.8.9 as well. They are kept outside the game folder ({@link #location})
 * so that every instance sees them, and the default profile is applied to a new instance on its first start.
 */
public final class BindProfiles {
    private static final String FILE = "bind-profiles.json";
    private final Path file;
    private final Map<String, Map<String, Integer>> profiles = new LinkedHashMap<String, Map<String, Integer>>();
    private String defaultName;

    public BindProfiles(Path file) {
        this.file = file;
        load();
    }

    /**
     * The file every instance shares: {@code $XDG_DATA_HOME/starlight/bind-profiles.json} when that is set, else
     * {@code ~/.starlight/bind-profiles.json}. A Flatpak launcher (Prism, Dawn) gives the game a home folder that is
     * emptied when the game closes, and points XDG_DATA_HOME at its own lasting data folder, so there the profiles are
     * shared by that launcher's instances. The instance's config folder is used when neither can be written, and always
     * for {@code isolated} runs (smoke and benchmark runs must not touch the player's real profiles).
     */
    public static Path location(Path instanceDir, boolean isolated) {
        return isolated ? instanceDir.resolve(FILE) : location(instanceDir, System.getenv("XDG_DATA_HOME"), System.getProperty("user.home"));
    }

    /** {@link #location(Path, boolean)} with the environment passed in (tests). */
    public static Path location(Path instanceDir, String xdgDataHome, String home) {
        try {
            Path dir = xdgDataHome != null && !xdgDataHome.isEmpty() ? Paths.get(xdgDataHome, "starlight")
                    : home != null && !home.isEmpty() ? Paths.get(home, ".starlight") : null;
            if (dir != null) {
                Files.createDirectories(dir);
                if (Files.isWritable(dir)) return dir.resolve(FILE);
            }
        } catch (IOException | RuntimeException ignored) {
            // not writable, or not a valid path: the instance keeps them
        }
        return instanceDir.resolve(FILE);
    }

    public Path file() {
        return file;
    }

    public List<String> names() {
        return new ArrayList<String>(profiles.keySet());
    }

    /** The bindings of {@code name} (id -> canonical code), or null. */
    public Map<String, Integer> get(String name) {
        return profiles.get(name);
    }

    public String defaultName() {
        return defaultName;
    }

    /** Stores {@code binds} as {@code name} (replacing a profile of that name) and saves. */
    public void put(String name, Map<String, Integer> binds) {
        profiles.put(name, new LinkedHashMap<String, Integer>(binds));
        write();
    }

    public void delete(String name) {
        if (profiles.remove(name) == null) return;
        if (name.equals(defaultName)) defaultName = null;
        write();
    }

    /** The profile new instances start with (null: none). */
    public void setDefault(String name) {
        defaultName = name != null && profiles.containsKey(name) ? name : null;
        write();
    }

    /** A free name like "Profile 2". */
    public String freeName() {
        for (int i = profiles.size() + 1; ; i++) if (!profiles.containsKey("Profile " + i)) return "Profile " + i;
    }

    @SuppressWarnings("unchecked")
    private void load() {
        if (!Files.exists(file)) return;
        try {
            Map<String, Object> root = Json.obj(Json.parse(new String(Files.readAllBytes(file), StandardCharsets.UTF_8)));
            Map<String, Object> ps = Json.obj(root.get("profiles"));
            for (Map.Entry<String, Object> e : ps.entrySet()) {
                Map<String, Integer> binds = new LinkedHashMap<String, Integer>();
                for (Map.Entry<String, Object> b : Json.obj(e.getValue()).entrySet()) {
                    if (b.getValue() instanceof Number) binds.put(b.getKey(), ((Number) b.getValue()).intValue());
                }
                profiles.put(e.getKey(), binds);
            }
            String d = Json.str(root, "default", null);
            defaultName = d != null && profiles.containsKey(d) ? d : null;
        } catch (IOException | RuntimeException e) {
            Log.warn("bind profiles: could not read " + file + ": " + e);
        }
    }

    private void write() {
        Map<String, Object> root = new LinkedHashMap<String, Object>();
        root.put("schema", 1);
        root.put("default", defaultName);
        root.put("profiles", profiles);
        try {
            Files.createDirectories(file.getParent());
            Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
            Files.write(tmp, Json.write(root, true).getBytes(StandardCharsets.UTF_8));
            try {
                Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (java.nio.file.AtomicMoveNotSupportedException e) {
                Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException | RuntimeException e) {
            Log.warn("bind profiles: could not save " + file + ": " + e);
        }
    }
}
