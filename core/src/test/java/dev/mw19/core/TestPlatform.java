package dev.mw19.core;

import dev.mw19.api.Logger;
import dev.mw19.api.render.ItemRef;
import dev.mw19.core.platform.ChatAccess;
import dev.mw19.core.platform.ModList;
import dev.mw19.core.platform.Platform;
import dev.mw19.core.platform.ScreenHost;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/** Headless Platform for tests: no world, no screen, everything inert. */
final class TestPlatform implements Platform, ScreenHost, ChatAccess, ModList {
    private final Path gameDir;
    /** Errors logged by any test instance (tests that care reset it). */
    static final java.util.concurrent.atomic.AtomicInteger ERRORS = new java.util.concurrent.atomic.AtomicInteger();

    TestPlatform(Path gameDir) {
        this.gameDir = gameDir;
    }

    static Mw19 boot(Path gameDir) {
        return Mw19.boot(new TestPlatform(gameDir), "test");
    }

    private static final ItemRef EMPTY = new ItemRef() {
        public boolean isEmpty() { return true; }
        public String id() { return "minecraft:air"; }
        public int count() { return 0; }
        public int damage() { return 0; }
        public int maxDamage() { return 0; }
        public String displayName() { return ""; }
    };

    public Logger logger() {
        return new Logger() {
            public void info(String msg) { System.out.println("[test] " + msg); }
            public void warn(String msg) { System.out.println("[test] WARN " + msg); }
            public void error(String msg, Throwable t) { ERRORS.incrementAndGet(); System.out.println("[test] ERROR " + msg + (t == null ? "" : " " + t)); }
        };
    }

    public Path gameDir() { return gameDir; }
    public ScreenHost screens() { return this; }
    public dev.mw19.core.platform.Skins skins() { return skins; }
    public float health() { return 0; }
    public void setHitboxes(boolean on) {}
    public void tntTimers(boolean on, int range) {}
    public double reachTo(int entityId) { return -1; }
    public float maxHealth() { return 0; }
    public int food() { return 0; }
    public float saturation() { return 0; }
    public dev.mw19.core.platform.Packs packs() { return packs; }
    public dev.mw19.core.platform.Host host() { return host; }
    final dev.mw19.core.platform.Skins skins = new dev.mw19.core.platform.Skins() {
        public int loadImage(byte[] png) { return 0; }
        public void releaseImage(int handle) {}
        public boolean ownSkinSlim() { return false; }
        public String accessToken() { return null; }
    };
    final java.util.List<String> enabledPacks = new java.util.ArrayList<String>();
    final dev.mw19.core.platform.Packs packs = new dev.mw19.core.platform.Packs() {
        public Path folder() { return gameDir.resolve("resourcepacks"); }
        public java.util.List<String> enabled() { return enabledPacks; }
        public boolean enable(String fileName) { enabledPacks.add(0, fileName); return true; }
    };
    final dev.mw19.core.platform.Host host = new dev.mw19.core.platform.Host() {
        public boolean available() { return false; }
        public int port() { return -1; }
        public int open(int gameMode, boolean commands) { return -1; }
        public void setWhitelist(boolean on) {}
        public void allow(UUID id, String name) {}
        public void disallow(UUID id, String name) {}
    };
    public ChatAccess chat() { return this; }
    public ModList mods() { return this; }
    public boolean reducedDebugInfo() { return false; }
    public boolean hideGui() { return false; }
    public int perspective() { return 0; }
    public String clipboard() { return ""; }
    public void setClipboard(String text) {}
    public void vanillaBindings(BindingSink sink) {}
    final java.util.Map<String, String> options = new java.util.HashMap<String, String>();
    public java.util.Map<String, String> applyVideo(java.util.Map<String, String> values) {
        java.util.Map<String, String> prev = new java.util.LinkedHashMap<String, String>();
        for (java.util.Map.Entry<String, String> e : values.entrySet()) {
            String old = options.containsKey(e.getKey()) ? options.get(e.getKey()) : DEFAULTS.get(e.getKey());
            if (old == null || old.equals(e.getValue())) continue;
            prev.put(e.getKey(), old);
            options.put(e.getKey(), e.getValue());
        }
        return prev;
    }
    /** The options this fake game knows, at vanilla's defaults (others are "not on this version"). */
    static final java.util.Map<String, String> DEFAULTS = new java.util.HashMap<String, String>();
    static {
        DEFAULTS.put("renderDistance", "12");
        DEFAULTS.put("simulationDistance", "12");
        DEFAULTS.put("clouds", "FANCY");
        DEFAULTS.put("particles", "ALL");
        DEFAULTS.put("vsync", "true");
    }
    public int videoOption(String id) {
        String v = options.containsKey(id) ? options.get(id) : DEFAULTS.get(id);
        try { return v == null ? -1 : Integer.parseInt(v); } catch (NumberFormatException e) { return -1; }
    }
    String gpu = "Mesa Intel(R) UHD Graphics 620 (KBL GT2)";
    public String gpuName() { return gpu; }
    public String gpuKind() { return ""; }
    public void restoreOptions(java.util.Map<String, String> previous) { options.putAll(previous); }
    public String graphicsApi() { return null; }
    public void setGraphicsApi(String api) {}
    public int bindingKey(Binding b) { return b == Binding.ATTACK ? Keys.mouse(0) : b == Binding.USE ? Keys.mouse(1) : Keys.NONE; }
    public void openFolder(Path dir) {}
    public void screenshot(String name) {}
    public void quit() {}
    public void openWorld(String folder, long seed) {}
    public void effects(EffectSink sink) {}
    public ItemRef itemIcon(String itemId) { return EMPTY; }
    public float attackCooldown() { return 1f; }
    public int hurtTime() { return 0; }
    public boolean sprinting() { return false; }
    public boolean sneaking() { return false; }
    public void setToggle(Binding binding, boolean enabled) {}
    public boolean toggleLatched(Binding binding) { return false; }
    public void setSmoothCamera(boolean on) {}
    public void setPerspective(int perspective) {}
    public long dayTime() { return -1; }
    public Path screenshotsDir() { return gameDir.resolve("screenshots"); }
    public void playPing() {}
    public void debugIncomingChat(String text) {}
    public String minecraftVersion() { return "test"; }
    public String loader() { return "test"; }
    public boolean inWorld() { return false; }
    public String playerName() { return "Tester"; }
    public UUID playerUuid() { return new UUID(0, 1); }
    public double x() { return 0; }
    public double y() { return 0; }
    public double z() { return 0; }
    public float yaw() { return 0; }
    public float pitch() { return 0; }
    public int fps() { return 60; }
    public int ping() { return -1; }
    public String serverAddress() { return null; }
    public boolean singleplayer() { return true; }
    public ItemRef armor(int slot) { return EMPTY; }
    public ItemRef mainHand() { return EMPTY; }
    public ItemRef offHand() { return EMPTY; }
    public int countItem(String itemId) { return 0; }
    public boolean isKeyDown(int key) { return false; }
    public String keyName(int key) { return Keys.name(key); }
    public boolean bindingDown(Binding b) { return false; }
    public String bindingName(Binding b) { return "?"; }
    public int screenWidth() { return 400; }
    public int screenHeight() { return 240; }
    public boolean supports(String feature) { return false; }
    // ScreenHost
    public void openGui() {}
    public void closeGui() {}
    public Kind current() { return Kind.NONE; }
    public void openSingleplayer() {}
    public void openMultiplayer() {}
    public void openOptions() {}
    public void openVanillaTitle() {}
    public boolean hasModList() { return false; }
    public void openModList() {}
    public int width() { return 400; }
    public int height() { return 240; }
    public float guiScale() { return 2f; }
    // ChatAccess
    public void showLocal(String formatted) {}
    public void sendMessage(String message) {}
    public void sendCommand(String command) {}
    // ModList
    public boolean isLoaded(String modId) { return false; }
    public List<String> describe() { return Collections.emptyList(); }
}
