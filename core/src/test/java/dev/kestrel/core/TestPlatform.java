package dev.kestrel.core;

import dev.kestrel.api.Logger;
import dev.kestrel.api.render.ItemRef;
import dev.kestrel.core.platform.ChatAccess;
import dev.kestrel.core.platform.ModList;
import dev.kestrel.core.platform.Platform;
import dev.kestrel.core.platform.ScreenHost;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/** Headless Platform for tests: no world, no screen, everything inert. */
final class TestPlatform implements Platform, ScreenHost, ChatAccess, ModList {
    private final Path gameDir;

    TestPlatform(Path gameDir) {
        this.gameDir = gameDir;
    }

    static Kestrel boot(Path gameDir) {
        return Kestrel.boot(new TestPlatform(gameDir), "test");
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
            public void error(String msg, Throwable t) { System.out.println("[test] ERROR " + msg + (t == null ? "" : " " + t)); }
        };
    }

    public Path gameDir() { return gameDir; }
    public ScreenHost screens() { return this; }
    public ChatAccess chat() { return this; }
    public ModList mods() { return this; }
    public boolean reducedDebugInfo() { return false; }
    public boolean hideGui() { return false; }
    public int perspective() { return 0; }
    public String clipboard() { return ""; }
    public void setClipboard(String text) {}
    public void vanillaBindings(BindingSink sink) {}
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
