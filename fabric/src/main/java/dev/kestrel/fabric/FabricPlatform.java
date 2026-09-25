package dev.kestrel.fabric;

import com.mojang.blaze3d.platform.InputConstants;
import dev.kestrel.api.Logger;
import dev.kestrel.api.render.ItemRef;
import dev.kestrel.core.Keys;
import dev.kestrel.core.Kestrel;
import dev.kestrel.core.platform.ChatAccess;
import dev.kestrel.core.platform.ModList;
import dev.kestrel.core.platform.Platform;
import dev.kestrel.core.platform.ScreenHost;
import dev.kestrel.fabric.mixin.KeyMappingAccessor;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.client.CameraType;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
//? if >=1.21.11 {
import net.minecraft.util.Util;
//?} else {
/*import net.minecraft.Util;
*///?}
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import org.lwjgl.glfw.GLFW;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Platform for Fabric targets. Everything version-specific lives here, in FabricBackend and in the mixins. */
public final class FabricPlatform implements Platform, ScreenHost, ChatAccess, ModList {
    private static final EquipmentSlot[] ARMOR = {EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD};

    private final Minecraft mc = Minecraft.getInstance();
    private final org.slf4j.Logger log = LoggerFactory.getLogger("Kestrel");
    private final FabricItem[] armor = {new FabricItem(), new FabricItem(), new FabricItem(), new FabricItem()};
    private final FabricItem main = new FabricItem(), off = new FabricItem();
    private final String mcVersion;
    private boolean wasInWorld;
    private String lastServer;

    private final Logger logger = new Logger() {
        @Override
        public void info(String msg) {
            log.info(msg);
        }

        @Override
        public void warn(String msg) {
            log.warn(msg);
        }

        @Override
        public void error(String msg, Throwable t) {
            if (t == null) log.error(msg);
            else log.error(msg, t);
        }
    };

    public FabricPlatform() {
        this.mcVersion = FabricLoader.getInstance().getModContainer("minecraft")
                .map(c -> c.getMetadata().getVersion().getFriendlyString()).orElse("?");
    }

    /** Called at the start of every client tick: detects world/server changes without extra mixins. */
    void pollServer() {
        boolean inWorld = mc.level != null && mc.player != null;
        String server = inWorld && mc.getCurrentServer() != null ? mc.getCurrentServer().ip : null;
        if (inWorld && (!wasInWorld || !eq(server, lastServer))) {
            if (wasInWorld) Kestrel.onServerLeave();
            Kestrel.onServerJoin(server);
        } else if (!inWorld && wasInWorld) {
            Kestrel.onServerLeave();
        }
        wasInWorld = inWorld;
        lastServer = server;
    }

    private static boolean eq(String a, String b) {
        return a == null ? b == null : a.equals(b);
    }

    // ------------------------------------------------------------------ Game

    @Override
    public String minecraftVersion() {
        return mcVersion;
    }

    @Override
    public String loader() {
        return "fabric";
    }

    @Override
    public boolean inWorld() {
        return mc.level != null && mc.player != null;
    }

    @Override
    public String playerName() {
        return mc.getUser().getName();
    }

    @Override
    public UUID playerUuid() {
        return mc.getUser().getProfileId();
    }

    @Override
    public double x() {
        return mc.player == null ? 0 : mc.player.getX();
    }

    @Override
    public double y() {
        return mc.player == null ? 0 : mc.player.getY();
    }

    @Override
    public double z() {
        return mc.player == null ? 0 : mc.player.getZ();
    }

    @Override
    public float yaw() {
        return mc.player == null ? 0 : mc.player.getYRot();
    }

    @Override
    public float pitch() {
        return mc.player == null ? 0 : mc.player.getXRot();
    }

    @Override
    public int fps() {
        return mc.getFps();
    }

    @Override
    public int ping() {
        if (mc.player == null || mc.getConnection() == null) return -1;
        PlayerInfo info = mc.getConnection().getPlayerInfo(mc.player.getUUID());
        return info == null ? -1 : info.getLatency();
    }

    @Override
    public String serverAddress() {
        return mc.getCurrentServer() == null ? null : mc.getCurrentServer().ip;
    }

    @Override
    public boolean singleplayer() {
        return mc.hasSingleplayerServer();
    }

    @Override
    public ItemRef armor(int slot) {
        return armor[slot & 3].set(mc.player == null ? ItemStack.EMPTY : mc.player.getItemBySlot(ARMOR[slot & 3]));
    }

    @Override
    public ItemRef mainHand() {
        return main.set(mc.player == null ? ItemStack.EMPTY : mc.player.getMainHandItem());
    }

    @Override
    public ItemRef offHand() {
        return off.set(mc.player == null ? ItemStack.EMPTY : mc.player.getOffhandItem());
    }

    @Override
    public int countItem(String itemId) {
        if (mc.player == null) return 0;
        Inventory inv = mc.player.getInventory();
        int n = 0;
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (!s.isEmpty() && BuiltInRegistries.ITEM.getKey(s.getItem()).toString().equals(itemId)) n += s.getCount();
        }
        return n;
    }

    private long window() {
        //? if >=1.21.9 {
        return mc.getWindow().handle();
        //?} else {
        /*return mc.getWindow().getWindow();
        *///?}
    }

    @Override
    public boolean isKeyDown(int key) {
        if (Keys.isMouse(key)) return GLFW.glfwGetMouseButton(window(), key - Keys.MOUSE_BASE) == GLFW.GLFW_PRESS;
        if (key <= 0) return false;
        //? if >=1.21.9 {
        return InputConstants.isKeyDown(mc.getWindow(), key);
        //?} else {
        /*return InputConstants.isKeyDown(window(), key);
        *///?}
    }

    @Override
    public String keyName(int key) {
        return Keys.name(key);
    }

    private KeyMapping mapping(Binding b) {
        switch (b) {
            case FORWARD: return mc.options.keyUp;
            case LEFT: return mc.options.keyLeft;
            case BACK: return mc.options.keyDown;
            case RIGHT: return mc.options.keyRight;
            case JUMP: return mc.options.keyJump;
            case SNEAK: return mc.options.keyShift;
            case SPRINT: return mc.options.keySprint;
            case ATTACK: return mc.options.keyAttack;
            default: return mc.options.keyUse;
        }
    }

    @Override
    public boolean bindingDown(Binding b) {
        return mapping(b).isDown();
    }

    @Override
    public String bindingName(Binding b) {
        KeyMapping km = mapping(b);
        InputConstants.Key key = ((KeyMappingAccessor) km).kestrel$key();
        if (key.getType() == InputConstants.Type.MOUSE) {
            int v = key.getValue();
            return v == 0 ? "LMB" : v == 1 ? "RMB" : v == 2 ? "MMB" : "M" + (v + 1);
        }
        String s = key.getDisplayName().getString();
        return s.length() > 5 ? s.substring(0, 5) : s;
    }

    @Override
    public int screenWidth() {
        return mc.getWindow().getGuiScaledWidth();
    }

    @Override
    public int screenHeight() {
        return mc.getWindow().getGuiScaledHeight();
    }

    @Override
    public boolean supports(String feature) {
        return "offhand".equals(feature) || "shield".equals(feature) || "blur".equals(feature);
    }

    // ------------------------------------------------------------------ Platform

    @Override
    public Logger logger() {
        return logger;
    }

    @Override
    public Path gameDir() {
        return FabricLoader.getInstance().getGameDir();
    }

    @Override
    public ScreenHost screens() {
        return this;
    }

    @Override
    public ChatAccess chat() {
        return this;
    }

    @Override
    public ModList mods() {
        return this;
    }

    @Override
    public boolean reducedDebugInfo() {
        return (mc.player != null && mc.player.isReducedDebugInfo()) || mc.options.reducedDebugInfo().get();
    }

    @Override
    public boolean hideGui() {
        return mc.options.hideGui;
    }

    @Override
    public int perspective() {
        CameraType t = mc.options.getCameraType();
        return t.isFirstPerson() ? 0 : t.isMirrored() ? 2 : 1;
    }

    @Override
    public String clipboard() {
        return mc.keyboardHandler.getClipboard();
    }

    @Override
    public void setClipboard(String text) {
        mc.keyboardHandler.setClipboard(text);
    }

    @Override
    public void vanillaBindings(BindingSink sink) {
        for (KeyMapping km : mc.options.keyMappings) {
            InputConstants.Key key = ((KeyMappingAccessor) km).kestrel$key();
            int code;
            if (key.getType() == InputConstants.Type.MOUSE) code = Keys.mouse(key.getValue());
            else if (key.getType() == InputConstants.Type.KEYSYM) code = key.getValue();
            else continue;
            if (code < 0) continue;
            sink.accept(Component.translatable(km.getName()).getString(), code);
        }
    }

    @Override
    public void openFolder(Path dir) {
        try {
            java.nio.file.Files.createDirectories(dir);
        } catch (java.io.IOException ignored) {
            // opening will report it
        }
        Util.getPlatform().openPath(dir);
    }

    @Override
    public void screenshot(String name) {
        //? if >=1.21.6 {
        Screenshot.grab(mc.gameDirectory, name + ".png", mc.getMainRenderTarget(), 1, msg -> log.info("screenshot: {}", msg.getString()));
        //?} else {
        /*Screenshot.grab(mc.gameDirectory, name + ".png", mc.getMainRenderTarget(), msg -> log.info("screenshot: {}", msg.getString()));
        *///?}
    }

    @Override
    public void quit() {
        mc.stop();
    }

    @Override
    public void openWorld(String folder, long seed) {
        Screen parent = mc.screen;
        if (mc.getLevelSource().levelExists(folder)) {
            mc.createWorldOpenFlows().openWorld(folder, () -> mc.setScreen(parent));
            return;
        }
        //? if >=1.21.11 {
        net.minecraft.world.level.gamerules.GameRules rules = new net.minecraft.world.level.gamerules.GameRules(FeatureFlags.DEFAULT_FLAGS);
        //?} elif >=1.21.2 {
        /*net.minecraft.world.level.GameRules rules = new net.minecraft.world.level.GameRules(FeatureFlags.DEFAULT_FLAGS);
        *///?} else {
        /*net.minecraft.world.level.GameRules rules = new net.minecraft.world.level.GameRules();
        *///?}
        LevelSettings settings = new LevelSettings(folder, GameType.CREATIVE, false, Difficulty.PEACEFUL, true, rules,
                WorldDataConfiguration.DEFAULT);
        mc.createWorldOpenFlows().createFreshLevel(folder, settings, new WorldOptions(seed, false, false),
                WorldPresets::createNormalWorldDimensions, parent);
    }

    // ------------------------------------------------------------------ ScreenHost

    @Override
    public void openGui() {
        if (!(mc.screen instanceof KestrelScreen)) mc.setScreen(new KestrelScreen(mc.screen));
    }

    @Override
    public void closeGui() {
        if (mc.screen instanceof KestrelScreen) mc.screen.onClose();
    }

    @Override
    public Kind current() {
        Screen s = mc.screen;
        if (s == null) return Kind.NONE;
        if (s instanceof KestrelScreen) return Kind.OURS;
        if (s instanceof TitleScreen) return Kind.TITLE;
        if (s instanceof PauseScreen) return Kind.PAUSE;
        if (s instanceof ChatScreen) return Kind.CHAT;
        if (s instanceof AbstractContainerScreen) return Kind.INVENTORY;
        return Kind.OTHER;
    }

    @Override
    public int width() {
        return screenWidth();
    }

    @Override
    public int height() {
        return screenHeight();
    }

    @Override
    public float guiScale() {
        return (float) mc.getWindow().getGuiScale();
    }

    // ------------------------------------------------------------------ ChatAccess

    @Override
    public void showLocal(String formatted) {
        mc.gui.getChat().addMessage(Component.literal(formatted));
    }

    @Override
    public void sendMessage(String message) {
        if (mc.player != null) mc.player.connection.sendChat(message);
    }

    @Override
    public void sendCommand(String command) {
        if (mc.player != null) mc.player.connection.sendCommand(command);
    }

    // ------------------------------------------------------------------ ModList

    @Override
    public boolean isLoaded(String modId) {
        return FabricLoader.getInstance().isModLoaded(modId);
    }

    @Override
    public List<String> describe() {
        List<String> out = new ArrayList<String>();
        for (ModContainer c : FabricLoader.getInstance().getAllMods()) {
            out.add(c.getMetadata().getId() + " " + c.getMetadata().getVersion().getFriendlyString());
        }
        return out;
    }
}
