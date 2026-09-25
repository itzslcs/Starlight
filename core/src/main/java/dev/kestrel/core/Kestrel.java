package dev.kestrel.core;

import dev.kestrel.api.KestrelApi;
import dev.kestrel.api.event.ClientTickEvent;
import dev.kestrel.api.event.KeyPressEvent;
import dev.kestrel.api.event.ServerEvent;
import dev.kestrel.api.module.Module;
import dev.kestrel.api.module.Rule;
import dev.kestrel.core.config.AtomicFiles;
import dev.kestrel.core.config.ClientSettings;
import dev.kestrel.core.config.ConfigManager;
import dev.kestrel.core.event.EventBus;
import dev.kestrel.core.event.SchedulerImpl;
import dev.kestrel.core.gui.Anim;
import dev.kestrel.core.gui.GuiRoot;
import dev.kestrel.core.gui.Theme;
import dev.kestrel.core.gui.Toasts;
import dev.kestrel.core.hud.HudManager;
import dev.kestrel.core.module.ModuleManager;
import dev.kestrel.core.modules.BuiltinModules;
import dev.kestrel.core.platform.Platform;
import dev.kestrel.core.platform.ScreenHost;
import dev.kestrel.core.render.Gfx;
import dev.kestrel.core.render.RenderBackend;
import dev.kestrel.core.rules.ServerRules;

import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

/**
 * Core entry point. Platform adapters call the static hooks below; each is guarded and cheap when idle.
 * Game-thread only unless stated otherwise.
 */
public final class Kestrel {
    public static final String NAME = "Kestrel";

    private static volatile Kestrel instance;

    public final Platform platform;
    public final String modVersion;
    public final EventBus events = new EventBus();
    public final SchedulerImpl scheduler = new SchedulerImpl();
    public final ModuleManager modules = new ModuleManager();
    public final HudManager hud = new HudManager(modules);
    public final ClientSettings client = new ClientSettings();
    public final ConfigManager config;
    public final Toasts toasts = new Toasts();
    public final ServerRules serverRules = new ServerRules();
    public final PerfStats perf = new PerfStats();
    public final HookWatchdog hooks = new HookWatchdog();
    public final Compat compat;
    public final dev.kestrel.core.modules.ClickTracker clicks = new dev.kestrel.core.modules.ClickTracker();
    public Theme theme = Theme.preset("Kestrel");
    public String currentServer;
    private GuiRoot gui;
    private final Gfx hudGfx = new Gfx();
    private final ClientTickEvent tickEvent = new ClientTickEvent();
    private final KeyPressEvent keyEvent = new KeyPressEvent();
    private Smoke smoke;

    private Kestrel(Platform platform, String modVersion) {
        this.platform = platform;
        this.modVersion = modVersion;
        this.compat = new Compat(platform.mods());
        this.config = new ConfigManager(platform.gameDir().resolve(NAME), modules, hud, client, scheduler);
    }

    public static Kestrel get() {
        return instance;
    }

    public static boolean ready() {
        return instance != null;
    }

    /** Called once from the mod entrypoint. Never throws. */
    public static void init(final Platform platform, final String modVersion) {
        if (instance != null) return;
        Log.bind(platform.logger());
        Guard.run("init", new Runnable() {
            @Override
            public void run() {
                Kestrel k = new Kestrel(platform, modVersion);
                k.start();
                instance = k;
                Log.info(NAME + " " + modVersion + " on Minecraft " + platform.minecraftVersion() + " (" + platform.loader()
                        + "), API " + KestrelApi.VERSION + "; compat: " + k.compat.describePresent());
            }
        });
    }

    private void start() {
        BuiltinModules.registerAll(this);
        config.load();
        modules.setListener(new ModuleManager.Listener() {
            @Override
            public void changed(ModuleManager.State s) {
                config.markDirty();
            }

            @Override
            public void failed(ModuleManager.State s, Throwable cause) {
                toast(s.module.name() + " disabled", "It failed " + ModuleManager.MAX_FAILURES + " times; see the log. Re-enable it from the menu.", theme.bad);
            }
        });
        for (ModuleManager.State s : modules.all()) {
            for (dev.kestrel.api.setting.Setting<?> set : s.module.settings()) set.addListener(dirtyListener);
        }
        client.onAnyChange(new Runnable() {
            @Override
            public void run() {
                applyClientSettings();
                config.markDirty();
            }
        });
        applyClientSettings();
        loadServerRules();
        for (String n : config.notices) toast("Config restored", n, theme.warn);
        config.notices.clear();
        if ("1".equals(System.getProperty("kestrel.smoke"))) smoke = new Smoke(this);
    }

    private final Runnable dirtyListener = new Runnable() {
        @Override
        public void run() {
            config.markDirty();
        }
    };

    /** Registers a module (built-in or plugin) and applies the active profile's stored state to it. */
    public ModuleManager.State register(Module m, String owner) {
        ModuleManager.State s = modules.register(m, owner);
        for (dev.kestrel.api.setting.Setting<?> set : m.settings()) set.addListener(dirtyListener);
        if (instance != null) {
            config.applyTo(s);
            applyRulesTo(s, currentServer == null ? null : serverRules.disallowedFor(currentServer));
        }
        return s;
    }

    void applyClientSettings() {
        Theme t = Theme.preset(client.theme.get());
        if (client.customAccent.on()) t.accent = client.accent.get();
        theme = t;
        Anim.speed = client.animSpeed.floatValue();
        toasts.enabled = client.toasts.on();
        applyRules();
    }

    public void toast(String title, String msg, int color) {
        toasts.show(title, msg, color);
    }

    // ------------------------------------------------------------------ server rules

    public void loadServerRules() {
        String bundled = resource("/kestrel/serverrules.json");
        String user = null;
        Path f = config.root.resolve("serverrules.json");
        try {
            if (Files.exists(f)) user = AtomicFiles.read(f);
            serverRules.load(bundled, user);
        } catch (Exception e) {
            Log.error("serverrules.json is invalid; using bundled rules", e);
            toast("Server rules", "Your serverrules.json is invalid (" + e.getMessage() + "); using bundled rules.", theme.warn);
            try {
                serverRules.load(bundled, null);
            } catch (Exception e2) {
                Log.error("bundled server rules invalid", e2);
            }
        }
        applyRules();
    }

    private void applyRules() {
        Set<String> dis = currentServer == null ? null : serverRules.disallowedFor(currentServer);
        for (ModuleManager.State s : modules.all()) applyRulesTo(s, dis);
    }

    private void applyRulesTo(ModuleManager.State s, Set<String> disallowed) {
        modules.setSuspended(s, ModuleManager.SUSPEND_SERVER, disallowed != null && disallowed.contains(s.module.id()));
        modules.setSuspended(s, ModuleManager.SUSPEND_SAFE, client.competitiveSafe.on() && s.module.rule() != Rule.ALLOWED);
    }

    static String resource(String path) {
        try {
            InputStream in = Kestrel.class.getResourceAsStream(path);
            if (in == null) return "{\"schema\":1,\"servers\":[]}";
            try {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                byte[] buf = new byte[4096];
                int n;
                while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
                return new String(out.toByteArray(), StandardCharsets.UTF_8);
            } finally {
                in.close();
            }
        } catch (Exception e) {
            return "{\"schema\":1,\"servers\":[]}";
        }
    }

    // ------------------------------------------------------------------ GUI

    /** The GUI tree, created on first open. */
    public GuiRoot gui() {
        if (gui == null) gui = new GuiRoot(this);
        return gui;
    }

    public void openGui() {
        platform.screens().openGui();
    }

    // ================================================================== static hooks (platform → core)

    public static void onTick(final boolean end) {
        final Kestrel k = instance;
        if (k == null) return;
        long t0 = System.nanoTime();
        k.hooks.tick = true;
        Guard.run("tick", end ? k.tickEnd : k.tickStart);
        k.perf.tickUs = k.perf.tickUs * 0.9 + (System.nanoTime() - t0) / 1000.0 * 0.1;
    }

    private final Runnable tickStart = new Runnable() {
        @Override
        public void run() {
            scheduler.tick();
            tickEvent.end = false;
            events.post(tickEvent);
        }
    };

    private final Runnable tickEnd = new Runnable() {
        @Override
        public void run() {
            modules.tick();
            tickEvent.end = true;
            events.post(tickEvent);
            if (smoke != null) smoke.tick();
        }
    };

    /** Once per frame after the vanilla HUD, whether or not F1 hides it. */
    public static void onHudRender(RenderBackend backend, int screenWidth, int screenHeight) {
        Kestrel k = instance;
        if (k == null) return;
        long t0 = System.nanoTime();
        k.hooks.hud = true;
        Gfx g = k.hudGfx;
        try {
            g.begin(backend, System.currentTimeMillis());
            boolean editing = k.gui != null && k.gui.isHudEditorOpen() && k.platform.screens().current() == ScreenHost.Kind.OURS;
            if (!k.platform.hideGui() && !editing) k.hud.render(g, screenWidth, screenHeight, false);
            if (k.platform.screens().current() != ScreenHost.Kind.OURS) k.toasts.render(g, k.theme, screenWidth, g.millis());
        } catch (VirtualMachineError e) {
            throw e;
        } catch (Throwable t) {
            Log.error("hook hud failed", t);
        } finally {
            try {
                g.end();
            } catch (Throwable ignored) {
                // backend already gone
            }
        }
        k.perf.frameOwn += System.nanoTime() - t0;
        k.perf.frame(System.nanoTime());
    }

    /**
     * Keyboard event while no screen is open. Returns true if we consumed it (vanilla should ignore it).
     * {@code action}: 0 release, 1 press, 2 repeat.
     */
    public static boolean onKey(int key, int action, int mods) {
        Kestrel k = instance;
        if (k == null) return false;
        k.hooks.key = true;
        try {
            if (action == Keys.ACTION_PRESS && k.platform.screens().current() == ScreenHost.Kind.NONE) {
                if (key == k.client.openGui.key()) {
                    k.openGui();
                    return true;
                }
                k.keyEvent.key = key;
                k.events.post(k.keyEvent);
            }
        } catch (VirtualMachineError e) {
            throw e;
        } catch (Throwable t) {
            Log.error("hook key failed", t);
        }
        return false;
    }

    /** Mouse button event while no screen is open. */
    public static void onMouseButton(int button, int action) {
        Kestrel k = instance;
        if (k == null) return;
        k.hooks.mouse = true;
        try {
            if (action == Keys.ACTION_PRESS && k.platform.screens().current() == ScreenHost.Kind.NONE) {
                k.clicks.press(button, System.currentTimeMillis());
                int code = Keys.mouse(button);
                if (code == k.client.openGui.key()) {
                    k.openGui();
                    return;
                }
                k.keyEvent.key = code;
                k.events.post(k.keyEvent);
            }
        } catch (VirtualMachineError e) {
            throw e;
        } catch (Throwable t) {
            Log.error("hook mouse failed", t);
        }
    }

    public static void onServerJoin(final String address) {
        final Kestrel k = instance;
        if (k == null) return;
        k.hooks.serverJoin = true;
        Guard.run("serverJoin", new Runnable() {
            @Override
            public void run() {
                k.currentServer = address;
                String auto = k.config.profileFor(address);
                if (auto != null && !auto.equals(k.config.active())) {
                    k.config.switchTo(auto);
                    k.toast("Profile", "Switched to \"" + auto + "\" for " + address, k.theme.accent);
                }
                k.applyRules();
                Set<String> dis = k.serverRules.disallowedFor(address);
                StringBuilder off = new StringBuilder();
                for (String id : dis) {
                    ModuleManager.State s = k.modules.get(id);
                    if (s != null && s.enabled()) off.append(off.length() == 0 ? "" : ", ").append(s.module.name());
                }
                if (off.length() > 0) k.toast("Server rules", "Disabled on this server: " + off, k.theme.warn);
                k.events.post(new ServerEvent(true, address));
            }
        });
    }

    public static void onServerLeave() {
        final Kestrel k = instance;
        if (k == null) return;
        Guard.run("serverLeave", new Runnable() {
            @Override
            public void run() {
                String was = k.currentServer;
                k.currentServer = null;
                k.applyRules();
                k.events.post(new ServerEvent(false, was));
            }
        });
    }

    /** A vanilla title/pause screen finished init: returns whether to add our menu button. */
    public static boolean wantMenuButton() {
        Kestrel k = instance;
        if (k == null) return false;
        k.hooks.screenButton = true;
        return k.client.menuButtons.on();
    }

    /** Game is closing: save synchronously. */
    public static void onShutdown() {
        final Kestrel k = instance;
        if (k == null) return;
        Guard.run("shutdown", new Runnable() {
            @Override
            public void run() {
                k.config.shutdown();
                k.scheduler.shutdown();
            }
        });
    }

    /** For crash reports: never throws. */
    public static String crashReportDetails() {
        Kestrel k = instance;
        if (k == null) return "not initialised";
        try {
            return "version " + k.modVersion + "; profile " + k.config.active() + "; enabled modules: "
                    + k.modules.describeEnabled() + "; hooks: " + k.hooks.describe();
        } catch (Throwable t) {
            return "unavailable (" + t + ")";
        }
    }
}
