package dev.mw19.forge;

import dev.mw19.api.Logger;
import dev.mw19.api.render.ItemRef;
import dev.mw19.core.Keys;
import dev.mw19.core.Mw19;
import dev.mw19.core.platform.ChatAccess;
import dev.mw19.core.platform.ModList;
import dev.mw19.core.platform.Platform;
import dev.mw19.core.platform.ScreenHost;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.GuiIngameMenu;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.ModContainer;
import org.apache.logging.log4j.LogManager;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Platform for Forge 1.8.9 (MCP names). */
public final class ForgePlatform implements Platform, ScreenHost, ChatAccess, ModList {
    private final Minecraft mc = Minecraft.getMinecraft();
    private final org.apache.logging.log4j.Logger log = LogManager.getLogger("MW19");
    private final ForgeItem[] armor = {new ForgeItem(), new ForgeItem(), new ForgeItem(), new ForgeItem()};
    private final ForgeItem main = new ForgeItem(), off = new ForgeItem(), probe = new ForgeItem();
    private int scaledW = 1, scaledH = 1, scale = 1;
    private final Map<String, ForgeItem> icons = new HashMap<String, ForgeItem>();
    // 1.8.9 has no vanilla toggle mode: latch the key ourselves (GRAY modules only)
    private final boolean[] toggleOn = new boolean[2], latched = new boolean[2], wasPhysical = new boolean[2];
    private float savedGamma = Float.NaN;
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

    /** ScaledResolution allocates; refresh once per tick/frame instead of per query. */
    void refreshResolution() {
        ScaledResolution r = new ScaledResolution(mc);
        refreshResolution(r);
    }

    void refreshResolution(ScaledResolution r) {
        scaledW = r.getScaledWidth();
        scaledH = r.getScaledHeight();
        scale = r.getScaleFactor();
    }

    /** Start of every client tick. */
    void tick() {
        pollServer();
        applyGamma();
        tickToggle(0, mc.gameSettings.keyBindSprint);
        tickToggle(1, mc.gameSettings.keyBindSneak);
    }

    /** Brightness: 1.8.9 does not clamp gammaSetting at render time, so set it while the module is on. */
    private void applyGamma() {
        double g = dev.mw19.core.Hooks.gamma;
        if (!Double.isNaN(g)) {
            if (Float.isNaN(savedGamma)) savedGamma = mc.gameSettings.gammaSetting;
            mc.gameSettings.gammaSetting = (float) g;
        } else if (!Float.isNaN(savedGamma)) {
            mc.gameSettings.gammaSetting = savedGamma;
            savedGamma = Float.NaN;
        }
    }

    private boolean physicallyDown(KeyBinding kb) {
        int code = kb.getKeyCode();
        if (code < 0) return Mouse.isButtonDown(code + 100);
        return code > 0 && Keyboard.isKeyDown(code);
    }

    private void tickToggle(int i, KeyBinding kb) {
        if (!toggleOn[i]) return;
        boolean phys = mc.currentScreen == null && physicallyDown(kb);
        if (phys && !wasPhysical[i]) latched[i] = !latched[i];
        wasPhysical[i] = phys;
        if (latched[i] && mc.currentScreen == null) KeyBinding.setKeyBindState(kb.getKeyCode(), true);
    }

    void pollServer() {
        boolean inWorld = inWorld();
        String server = inWorld && mc.getCurrentServerData() != null ? mc.getCurrentServerData().serverIP : null;
        if (inWorld && (!wasInWorld || !eq(server, lastServer))) {
            if (wasInWorld) Mw19.onServerLeave();
            Mw19.onServerJoin(server);
        } else if (!inWorld && wasInWorld) {
            Mw19.onServerLeave();
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
        return "1.8.9";
    }

    @Override
    public String loader() {
        return "forge";
    }

    @Override
    public boolean inWorld() {
        return mc.theWorld != null && mc.thePlayer != null;
    }

    @Override
    public String playerName() {
        return mc.getSession().getUsername();
    }

    @Override
    public UUID playerUuid() {
        UUID id = mc.getSession().getProfile().getId();
        return id != null ? id : (mc.thePlayer != null ? mc.thePlayer.getUniqueID() : new UUID(0, 0));
    }

    @Override
    public double x() {
        return mc.thePlayer == null ? 0 : mc.thePlayer.posX;
    }

    @Override
    public double y() {
        return mc.thePlayer == null ? 0 : mc.thePlayer.posY;
    }

    @Override
    public double z() {
        return mc.thePlayer == null ? 0 : mc.thePlayer.posZ;
    }

    @Override
    public float yaw() {
        return mc.thePlayer == null ? 0 : mc.thePlayer.rotationYaw;
    }

    @Override
    public float pitch() {
        return mc.thePlayer == null ? 0 : mc.thePlayer.rotationPitch;
    }

    @Override
    public int fps() {
        return Minecraft.getDebugFPS();
    }

    @Override
    public int ping() {
        if (mc.thePlayer == null || mc.getNetHandler() == null) return -1;
        NetworkPlayerInfo info = mc.getNetHandler().getPlayerInfo(mc.thePlayer.getUniqueID());
        return info == null ? -1 : info.getResponseTime();
    }

    @Override
    public String serverAddress() {
        return mc.getCurrentServerData() == null ? null : mc.getCurrentServerData().serverIP;
    }

    @Override
    public boolean singleplayer() {
        return mc.isSingleplayer();
    }

    @Override
    public ItemRef armor(int slot) {
        return armor[slot & 3].set(mc.thePlayer == null ? null : mc.thePlayer.inventory.armorInventory[slot & 3]);
    }

    @Override
    public ItemRef mainHand() {
        return main.set(mc.thePlayer == null ? null : mc.thePlayer.getCurrentEquippedItem());
    }

    @Override
    public ItemRef offHand() {
        return off.set(null);
    }

    @Override
    public int countItem(String itemId) {
        if (mc.thePlayer == null) return 0;
        int n = 0;
        for (ItemStack s : mc.thePlayer.inventory.mainInventory) {
            if (s != null && itemId.equals(probe.set(s).id())) n += s.stackSize;
        }
        return n;
    }

    @Override
    public boolean isKeyDown(int key) {
        if (Keys.isMouse(key)) return Mouse.isButtonDown(key - Keys.MOUSE_BASE);
        int l = LwjglKeys.toLwjgl(key);
        return l > 0 && Keyboard.isKeyDown(l);
    }

    @Override
    public String keyName(int key) {
        return Keys.name(key);
    }

    private KeyBinding mapping(Binding b) {
        GameSettings o = mc.gameSettings;
        switch (b) {
            case FORWARD: return o.keyBindForward;
            case LEFT: return o.keyBindLeft;
            case BACK: return o.keyBindBack;
            case RIGHT: return o.keyBindRight;
            case JUMP: return o.keyBindJump;
            case SNEAK: return o.keyBindSneak;
            case SPRINT: return o.keyBindSprint;
            case ATTACK: return o.keyBindAttack;
            case SCREENSHOT: return o.keyBindScreenshot;
            default: return o.keyBindUseItem;
        }
    }

    @Override
    public boolean bindingDown(Binding b) {
        return mapping(b).isKeyDown();
    }

    @Override
    public int bindingKey(Binding b) {
        return LwjglKeys.fromBinding(mapping(b).getKeyCode());
    }

    @Override
    public String bindingName(Binding b) {
        int code = mapping(b).getKeyCode();
        if (code < 0) {
            int v = code + 100;
            return v == 0 ? "LMB" : v == 1 ? "RMB" : v == 2 ? "MMB" : "M" + (v + 1);
        }
        String s = GameSettings.getKeyDisplayString(code);
        return s.length() > 5 ? s.substring(0, 5) : s;
    }

    @Override
    public int screenWidth() {
        return scaledW;
    }

    @Override
    public int screenHeight() {
        return scaledH;
    }

    @Override
    public boolean supports(String feature) {
        return false; // no offhand, no shield, no menu blur on 1.8.9
    }

    // ------------------------------------------------------------------ Platform

    @Override
    public Logger logger() {
        return logger;
    }

    @Override
    public Path gameDir() {
        return mc.mcDataDir.toPath();
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
        return (mc.thePlayer != null && mc.thePlayer.hasReducedDebug()) || mc.gameSettings.reducedDebugInfo;
    }

    @Override
    public boolean hideGui() {
        return mc.gameSettings.hideGUI;
    }

    @Override
    public int perspective() {
        return mc.gameSettings.thirdPersonView;
    }

    @Override
    public String clipboard() {
        return GuiScreen.getClipboardString();
    }

    @Override
    public void setClipboard(String text) {
        GuiScreen.setClipboardString(text);
    }

    @Override
    public void vanillaBindings(BindingSink sink) {
        for (KeyBinding kb : mc.gameSettings.keyBindings) {
            int code = LwjglKeys.fromBinding(kb.getKeyCode());
            if (code != Keys.NONE) sink.accept(I18n.format(kb.getKeyDescription()), code);
        }
    }

    @Override
    public void openFolder(Path dir) {
        try {
            java.nio.file.Files.createDirectories(dir);
            org.lwjgl.Sys.openURL(dir.toUri().toString());
        } catch (Exception e) {
            log.warn("could not open " + dir + ": " + e);
        }
    }

    @Override
    public void screenshot(String name) {
        IChatComponent msg = ScreenShotHelper.saveScreenshot(mc.mcDataDir, name + ".png", mc.displayWidth, mc.displayHeight, mc.getFramebuffer());
        log.info("screenshot: " + (msg == null ? "?" : msg.getUnformattedText()));
    }

    @Override
    public void quit() {
        mc.shutdown();
    }

    @Override
    public void openWorld(String folder, long seed) {
        mc.launchIntegratedServer(folder, folder, new WorldSettings(seed, WorldSettings.GameType.CREATIVE, false, false, WorldType.DEFAULT));
    }

    // ------------------------------------------------------------------ Phase 4 data

    @Override
    public void effects(EffectSink sink) {
        if (mc.thePlayer == null) return;
        for (net.minecraft.potion.PotionEffect e : mc.thePlayer.getActivePotionEffects()) {
            int id = e.getPotionID();
            net.minecraft.potion.Potion p = id >= 0 && id < net.minecraft.potion.Potion.potionTypes.length ? net.minecraft.potion.Potion.potionTypes[id] : null;
            if (p == null) continue;
            sink.accept(I18n.format(p.getName()), e.getAmplifier(), e.getDuration() >= 32767 ? -1 : e.getDuration(), p.getLiquidColor(), !p.isBadEffect());
        }
    }

    @Override
    public ItemRef itemIcon(String itemId) {
        ForgeItem f = icons.get(itemId);
        if (f == null) {
            net.minecraft.item.Item item = net.minecraft.item.Item.itemRegistry.getObject(new net.minecraft.util.ResourceLocation(itemId));
            f = new ForgeItem().set(item == null ? null : new ItemStack(item));
            icons.put(itemId, f);
        }
        return f;
    }

    @Override
    public float attackCooldown() {
        return 1f;
    }

    @Override
    public int hurtTime() {
        return mc.thePlayer == null ? 0 : mc.thePlayer.hurtTime;
    }

    @Override
    public boolean sprinting() {
        return mc.thePlayer != null && mc.thePlayer.isSprinting();
    }

    @Override
    public boolean sneaking() {
        return mc.thePlayer != null && mc.thePlayer.isSneaking();
    }

    @Override
    public void setToggle(Binding binding, boolean enabled) {
        int i = binding == Binding.SPRINT ? 0 : binding == Binding.SNEAK ? 1 : -1;
        if (i < 0) return;
        toggleOn[i] = enabled;
        if (!enabled && latched[i]) {
            KeyBinding kb = i == 0 ? mc.gameSettings.keyBindSprint : mc.gameSettings.keyBindSneak;
            KeyBinding.setKeyBindState(kb.getKeyCode(), physicallyDown(kb));
        }
        latched[i] = false;
    }

    @Override
    public boolean toggleLatched(Binding binding) {
        int i = binding == Binding.SPRINT ? 0 : binding == Binding.SNEAK ? 1 : -1;
        return i >= 0 && toggleOn[i] && latched[i];
    }

    @Override
    public void setSmoothCamera(boolean on) {
        mc.gameSettings.smoothCamera = on;
    }

    @Override
    public void setPerspective(int perspective) {
        mc.gameSettings.thirdPersonView = Math.max(0, Math.min(2, perspective));
    }

    @Override
    public long dayTime() {
        return mc.theWorld == null ? -1 : mc.theWorld.getWorldTime();
    }

    @Override
    public Path screenshotsDir() {
        return mc.mcDataDir.toPath().resolve("screenshots");
    }

    /** Mirrors NetHandlerPlayClient.handleChat: post the Forge event, print unless cancelled. */
    @Override
    public void debugIncomingChat(String text) {
        net.minecraftforge.client.event.ClientChatReceivedEvent e =
                new net.minecraftforge.client.event.ClientChatReceivedEvent((byte) 1, new ChatComponentText(text));
        if (!net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(e) && e.message != null) {
            mc.ingameGUI.getChatGUI().printChatMessage(e.message);
        }
    }

    @Override
    public void playPing() {
        mc.getSoundHandler().playSound(net.minecraft.client.audio.PositionedSoundRecord.create(new net.minecraft.util.ResourceLocation("note.pling"), 1.5f));
    }

    // ------------------------------------------------------------------ ScreenHost

    @Override
    public void openGui() {
        if (!(mc.currentScreen instanceof Mw19GuiScreen)) mc.displayGuiScreen(new Mw19GuiScreen(mc.currentScreen));
    }

    @Override
    public void closeGui() {
        if (mc.currentScreen instanceof Mw19GuiScreen) ((Mw19GuiScreen) mc.currentScreen).close();
    }

    @Override
    public Kind current() {
        GuiScreen s = mc.currentScreen;
        if (s == null) return Kind.NONE;
        if (s instanceof Mw19GuiScreen) return Kind.OURS;
        if (s instanceof GuiMainMenu) return Kind.TITLE;
        if (s instanceof GuiIngameMenu) return Kind.PAUSE;
        if (s instanceof GuiChat) return Kind.CHAT;
        if (s instanceof GuiContainer) return Kind.INVENTORY;
        return Kind.OTHER;
    }

    @Override
    public int width() {
        return scaledW;
    }

    @Override
    public int height() {
        return scaledH;
    }

    @Override
    public float guiScale() {
        return scale;
    }

    // ------------------------------------------------------------------ ChatAccess

    @Override
    public void showLocal(String formatted) {
        mc.ingameGUI.getChatGUI().printChatMessage(new ChatComponentText(formatted));
    }

    @Override
    public void sendMessage(String message) {
        if (mc.thePlayer != null) mc.thePlayer.sendChatMessage(message);
    }

    @Override
    public void sendCommand(String command) {
        if (mc.thePlayer != null) mc.thePlayer.sendChatMessage("/" + command);
    }

    // ------------------------------------------------------------------ ModList

    @Override
    public boolean isLoaded(String modId) {
        return Loader.isModLoaded(modId);
    }

    @Override
    public List<String> describe() {
        List<String> out = new ArrayList<String>();
        for (ModContainer c : Loader.instance().getActiveModList()) out.add(c.getModId() + " " + c.getVersion());
        return out;
    }
}
