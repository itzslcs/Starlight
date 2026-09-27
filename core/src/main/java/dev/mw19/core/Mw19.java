package dev.mw19.core;

import dev.mw19.api.event.ClientTickEvent;
import dev.mw19.api.event.KeyPressEvent;
import dev.mw19.api.event.ServerEvent;
import dev.mw19.api.module.Module;
import dev.mw19.api.module.Rule;
import dev.mw19.core.config.AtomicFiles;
import dev.mw19.core.config.ClientSettings;
import dev.mw19.core.config.ConfigManager;
import dev.mw19.core.event.EventBus;
import dev.mw19.core.event.SchedulerImpl;
import dev.mw19.core.gui.Anim;
import dev.mw19.core.gui.GuiRoot;
import dev.mw19.core.gui.Theme;
import dev.mw19.core.gui.Toasts;
import dev.mw19.core.hud.HudManager;
import dev.mw19.core.module.ModuleManager;
import dev.mw19.core.modules.BuiltinModules;
import dev.mw19.core.platform.Platform;
import dev.mw19.core.platform.ScreenHost;
import dev.mw19.core.render.Gfx;
import dev.mw19.core.render.RenderBackend;
import dev.mw19.core.rules.ServerRules;

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
public final class Mw19 {
    public static final String NAME = "MW19";

    private static volatile Mw19 instance;

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
    public final dev.mw19.core.modules.InputRates rates = new dev.mw19.core.modules.InputRates();
    public final VideoPresets video = new VideoPresets(this);
    public final dev.mw19.core.perf.Occlusion occlusion = new dev.mw19.core.perf.Occlusion();
    /** Block entities get their own cache so they never evict entity results. */
    public final dev.mw19.core.perf.Occlusion blockOcclusion = new dev.mw19.core.perf.Occlusion();
    public final dev.mw19.core.net.HttpClient http;
    public final dev.mw19.core.host.WorldHost host = new dev.mw19.core.host.WorldHost(this);
    public final dev.mw19.core.skin.SkinLibrary skins;
    /** Set by the home screen's Host World: open the Host page once a singleplayer world has loaded. */
    public boolean hostWhenWorldOpens;
    public Theme theme = Theme.preset("MW19");
    public String currentServer;
    private GuiRoot gui;
    private final Gfx hudGfx = new Gfx();
    private final Gfx menuGfx = new Gfx();
    private final ClientTickEvent tickEvent = new ClientTickEvent();
    private final dev.mw19.core.event.ScrollEvent scrollEvent = new dev.mw19.core.event.ScrollEvent();
    private final dev.mw19.core.event.AttackEvent attackEvent = new dev.mw19.core.event.AttackEvent();
    private final dev.mw19.api.event.ChatReceivedEvent chatEvent = new dev.mw19.api.event.ChatReceivedEvent();
    private final OverlayCall overlayCall = new OverlayCall();

    private static final class OverlayCall implements Runnable {
        private ModuleManager.State s;
        private Gfx g;
        private float w, h;

        void set(ModuleManager.State s, Gfx g, float w, float h) {
            this.s = s;
            this.g = g;
            this.w = w;
            this.h = h;
        }

        @Override
        public void run() {
            ((dev.mw19.core.module.Overlay) s.module).renderOverlay(g, w, h);
        }
    }
    private final KeyPressEvent keyEvent = new KeyPressEvent();
    private Smoke smoke;
    private Bench bench;
    private boolean started;

    private Mw19(Platform platform, String modVersion) {
        this.platform = platform;
        this.modVersion = modVersion;
        this.compat = new Compat(platform.mods());
        this.config = new ConfigManager(configDir(platform.gameDir()), modules, hud, client, scheduler);
        this.http = new dev.mw19.core.net.HttpClient(scheduler, modVersion);
        this.skins = new dev.mw19.core.skin.SkinLibrary(configDir(platform.gameDir()).resolve("skins"));
    }

    /** {@code <gameDir>/MW19}; a folder from before the rename ({@code Kestrel}, 2026-09-26) is moved over once. */
    static java.nio.file.Path configDir(java.nio.file.Path gameDir) {
        java.nio.file.Path dir = gameDir.resolve(NAME), old = gameDir.resolve("Kestrel");
        try {
            if (!java.nio.file.Files.exists(dir) && java.nio.file.Files.isDirectory(old)) java.nio.file.Files.move(old, dir);
        } catch (java.io.IOException e) {
            Log.warn("could not move " + old + " to " + dir + ": " + e);
        }
        return dir;
    }

    public static Mw19 get() {
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
                Mw19 k = boot(platform, modVersion);
                Log.info(NAME + " " + modVersion + " on Minecraft " + platform.minecraftVersion() + " (" + platform.loader()
                        + "); compat: " + k.compat.describePresent());
            }
        });
    }

    /** Builds and publishes an instance (tests call this directly to get a fresh client per case). */
    static Mw19 boot(Platform platform, String modVersion) {
        Mw19 k = new Mw19(platform, modVersion);
        instance = k; // before start(): modules enabled by the profile may call Mw19.get() in onEnable
        k.start();
        return k;
    }

    private void start() {
        modules.holdUntilGameReady();
        BuiltinModules.registerAll(this);
        config.load();
        if (config.freshInstall) video.markFresh();
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
            for (dev.mw19.api.setting.Setting<?> set : s.module.settings()) set.addListener(dirtyListener);
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
        if (compat.has("keycps")) {
            toast("KeyCPS is built in", "MW19 includes KeyCPS (Mods → KeyCPS). Remove the standalone KeyCPS mod to avoid two overlays.", theme.warn);
        }
        if ("1".equals(System.getProperty("mw19.smoke"))) smoke = new Smoke(this);
        if ("1".equals(System.getProperty("mw19.bench"))) bench = new Bench(this);
        started = true;
    }

    private final Runnable dirtyListener = new Runnable() {
        @Override
        public void run() {
            config.markDirty();
        }
    };

    /** Registers a module and applies the active profile's stored state to it. */
    public ModuleManager.State register(Module m, String owner) {
        ModuleManager.State s = modules.register(m, owner);
        for (dev.mw19.api.setting.Setting<?> set : m.settings()) set.addListener(dirtyListener);
        if (started) {
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
        String bundled = resource("/mw19/serverrules.json");
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
            InputStream in = Mw19.class.getResourceAsStream(path);
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
    private dev.mw19.core.gui.TitleUi home;

    /** The home screen that replaces the title screen (created on first use; game thread). */
    public dev.mw19.core.gui.TitleUi home() {
        if (home == null) home = new dev.mw19.core.gui.TitleUi(this);
        return home;
    }

    public GuiRoot gui() {
        if (gui == null) gui = new GuiRoot(this);
        return gui;
    }

    /** The menu instance on screen is the live one (see {@link #unloadGui}). */
    public void adoptGui(GuiRoot shown) {
        gui = shown;
    }

    /**
     * The menu closed: release its textures and drop it, so it costs nothing (memory or GPU) until it is opened again.
     * Deferred to the next tick, because closing can happen while its own screen is still unwinding.
     */
    public void unloadGui(final GuiRoot closed) {
        scheduler.runOnMain(new Runnable() {
            @Override
            public void run() {
                if (gui != closed || platform.screens().current() == dev.mw19.core.platform.ScreenHost.Kind.OURS) return;
                gui = null;
                Guard.run("unload menu", new Runnable() {
                    @Override
                    public void run() {
                        closed.dispose();
                    }
                });
            }
        });
    }

    public void openGui() {
        platform.screens().openGui();
    }

    // ================================================================== static hooks (platform → core)

    public static void onTick(final boolean end) {
        final Mw19 k = instance;
        if (k == null) return;
        long t0 = System.nanoTime();
        k.hooks.tick = true;
        Guard.run("tick", end ? k.tickEnd : k.tickStart);
        k.perf.tickUs = k.perf.tickUs * 0.9 + (System.nanoTime() - t0) / 1000.0 * 0.1;
    }

    private final Runnable tickStart = new Runnable() {
        @Override
        public void run() {
            if (modules.gameReady() && bench == null) video.firstRun(config.freshInstall); // first tick only
            scheduler.tick();
            tickEvent.end = false;
            events.post(tickEvent);
        }
    };

    private final Runnable tickEnd = new Runnable() {
        @Override
        public void run() {
            modules.tick();
            host.tick();
            if (hostWhenWorldOpens) {
                dev.mw19.core.platform.ScreenHost.Kind screen = platform.screens().current();
                if (!platform.inWorld() && screen == dev.mw19.core.platform.ScreenHost.Kind.TITLE) {
                    hostWhenWorldOpens = false; // backed out of the world list
                } else if (host.available() && platform.inWorld() && screen == dev.mw19.core.platform.ScreenHost.Kind.NONE) {
                    hostWhenWorldOpens = false;
                    gui().openHost();
                }
            }
            tickEvent.end = true;
            events.post(tickEvent);
            if (smoke != null) smoke.tick();
            if (bench != null) bench.tick();
        }
    };

    /** Once per frame after the vanilla HUD, whether or not F1 hides it. */
    public static void onHudRender(RenderBackend backend, int screenWidth, int screenHeight) {
        Mw19 k = instance;
        if (k == null) return;
        long t0 = System.nanoTime();
        k.hooks.hud = true;
        if (k.bench != null) k.bench.frame();
        long frameNow = System.currentTimeMillis();
        k.occlusion.newFrame(frameNow);
        k.blockOcclusion.newFrame(frameNow);
        Gfx g = k.hudGfx;
        try {
            g.begin(backend, System.currentTimeMillis());
            boolean editing = k.gui != null && k.gui.isHudEditorOpen() && k.platform.screens().current() == ScreenHost.Kind.OURS;
            if (!k.platform.hideGui() && !editing) {
                k.hud.render(g, screenWidth, screenHeight, false);
                for (ModuleManager.State s : k.modules.activeOverlay()) {
                    k.overlayCall.set(s, g, screenWidth, screenHeight);
                    k.modules.guard(s, "overlay", k.overlayCall);
                }
            }
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
        Mw19 k = instance;
        if (k == null) return false;
        k.hooks.key = true;
        try {
            ScreenHost.Kind screen = k.platform.screens().current();
            // KeyCPS counts presses and OS key-repeat in game (not while typing in a screen).
            if ((action == Keys.ACTION_PRESS || action == Keys.ACTION_REPEAT) && screen == ScreenHost.Kind.NONE) {
                k.rates.record(k.platform, key, System.currentTimeMillis());
            }
            if (action == Keys.ACTION_PRESS && screen == ScreenHost.Kind.NONE) {
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

    /** Mouse button event (any screen); modules only see presses while no screen is open. */
    public static void onMouseButton(int button, int action) {
        Mw19 k = instance;
        if (k == null) return;
        k.hooks.mouse = true;
        try {
            if (action != Keys.ACTION_PRESS) return;
            ScreenHost.Kind screen = k.platform.screens().current();
            int code = Keys.mouse(button);
            // Clicks count in game and in our own GUI (so the HUD editor preview shows your CPS), not in inventories.
            if (screen == ScreenHost.Kind.NONE || screen == ScreenHost.Kind.OURS) k.rates.record(k.platform, code, System.currentTimeMillis());
            if (screen == ScreenHost.Kind.NONE) {
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

    /** Mouse wheel while no screen is open; returns true when a module consumed it (e.g. zoom). */
    public static boolean onScroll(double amount) {
        Mw19 k = instance;
        if (k == null || k.platform.screens().current() != ScreenHost.Kind.NONE) return false;
        try {
            k.scrollEvent.amount = amount;
            k.scrollEvent.consumed = false;
            k.events.post(k.scrollEvent);
            return k.scrollEvent.consumed;
        } catch (VirtualMachineError e) {
            throw e;
        } catch (Throwable t) {
            Log.error("hook scroll failed", t);
            return false;
        }
    }

    /** The local player attacked an entity. */
    public static void onAttack(int entityId) {
        Mw19 k = instance;
        if (k == null) return;
        try {
            k.attackEvent.entityId = entityId;
            k.events.post(k.attackEvent);
        } catch (VirtualMachineError e) {
            throw e;
        } catch (Throwable t) {
            Log.error("hook attack failed", t);
        }
    }

    /** Incoming chat line: event listeners may cancel it, then Chat Tools may decorate it. Never throws. */
    public static void onChat(dev.mw19.core.chat.ChatLine line) {
        Mw19 k = instance;
        if (k == null) return;
        k.hooks.chat = true;
        try {
            k.chatEvent.reset(line.plain, line.formatted);
            k.events.post(k.chatEvent);
            if (k.chatEvent.cancelled()) {
                line.cancel = true;
                return;
            }
            ModuleManager.State s = k.modules.get("chat");
            if (s != null && s.active()) {
                final dev.mw19.core.chat.ChatLine l = line;
                final dev.mw19.core.modules.ChatModule chat = (dev.mw19.core.modules.ChatModule) s.module;
                k.modules.guard(s, "chat", new Runnable() {
                    @Override
                    public void run() {
                        chat.process(l);
                    }
                });
            }
        } catch (VirtualMachineError e) {
            throw e;
        } catch (Throwable t) {
            Log.error("hook chat failed", t);
        }
    }

    public static void onServerJoin(final String address) {
        final Mw19 k = instance;
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
        final Mw19 k = instance;
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

    private volatile long lastProbeToast;

    /** Exploit Protection stopped a probe (any thread). Tells the player at most once a minute. */
    public static void onProbeBlocked(final String what) {
        final Mw19 k = instance;
        if (k == null) return;
        long now = System.currentTimeMillis();
        Log.info("exploit protection: blocked " + what);
        if (now - k.lastProbeToast < 60_000) return;
        k.lastProbeToast = now;
        k.scheduler.runOnMain(new Runnable() {
            @Override
            public void run() {
                k.toast("Exploit Protection", "This server tried " + what + ". MW19 answered like a client without mods.", k.theme.warn);
            }
        });
    }

    /** A vanilla title/pause screen finished init: returns whether to add our menu button. */
    /**
     * A vanilla button's background (pause menu, server list, options...) drawn like the home screen's buttons: dark
     * glass with a thin edge that lights up on hover. False when "MW19 game menus" is off, so vanilla draws its own.
     * Render thread, no allocation.
     */
    public static boolean vanillaButton(RenderBackend backend, float x, float y, float w, float h, boolean hovered, boolean active, float alpha) {
        Mw19 k = instance;
        if (k == null || !k.client.styleMenus.on()) return false;
        Gfx g = k.menuGfx;
        try {
            g.begin(backend, 0);
            g.pushAlpha(alpha);
            dev.mw19.core.gui.MenuStyle.key(g, x, y, w, h, hovered ? 1f : 0f, active, dev.mw19.core.gui.MenuStyle.glow(k.theme.accent));
            g.popAlpha();
            return true;
        } catch (VirtualMachineError e) {
            throw e;
        } catch (Throwable t) {
            Log.error("hook vanillaButton failed", t);
            return false;
        } finally {
            try {
                g.end();
            } catch (Throwable ignored) {
            }
        }
    }

    /** A vanilla slider (options, volume, FOV) in the MW19 style: its track and handle; vanilla draws the label. */
    public static boolean vanillaSlider(RenderBackend backend, float x, float y, float w, float h, float value, boolean hovered, boolean active, float alpha) {
        Mw19 k = instance;
        if (k == null || !k.client.styleMenus.on()) return false;
        Gfx g = k.menuGfx;
        try {
            g.begin(backend, 0);
            g.pushAlpha(alpha);
            dev.mw19.core.gui.MenuStyle.slider(g, x, y, w, h, value, hovered ? 1f : 0f, active, dev.mw19.core.gui.MenuStyle.glow(k.theme.accent));
            g.popAlpha();
            return true;
        } catch (VirtualMachineError e) {
            throw e;
        } catch (Throwable t) {
            Log.error("hook vanillaSlider failed", t);
            return false;
        } finally {
            try {
                g.end();
            } catch (Throwable ignored) {
            }
        }
    }

    /**
     * A vanilla screen's background (pause menu, server list, options...): the MW19 backdrop instead of the panorama,
     * blur and dark overlay. False = vanilla draws its own (MW19 game menus off, or it failed).
     */
    public static boolean menuBackdrop(RenderBackend backend, float w, float h, boolean overWorld) {
        Mw19 k = instance;
        if (k == null || !k.client.styleMenus.on()) return false;
        k.hooks.backdrop = true;
        Gfx g = k.menuGfx;
        long now = System.currentTimeMillis();
        try {
            g.begin(backend, now);
            dev.mw19.core.gui.MenuStyle.backdrop(g, w, h, overWorld, dev.mw19.core.gui.MenuStyle.glow(k.theme.accent), now, dev.mw19.core.gui.Anim.speed <= 0);
            return true;
        } catch (VirtualMachineError e) {
            throw e;
        } catch (Throwable t) {
            Log.error("hook menuBackdrop failed", t);
            return false;
        } finally {
            try {
                g.end();
            } catch (Throwable ignored) {
            }
        }
    }

    /** Pause screen: MW19 Menu opens the menu, Packs opens it on the resource pack browser. */
    public static void openFromPause(boolean packs) {
        Mw19 k = instance;
        if (k == null) return;
        k.openGui();
        if (packs) k.gui().openPage(dev.mw19.core.gui.page.PacksPage.class);
    }

    public static boolean wantMenuButton() {
        Mw19 k = instance;
        if (k == null) return false;
        k.hooks.screenButton = true;
        return k.client.menuButtons.on();
    }

    /** Game is closing: save synchronously. */
    public static void onShutdown() {
        final Mw19 k = instance;
        if (k == null) return;
        Guard.run("shutdown", new Runnable() {
            @Override
            public void run() {
                k.host.shutdown();
                k.config.shutdown();
                k.scheduler.shutdown();
                k.http.shutdown();
            }
        });
    }

    /** For crash reports: never throws. */
    public static String crashReportDetails() {
        Mw19 k = instance;
        if (k == null) return "not initialised";
        try {
            return "version " + k.modVersion + "; profile " + k.config.active() + "; enabled modules: "
                    + k.modules.describeEnabled() + "; hooks: " + k.hooks.describe();
        } catch (Throwable t) {
            return "unavailable (" + t + ")";
        }
    }
}
