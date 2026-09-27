package dev.mw19.core.config;

import dev.mw19.api.Scheduler;
import dev.mw19.api.module.HudModule;
import dev.mw19.api.setting.Setting;
import dev.mw19.api.util.Json;
import dev.mw19.core.Log;
import dev.mw19.core.hud.HudElement;
import dev.mw19.core.hud.HudManager;
import dev.mw19.core.module.ModuleManager;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * config.json (global) + profiles/<name>.json. Game-thread API; disk writes happen on "MW19-IO".
 * Unknown keys and entries of modules that are not registered right now (e.g. from a newer MW19) are preserved.
 */
public final class ConfigManager {
    public static final String DEFAULT_PROFILE = "Default";
    private static final long DEBOUNCE_MS = 1500;
    private static final long BACKUP_INTERVAL_MS = 10 * 60 * 1000L;
    private static final int KEEP_BACKUPS = 10;

    public final Path root, profilesDir, backupsDir;
    private final ModuleManager modules;
    private final HudManager hud;
    private final ClientSettings client;
    private final Scheduler scheduler;
    private final ScheduledExecutorService io;
    private final Map<Path, Long> lastBackup = new HashMap<Path, Long>();

    private Map<String, Object> global = new LinkedHashMap<String, Object>();
    private Map<String, Object> profileRaw = newProfile(DEFAULT_PROFILE);
    private String active = DEFAULT_PROFILE;
    private ScheduledFuture<?> pending;
    private boolean suppressDirty;
    /** Messages for the user (e.g. "restored from backup"), drained by the GUI as toasts. */
    public final List<String> notices = new ArrayList<String>();

    public ConfigManager(Path root, ModuleManager modules, HudManager hud, ClientSettings client, Scheduler scheduler) {
        this.root = root;
        this.profilesDir = root.resolve("profiles");
        this.backupsDir = root.resolve("backups");
        this.modules = modules;
        this.hud = hud;
        this.client = client;
        this.scheduler = scheduler;
        this.io = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r, "MW19-IO");
                t.setDaemon(true);
                return t;
            }
        });
    }

    // ------------------------------------------------------------------ load

    /** Loads config.json and the active profile. Never throws; falls back to backups, then defaults. */
    /** No profile had been saved before this start: MW19's first run in this game directory. */
    public boolean freshInstall;

    public void load() {
        suppressDirty = true;
        try {
            freshInstall = !Files.exists(profileFile(DEFAULT_PROFILE));
            Files.createDirectories(profilesDir);
            Map<String, Object> g = readWithRecovery(root.resolve("config.json"), false);
            global = g != null ? g : newGlobal();
            client.fromJson(Json.obj(global.get("client")));
            String want = Json.str(global, "activeProfile", DEFAULT_PROFILE);
            loadProfile(profileExists(want) ? want : DEFAULT_PROFILE);
            backupNow(root.resolve("config.json"));
            backupNow(profileFile(active));
        } catch (IOException e) {
            Log.error("config load failed; using defaults", e);
        } finally {
            suppressDirty = false;
        }
    }

    private Map<String, Object> readWithRecovery(Path file, boolean profile) {
        if (!Files.exists(file)) return null;
        try {
            return parse(file, profile);
        } catch (Exception e) {
            Log.error("unreadable " + file.getFileName() + ", trying backups", e);
            Path moved = AtomicFiles.quarantine(file);
            try {
                List<Path> backups = AtomicFiles.backupsOf(file, backupsDir);
                for (int i = backups.size() - 1; i >= 0; i--) {
                    try {
                        Map<String, Object> m = parse(backups.get(i), profile);
                        notices.add(file.getFileName() + " was damaged; restored " + backups.get(i).getFileName()
                                + (moved != null ? " (damaged copy kept as " + moved.getFileName() + ")" : ""));
                        return m;
                    } catch (Exception ignored) {
                        // try the next older one
                    }
                }
            } catch (IOException ignored) {
                // fall through
            }
            notices.add(file.getFileName() + " was damaged and no backup was usable; defaults loaded");
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> parse(Path file, boolean profile) throws IOException {
        Object o = Json.parse(AtomicFiles.read(file));
        if (!(o instanceof Map)) throw new IOException("not a JSON object");
        Map<String, Object> m = (Map<String, Object>) o;
        return profile ? Migrations.profile(m) : Migrations.global(m);
    }

    private void loadProfile(String name) {
        Map<String, Object> p = readWithRecovery(profileFile(name), true);
        profileRaw = p != null ? p : newProfile(name);
        active = name;
        for (ModuleManager.State s : modules.all()) applyTo(s);
    }

    /** Applies the active profile's stored entry (or defaults) to one module. Called for late registrations too. */
    public void applyTo(ModuleManager.State s) {
        boolean prev = suppressDirty;
        suppressDirty = true;
        try {
            Map<String, Object> entry = Json.obj(Json.obj(profileRaw.get("modules")).get(s.module.id()));
            Map<String, Object> settings = Json.obj(entry.get("settings"));
            for (Setting<?> set : s.module.settings()) {
                if (settings.containsKey(set.id())) set.fromJson(settings.get(set.id()));
                else set.reset();
            }
            if (s.module instanceof HudModule) {
                HudModule hm = (HudModule) s.module;
                hud.resetElement(hm);
                Map<String, Object> h = Json.obj(entry.get("hud"));
                if (!h.isEmpty()) hud.element(hm).fromJson(h);
            }
            boolean enabled = entry.containsKey("enabled") ? Json.bool(entry, "enabled", false) : s.module.defaultEnabled();
            modules.loadEnabled(s, enabled, Json.bool(entry, "favorite", false));
        } finally {
            suppressDirty = prev;
        }
    }

    // ------------------------------------------------------------------ save

    private final Object writeLock = new Object();
    private final java.util.concurrent.atomic.AtomicLong saves = new java.util.concurrent.atomic.AtomicLong();
    private long written; // guarded by writeLock

    public void markDirty() {
        if (suppressDirty) return;
        synchronized (this) {
            if (pending != null) pending.cancel(false);
            pending = io.schedule(new Runnable() {
                @Override
                public void run() {
                    scheduler.runOnMain(new Runnable() {
                        @Override
                        public void run() {
                            saveAsync();
                        }
                    });
                }
            }, DEBOUNCE_MS, TimeUnit.MILLISECONDS);
        }
    }

    private void saveAsync() {
        final String g = Json.write(snapshotGlobal(), true);
        final String p = Json.write(snapshotProfile(), true);
        final Path gf = root.resolve("config.json"), pf = profileFile(active);
        final long snapshot = saves.incrementAndGet();
        io.execute(new Runnable() {
            @Override
            public void run() {
                writeBoth(snapshot, gf, g, pf, p);
            }
        });
    }

    /** Synchronous save on the calling thread; used at shutdown and before profile switches. */
    public void flush() {
        synchronized (this) {
            if (pending != null) pending.cancel(false);
            pending = null;
        }
        String g = Json.write(snapshotGlobal(), true), p = Json.write(snapshotProfile(), true);
        writeBoth(saves.incrementAndGet(), root.resolve("config.json"), g, profileFile(active), p);
    }

    /**
     * One writer at a time, newest snapshot wins. A queued background save must not overwrite a newer flush, and two
     * writers must not share the .tmp file (debug-log 2026-09-26: at exit, flush and a background save raced on
     * config.json.tmp and one failed with NoSuchFileException).
     */
    private void writeBoth(long snapshot, Path gf, String g, Path pf, String p) {
        synchronized (writeLock) {
            if (snapshot < written) return;
            written = snapshot;
            write(gf, g);
            write(pf, p);
        }
    }

    private void write(Path file, String content) {
        try {
            Long last = lastBackup.get(file);
            if (last == null || System.currentTimeMillis() - last > BACKUP_INTERVAL_MS) backupNow(file);
            AtomicFiles.write(file, content);
        } catch (IOException e) {
            Log.error("could not save " + file.getFileName(), e);
        }
    }

    private void backupNow(Path file) {
        try {
            if (AtomicFiles.backup(file, backupsDir, KEEP_BACKUPS) != null) lastBackup.put(file, System.currentTimeMillis());
        } catch (IOException e) {
            Log.warn("backup of " + file.getFileName() + " failed: " + e);
        }
    }

    public Map<String, Object> snapshotGlobal() {
        Map<String, Object> g = new LinkedHashMap<String, Object>(global);
        g.put("schema", (double) Migrations.GLOBAL);
        g.put("activeProfile", active);
        g.put("client", client.toJson());
        return g;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> snapshotProfile() {
        Map<String, Object> p = new LinkedHashMap<String, Object>(profileRaw);
        Map<String, Object> mods = new LinkedHashMap<String, Object>(Json.obj(profileRaw.get("modules")));
        for (ModuleManager.State s : modules.all()) mods.put(s.module.id(), moduleEntry(s));
        p.put("schema", (double) Migrations.PROFILE);
        p.put("name", active);
        p.put("modules", mods);
        profileRaw = p;
        return p;
    }

    private Map<String, Object> moduleEntry(ModuleManager.State s) {
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        m.put("enabled", s.enabled());
        if (s.favorite()) m.put("favorite", true);
        Map<String, Object> set = new LinkedHashMap<String, Object>();
        for (Setting<?> st : s.module.settings()) set.put(st.id(), st.toJson());
        if (!set.isEmpty()) m.put("settings", set);
        if (s.module instanceof HudModule) {
            HudElement e = hud.peek(s.module.id());
            if (e != null) m.put("hud", e.toJson());
        }
        return m;
    }

    // ------------------------------------------------------------------ global extras (sections, auto-profiles)

    @SuppressWarnings("unchecked")
    public Map<String, Object> section(String key) {
        Object o = global.get(key);
        if (!(o instanceof Map)) {
            o = new LinkedHashMap<String, Object>();
            global.put(key, o);
        }
        return (Map<String, Object>) o;
    }

    /** Server pattern → profile name, in priority order. */
    public List<String[]> autoProfiles() {
        List<String[]> out = new ArrayList<String[]>();
        for (Object o : Json.arr(global.get("autoProfiles"))) {
            Map<String, Object> m = Json.obj(o);
            String match = Json.str(m, "match", null), prof = Json.str(m, "profile", null);
            if (match != null && prof != null) out.add(new String[]{match, prof});
        }
        return out;
    }

    public void setAutoProfiles(List<String[]> rules) {
        List<Object> arr = new ArrayList<Object>();
        for (String[] r : rules) {
            Map<String, Object> m = new LinkedHashMap<String, Object>();
            m.put("match", r[0]);
            m.put("profile", r[1]);
            arr.add(m);
        }
        global.put("autoProfiles", arr);
        markDirty();
    }

    /** First auto-profile rule matching the server, or null. */
    public String profileFor(String address) {
        if (address == null) return null;
        for (String[] r : autoProfiles()) {
            if (dev.mw19.core.rules.ServerRules.matches(r[0], address) && profileExists(r[1])) return r[1];
        }
        return null;
    }

    // ------------------------------------------------------------------ profiles

    public String active() {
        return active;
    }

    public List<String> profiles() {
        List<String> out = new ArrayList<String>();
        try {
            if (Files.isDirectory(profilesDir)) {
                DirectoryStream<Path> ds = Files.newDirectoryStream(profilesDir, "*.json");
                try {
                    for (Path p : ds) out.add(AtomicFiles.stem(p));
                } finally {
                    ds.close();
                }
            }
        } catch (IOException e) {
            Log.warn("listing profiles failed: " + e);
        }
        if (!out.contains(active)) out.add(active);
        Collections.sort(out, String.CASE_INSENSITIVE_ORDER);
        return out;
    }

    public boolean profileExists(String name) {
        return name != null && (name.equals(active) || Files.exists(profileFile(name)));
    }

    /** Instant switch: saves the current profile, then applies the other one in memory. */
    public void switchTo(String name) {
        if (!validName(name) || name.equals(active)) return;
        flush();
        modules.clearFailures();
        loadProfile(name);
        markDirty();
    }

    public void create(String name, boolean copyCurrent) throws IOException {
        if (!validName(name)) throw new IOException("Use 1-32 letters, digits, space, '-' or '_'");
        if (profileExists(name)) throw new IOException("A profile with that name exists");
        Map<String, Object> p = copyCurrent ? new LinkedHashMap<String, Object>(snapshotProfile()) : newProfile(name);
        p.put("name", name);
        synchronized (writeLock) {
            AtomicFiles.write(profileFile(name), Json.write(p, true));
        }
    }

    public void rename(String from, String to) throws IOException {
        if (!validName(to)) throw new IOException("Use 1-32 letters, digits, space, '-' or '_'");
        if (profileExists(to)) throw new IOException("A profile with that name exists");
        if (from.equals(active)) {
            flush();
            Files.move(profileFile(from), profileFile(to));
            active = to;
            markDirty();
        } else {
            Files.move(profileFile(from), profileFile(to));
        }
    }

    public void delete(String name) throws IOException {
        if (name.equals(active)) throw new IOException("Switch to another profile first");
        backupNow(profileFile(name));
        Files.deleteIfExists(profileFile(name));
    }

    public String export(String name) throws IOException {
        Map<String, Object> p = name.equals(active) ? snapshotProfile() : parse(profileFile(name), true);
        return ProfileCodec.encode(p);
    }

    /** Imports a shared string as a new profile; returns the name used. */
    public String importProfile(String text) throws IOException {
        Map<String, Object> p;
        try {
            p = ProfileCodec.decode(text);
        } catch (IllegalArgumentException e) {
            throw new IOException(e.getMessage());
        }
        String base = sanitize(Json.str(p, "name", "Imported"));
        String name = base;
        for (int i = 2; profileExists(name); i++) name = (base.length() > 28 ? base.substring(0, 28) : base) + " " + i;
        p.put("name", name);
        synchronized (writeLock) {
            AtomicFiles.write(profileFile(name), Json.write(p, true));
        }
        return name;
    }

    public Path profileFile(String name) {
        return profilesDir.resolve(name + ".json");
    }

    public static boolean validName(String n) {
        return n != null && n.matches("[A-Za-z0-9 _-]{1,32}") && !n.trim().isEmpty();
    }

    static String sanitize(String n) {
        String s = n == null ? "" : n.replaceAll("[^A-Za-z0-9 _-]", "").trim();
        if (s.length() > 32) s = s.substring(0, 32).trim();
        return s.isEmpty() ? "Imported" : s;
    }

    private static Map<String, Object> newProfile(String name) {
        Map<String, Object> p = new LinkedHashMap<String, Object>();
        p.put("schema", (double) Migrations.PROFILE);
        p.put("name", name);
        p.put("modules", new LinkedHashMap<String, Object>());
        return p;
    }

    private static Map<String, Object> newGlobal() {
        Map<String, Object> g = new LinkedHashMap<String, Object>();
        g.put("schema", (double) Migrations.GLOBAL);
        g.put("activeProfile", DEFAULT_PROFILE);
        return g;
    }

    public static String lower(String s) {
        return s.toLowerCase(Locale.ROOT);
    }

    public void shutdown() {
        flush();
        io.shutdown();
        try {
            io.awaitTermination(2, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
