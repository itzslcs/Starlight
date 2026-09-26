package dev.mw19.core.plugin;

import dev.mw19.api.Mw19Api;
import dev.mw19.api.Plugin;
import dev.mw19.api.util.Json;
import dev.mw19.core.Mw19;
import dev.mw19.core.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Loads plugin jars from {@code <gameDir>/MW19/plugins}. Order: parse → API check → dependencies (topological) →
 * first-run consent (id + SHA-256 of the jar) → isolated class loader → {@link Plugin#onEnable}. Nothing here can take
 * the game down: every failure marks the plugin FAILED with a message shown on the Plugins page.
 */
public final class PluginManager {
    public enum State { NEEDS_CONSENT, DISABLED, ENABLED, FAILED, INCOMPATIBLE }

    public static final class Entry {
        public final Path jar;
        public final String sha256;
        public PluginDescriptor descriptor;
        public State state = State.DISABLED;
        public String error;
        URLClassLoader loader;
        Plugin instance;
        PluginContextImpl context;

        Entry(Path jar, String sha256) {
            this.jar = jar;
            this.sha256 = sha256;
        }

        public String id() {
            return descriptor != null ? descriptor.id : jar.getFileName().toString();
        }
    }

    private final Mw19 k;
    private final Path dir;
    private final List<Entry> entries = new ArrayList<Entry>();

    public PluginManager(Mw19 k, Path dir) {
        this.k = k;
        this.dir = dir;
    }

    public List<Entry> entries() {
        return Collections.unmodifiableList(entries);
    }

    /** Scans the folder and enables what the user already approved. Called once at startup (game thread). */
    public void loadAll() {
        entries.clear();
        List<Path> jars = new ArrayList<Path>();
        try {
            Files.createDirectories(dir);
            DirectoryStream<Path> ds = Files.newDirectoryStream(dir, "*.jar");
            try {
                for (Path p : ds) jars.add(p);
            } finally {
                ds.close();
            }
        } catch (IOException e) {
            Log.error("cannot read plugins folder " + dir, e);
            return;
        }
        Collections.sort(jars);
        Map<String, Entry> byId = new LinkedHashMap<String, Entry>();
        for (Path jar : jars) {
            Entry e;
            try {
                e = new Entry(jar, sha256(jar));
                e.descriptor = PluginDescriptor.parse(readDescriptor(jar));
            } catch (Exception ex) {
                e = new Entry(jar, "?");
                fail(e, State.INCOMPATIBLE, jar.getFileName() + ": " + ex.getMessage());
                entries.add(e);
                continue;
            }
            entries.add(e);
            if (byId.containsKey(e.descriptor.id)) {
                fail(e, State.INCOMPATIBLE, "duplicate plugin id " + e.descriptor.id + " (also in " + byId.get(e.descriptor.id).jar.getFileName() + ")");
                continue;
            }
            if (!e.descriptor.apiCompatible(Mw19Api.MAJOR, Mw19Api.MINOR)) {
                fail(e, State.INCOMPATIBLE, "needs plugin API " + e.descriptor.api + ", this client has " + Mw19Api.VERSION);
                continue;
            }
            byId.put(e.descriptor.id, e);
        }
        for (Entry e : order(byId)) {
            if (e.state == State.INCOMPATIBLE) continue;
            if (!consented(e)) {
                e.state = State.NEEDS_CONSENT;
            } else if (userDisabled(e.descriptor.id)) {
                e.state = State.DISABLED;
            } else {
                enable(e);
            }
        }
        int waiting = 0;
        for (Entry e : entries) if (e.state == State.NEEDS_CONSENT) waiting++;
        if (waiting > 0) k.toast("Plugins", waiting + " new plugin(s) need your approval (menu → Plugins)", k.theme.warn);
    }

    /** Dependency order; missing deps and cycles mark plugins INCOMPATIBLE. */
    private List<Entry> order(Map<String, Entry> byId) {
        List<Entry> out = new ArrayList<Entry>();
        Map<String, Integer> mark = new HashMap<String, Integer>(); // 1 visiting, 2 done
        for (Entry e : byId.values()) visit(e, byId, mark, out);
        return out;
    }

    private boolean visit(Entry e, Map<String, Entry> byId, Map<String, Integer> mark, List<Entry> out) {
        Integer m = mark.get(e.descriptor.id);
        if (m != null) {
            if (m == 1) {
                fail(e, State.INCOMPATIBLE, "dependency cycle through " + e.descriptor.id);
                return false;
            }
            return e.state != State.INCOMPATIBLE;
        }
        mark.put(e.descriptor.id, 1);
        boolean ok = e.state != State.INCOMPATIBLE;
        for (Map.Entry<String, String> dep : e.descriptor.depends.entrySet()) {
            Entry d = byId.get(dep.getKey());
            if (d == null) {
                fail(e, State.INCOMPATIBLE, "missing dependency " + dep.getKey() + " " + dep.getValue());
                ok = false;
            } else if (!PluginDescriptor.satisfies(d.descriptor.version, dep.getValue())) {
                fail(e, State.INCOMPATIBLE, "needs " + dep.getKey() + " " + dep.getValue() + ", found " + d.descriptor.version);
                ok = false;
            } else if (!visit(d, byId, mark, out)) {
                fail(e, State.INCOMPATIBLE, "dependency " + dep.getKey() + " failed");
                ok = false;
            }
        }
        mark.put(e.descriptor.id, 2);
        out.add(e);
        return ok;
    }

    // ------------------------------------------------------------------ consent / user state

    private Map<String, Object> consentMap() {
        return k.config.section("pluginConsent");
    }

    public boolean consented(Entry e) {
        return e.sha256.equals(consentMap().get(e.descriptor.id));
    }

    /** The user accepted the warning for exactly this jar (a changed jar asks again). */
    public void grantConsent(Entry e) {
        consentMap().put(e.descriptor.id, e.sha256);
        setUserDisabled(e.descriptor.id, false);
        k.config.markDirty();
        if (e.state == State.NEEDS_CONSENT || e.state == State.DISABLED) enable(e);
    }

    private boolean userDisabled(String id) {
        return Json.bool(k.config.section("pluginDisabled"), id, false);
    }

    private void setUserDisabled(String id, boolean off) {
        if (off) k.config.section("pluginDisabled").put(id, true);
        else k.config.section("pluginDisabled").remove(id);
        k.config.markDirty();
    }

    // ------------------------------------------------------------------ enable / disable

    public void setEnabled(Entry e, boolean on) {
        if (e.state == State.INCOMPATIBLE || e.state == State.NEEDS_CONSENT) return;
        setUserDisabled(e.descriptor.id, !on);
        if (on && e.state != State.ENABLED) enable(e);
        else if (!on && e.state == State.ENABLED) disable(e, State.DISABLED);
    }

    private void enable(Entry e) {
        for (Map.Entry<String, String> dep : e.descriptor.depends.entrySet()) {
            Entry d = find(dep.getKey());
            if (d == null || d.state != State.ENABLED) {
                fail(e, State.FAILED, "dependency " + dep.getKey() + " is not enabled");
                return;
            }
        }
        try {
            ClassLoader parent = Mw19.class.getClassLoader();
            e.loader = new URLClassLoader(new URL[]{e.jar.toUri().toURL()}, parent);
            Class<?> cls = Class.forName(e.descriptor.main, true, e.loader);
            if (!Plugin.class.isAssignableFrom(cls)) throw new IllegalArgumentException(e.descriptor.main + " does not implement " + Plugin.class.getName());
            e.instance = (Plugin) cls.getDeclaredConstructor().newInstance();
            e.context = new PluginContextImpl(k, e.descriptor);
            e.instance.onEnable(e.context);
            e.state = State.ENABLED;
            e.error = null;
            Log.info("plugin " + e.descriptor.id + " " + e.descriptor.version + " enabled");
        } catch (VirtualMachineError err) {
            throw err;
        } catch (Throwable t) {
            Log.error("plugin " + e.id() + " failed to enable", t);
            cleanup(e);
            fail(e, State.FAILED, t.getClass().getSimpleName() + ": " + t.getMessage());
            k.toast("Plugin failed", e.descriptor.name + ": " + e.error, k.theme.bad);
        }
    }

    private void disable(Entry e, State next) {
        if (e.instance != null) {
            try {
                e.instance.onDisable();
            } catch (VirtualMachineError err) {
                throw err;
            } catch (Throwable t) {
                Log.error("plugin " + e.id() + " failed to disable cleanly", t);
            }
        }
        cleanup(e);
        e.state = next;
    }

    private void cleanup(Entry e) {
        if (e.context != null) e.context.close();
        e.context = null;
        e.instance = null;
        if (e.loader != null) {
            try {
                e.loader.close();
            } catch (IOException ignored) {
                // best effort
            }
        }
        e.loader = null;
    }

    private void fail(Entry e, State s, String why) {
        e.state = s;
        e.error = why;
        Log.warn("plugin " + e.id() + ": " + why);
    }

    public Entry find(String id) {
        for (Entry e : entries) if (e.descriptor != null && e.descriptor.id.equals(id)) return e;
        return null;
    }

    public void shutdown() {
        for (int i = entries.size() - 1; i >= 0; i--) if (entries.get(i).state == State.ENABLED) disable(entries.get(i), State.DISABLED);
    }

    // ------------------------------------------------------------------ jar helpers

    static String readDescriptor(Path jar) throws IOException {
        ZipFile z = new ZipFile(jar.toFile());
        try {
            ZipEntry en = z.getEntry("plugin.json");
            if (en == null) throw new IOException("no plugin.json in jar");
            if (en.getSize() > 64 * 1024) throw new IOException("plugin.json too large");
            InputStream in = z.getInputStream(en);
            try {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                byte[] buf = new byte[4096];
                int n;
                while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
                return new String(out.toByteArray(), StandardCharsets.UTF_8);
            } finally {
                in.close();
            }
        } finally {
            z.close();
        }
    }

    static String sha256(Path file) throws IOException {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            InputStream in = Files.newInputStream(file);
            try {
                byte[] buf = new byte[65536];
                int n;
                while ((n = in.read(buf)) > 0) md.update(buf, 0, n);
            } finally {
                in.close();
            }
            StringBuilder sb = new StringBuilder();
            for (byte b : md.digest()) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IOException(e);
        }
    }
}
