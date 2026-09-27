package dev.mw19.fabric;

import com.mojang.blaze3d.platform.InputConstants;
import dev.mw19.api.Logger;
import dev.mw19.api.render.ItemRef;
import dev.mw19.core.Hooks;
import dev.mw19.core.Keys;
import dev.mw19.core.Mw19;
import dev.mw19.core.SdlKeys;
import dev.mw19.core.platform.ChatAccess;
import dev.mw19.core.platform.ModList;
import dev.mw19.core.platform.Platform;
import dev.mw19.core.platform.ScreenHost;
import dev.mw19.fabric.mixin.KeyMappingAccessor;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.client.CameraType;
//? if >=1.21.2 {
import net.minecraft.server.level.ParticleStatus;
//?} else {
/*import net.minecraft.client.ParticleStatus;
*///?}
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
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
//? if <26.3
import org.lwjgl.glfw.GLFW;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Platform for Fabric targets. Everything version-specific lives here, in FabricBackend and in the mixins. */
public final class FabricPlatform implements Platform, ScreenHost, ChatAccess, ModList {
    private static final EquipmentSlot[] ARMOR = {EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD};

    private final Minecraft mc = Minecraft.getInstance();
    private final org.slf4j.Logger log = LoggerFactory.getLogger("MW19");
    private final FabricItem[] armor = {new FabricItem(), new FabricItem(), new FabricItem(), new FabricItem()};
    private final FabricItem main = new FabricItem(), off = new FabricItem();
    private final String mcVersion;
    private boolean wasInWorld;
    private String lastServer;
    private final Map<String, FabricItem> icons = new HashMap<String, FabricItem>();
    private boolean savedToggleSprint, savedToggleSneak;
    private int appliedHitColor;

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
    /** Start of every client tick. */
    private int ticks;

    /** Ticks with a menu or world on screen; -1 once the start counts as good. */
    private int upTicks;

    void tick() {
        pollServer();
        if (++ticks % 20 == 0) FastChests.check();
        if (upTicks >= 0 && (mc.level != null || current() == Kind.TITLE || current() == Kind.OURS) && ++upTicks >= 40) {
            upTicks = -1; // loading is over and the game has run for 2 s: Vulkan (if on) got through its start
            if ("vulkan".equals(RendererSwitch.state)) RendererSwitch.started();
            VulkanProbe.maybeRun();
        }
        if (Hooks.hitColor != appliedHitColor) applyHitColor(Hooks.hitColor);
    }

    /** Rewrites the red rows of the entity hurt-overlay texture (vanilla restores with colour 0). */
    private void applyHitColor(int argb) {
        appliedHitColor = argb;
        try {
            net.minecraft.client.renderer.texture.DynamicTexture tex =
                    ((dev.mw19.fabric.mixin.OverlayTextureAccessor) mc.gameRenderer.overlayTexture()).mw19$texture();
            com.mojang.blaze3d.platform.NativeImage img = tex.getPixels();
            if (img == null) return;
            int c = argb == 0 ? 0xB2FF0000 : argb;
            for (int y = 0; y < 8; y++) {
                for (int x = 0; x < 16; x++) {
                    //? if >=1.21.2 {
                    img.setPixel(x, y, c);
                    //?} else {
                    /*img.setPixelRGBA(x, y, (c & 0xFF00FF00) | ((c >> 16) & 0xFF) | ((c & 0xFF) << 16));
                    *///?}
                }
            }
            tex.upload();
        } catch (Throwable t) {
            log.warn("hit colour not applied: {}", t.toString());
        }
    }

    void pollServer() {
        boolean inWorld = mc.level != null && mc.player != null;
        String server = inWorld && mc.getCurrentServer() != null ? mc.getCurrentServer().ip : null;
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
        //? if >=26.3 {
        /*if (Keys.isMouse(key)) {
            int mask = org.lwjgl.sdl.SDLMouse.SDL_GetMouseState((java.nio.FloatBuffer) null, (java.nio.FloatBuffer) null);
            return (mask & (1 << (SdlKeys.sdlButton(key - Keys.MOUSE_BASE) - 1))) != 0;
        }
        int sdl = SdlKeys.toSdl(key);
        return sdl > 0 && InputConstants.isKeyDown(sdl);
        *///?} elif >=1.21.9 {
        if (Keys.isMouse(key)) return GLFW.glfwGetMouseButton(window(), key - Keys.MOUSE_BASE) == GLFW.GLFW_PRESS;
        return key > 0 && InputConstants.isKeyDown(mc.getWindow(), key);
        //?} else {
        /*if (Keys.isMouse(key)) return GLFW.glfwGetMouseButton(window(), key - Keys.MOUSE_BASE) == GLFW.GLFW_PRESS;
        return key > 0 && InputConstants.isKeyDown(window(), key);
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
            case SCREENSHOT: return mc.options.keyScreenshot;
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
        InputConstants.Key key = ((KeyMappingAccessor) km).mw19$key();
        if (key.getType() == InputConstants.Type.MOUSE) {
            int v = FabricCompat.button(key.getValue());
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
        return "offhand".equals(feature) || "shield".equals(feature) || "blur".equals(feature) || "fast_chests".equals(feature);
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
    public dev.mw19.core.platform.Skins skins() {
        return FabricMedia.INSTANCE;
    }

    @Override
    public dev.mw19.core.platform.Packs packs() {
        return FabricMedia.INSTANCE;
    }

    @Override
    public dev.mw19.core.platform.Host host() {
        return FabricHost.INSTANCE;
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
        return FabricCompat.hideGui(mc);
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
            int code = FabricCompat.canonical(((KeyMappingAccessor) km).mw19$key());
            if (code != Keys.NONE) sink.accept(Component.translatable(km.getName()).getString(), code);
        }
    }

    @Override
    public int bindingKey(Binding b) {
        return FabricCompat.canonical(((KeyMappingAccessor) mapping(b)).mw19$key());
    }

    @Override
    public void openFolder(Path dir) {
        try {
            java.nio.file.Files.createDirectories(dir);
        } catch (java.io.IOException ignored) {
            // opening will report it
        }
        //? if >=26.3 {
        /*com.mojang.blaze3d.Blaze3D.openPath(dir);
        *///?} else {
        Util.getPlatform().openPath(dir);
        //?}
    }

    @Override
    public void screenshot(String name) {
        //? if >=26.2 {
        /*Screenshot.grab(mc.gameDirectory, name + ".png", mc.gameRenderer.mainRenderTarget(), 1, msg -> log.info("screenshot: {}", msg.getString()));
        *///?} elif >=1.21.6 {
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
        Screen parent = FabricCompat.screen(mc);
        if (mc.getLevelSource().levelExists(folder)) {
            mc.createWorldOpenFlows().openWorld(folder, () -> FabricCompat.setScreen(mc, parent));
            return;
        }
        //? if >=26.1 {
        /*LevelSettings settings = new LevelSettings(folder, GameType.CREATIVE,
                new LevelSettings.DifficultySettings(Difficulty.PEACEFUL, false, false), true, WorldDataConfiguration.DEFAULT);
        *///?} elif >=1.21.11 {
        LevelSettings settings = new LevelSettings(folder, GameType.CREATIVE, false, Difficulty.PEACEFUL, true,
                new net.minecraft.world.level.gamerules.GameRules(FeatureFlags.DEFAULT_FLAGS), WorldDataConfiguration.DEFAULT);
        //?} elif >=1.21.2 {
        /*LevelSettings settings = new LevelSettings(folder, GameType.CREATIVE, false, Difficulty.PEACEFUL, true,
                new net.minecraft.world.level.GameRules(FeatureFlags.DEFAULT_FLAGS), WorldDataConfiguration.DEFAULT);
        *///?} else {
        /*LevelSettings settings = new LevelSettings(folder, GameType.CREATIVE, false, Difficulty.PEACEFUL, true,
                new net.minecraft.world.level.GameRules(), WorldDataConfiguration.DEFAULT);
        *///?}
        mc.createWorldOpenFlows().createFreshLevel(folder, settings, new WorldOptions(seed, false, false),
                WorldPresets::createNormalWorldDimensions, parent);
    }

    // ------------------------------------------------------------------ Phase 4 data

    @Override
    public void effects(EffectSink sink) {
        if (mc.player == null) return;
        for (MobEffectInstance inst : mc.player.getActiveEffects()) {
            MobEffect e = inst.getEffect().value();
            sink.accept(e.getDisplayName().getString(), inst.getAmplifier(), inst.isInfiniteDuration() ? -1 : inst.getDuration(),
                    e.getColor(), e.isBeneficial());
        }
    }

    @Override
    public ItemRef itemIcon(String itemId) {
        FabricItem f = icons.get(itemId);
        if (f == null) {
            //? if >=1.21.11 {
            net.minecraft.resources.Identifier id = net.minecraft.resources.Identifier.tryParse(itemId);
            //?} else {
            /*net.minecraft.resources.ResourceLocation id = net.minecraft.resources.ResourceLocation.tryParse(itemId);
            *///?}
            net.minecraft.world.item.Item item = id == null ? null : BuiltInRegistries.ITEM.getOptional(id).orElse(null);
            ItemStack stack;
            try {
                stack = item == null ? ItemStack.EMPTY : new ItemStack(item);
            } catch (RuntimeException componentsNotBound) { // 26.x binds item components with the first world: ask again later
                return new FabricItem().set(ItemStack.EMPTY);
            }
            f = new FabricItem().set(stack);
            icons.put(itemId, f);
        }
        return f;
    }

    @Override
    public float attackCooldown() {
        return mc.player == null ? 1f : mc.player.getAttackStrengthScale(0f);
    }

    @Override
    public int hurtTime() {
        return mc.player == null ? 0 : mc.player.hurtTime;
    }

    @Override
    public String selfTest(String what) {
        if ("exploit".equals(what)) return ExploitGuard.selfTest();
        if ("fastchests".equals(what)) return FastChests.selfTest();
        if ("chestfaces".equals(what)) return Integer.toString(FastChests.chestFaces());
        if ("pauserow".equals(what)) {
            Screen s = FabricCompat.screen(mc);
            if (!(s instanceof net.minecraft.client.gui.screens.PauseScreen)) return "FAIL (not the pause menu)";
            int n = 0;
            for (var l : s.children()) {
                if (l instanceof net.minecraft.client.gui.components.Button b
                        && ("Packs".equals(b.getMessage().getString()) || "MW19 Menu".equals(b.getMessage().getString()))) n++;
            }
            return n == 2 ? "ok (MW19 Menu + Packs)" : "FAIL (" + n + " of 2 MW19 buttons)";
        }
        return "n/a";
    }

    @Override
    public void setFastChests(boolean on) {
        FastChests.set(on);
    }

    @Override
    public void setHitboxes(boolean on) {
        //? if >=1.21.9 {
        Mw19Fabric.hitboxes = on; // HitboxesMixin answers for the debug entry; the player's saved F3+B choice stays as it is
        //? if >=26.2 {
        /*mc.levelExtractor.debugRenderer.refreshRendererList();
        *///?} else {
        mc.levelRenderer.debugRenderer.refreshRendererList();
        //?}
        //?} else {
        /*mc.getEntityRenderDispatcher().setRenderHitBoxes(on);
        *///?}
    }

    /** Entity ids whose name MW19 set for the TNT timer (cleared again when the module stops). */
    private final java.util.Set<Integer> tntLabels = new java.util.HashSet<>();

    @Override
    public void tntTimers(boolean on, int range) {
        if (mc.level == null || mc.player == null) {
            tntLabels.clear();
            return;
        }
        double max = (double) range * range;
        for (net.minecraft.world.entity.Entity e : mc.level.entitiesForRendering()) {
            if (!(e instanceof net.minecraft.world.entity.item.PrimedTnt tnt)) continue;
            if (on && e.distanceToSqr(mc.player) <= max) {
                int fuse = tnt.getFuse();
                int rgb = fuse < 20 ? 0xFF5555 : fuse < 40 ? 0xFFAA00 : 0x55FF55;
                e.setCustomName(Component.literal(String.format(java.util.Locale.ROOT, "%.1fs", fuse / 20f)).withColor(rgb));
                e.setCustomNameVisible(true);
                tntLabels.add(e.getId());
            } else if (tntLabels.remove(e.getId())) {
                e.setCustomName(null);
                e.setCustomNameVisible(false);
            }
        }
        if (!on) tntLabels.clear();
    }

    @Override
    public double reachTo(int entityId) {
        if (mc.level == null || mc.player == null) return -1;
        net.minecraft.world.entity.Entity e = mc.level.getEntity(entityId);
        if (e == null) return -1;
        net.minecraft.world.phys.Vec3 eye = mc.player.getEyePosition();
        net.minecraft.world.phys.AABB b = e.getBoundingBox();
        double dx = Math.max(Math.max(b.minX - eye.x, 0), eye.x - b.maxX);
        double dy = Math.max(Math.max(b.minY - eye.y, 0), eye.y - b.maxY);
        double dz = Math.max(Math.max(b.minZ - eye.z, 0), eye.z - b.maxZ);
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    @Override
    public float health() {
        return mc.player == null ? 0 : mc.player.getHealth();
    }

    @Override
    public float maxHealth() {
        return mc.player == null ? 0 : mc.player.getMaxHealth();
    }

    @Override
    public int food() {
        return mc.player == null ? 0 : mc.player.getFoodData().getFoodLevel();
    }

    @Override
    public float saturation() {
        return mc.player == null ? 0 : mc.player.getFoodData().getSaturationLevel();
    }

    @Override
    public boolean sprinting() {
        return mc.player != null && mc.player.isSprinting();
    }

    @Override
    public boolean sneaking() {
        return mc.player != null && mc.player.isShiftKeyDown();
    }

    /** Uses vanilla's own "Toggle" key mode (Accessibility options); restores the previous mode when turned off. */
    @Override
    public void setToggle(Binding binding, boolean enabled) {
        if (binding == Binding.SPRINT) {
            if (enabled) savedToggleSprint = mc.options.toggleSprint().get();
            mc.options.toggleSprint().set(enabled || savedToggleSprint);
        } else if (binding == Binding.SNEAK) {
            if (enabled) savedToggleSneak = mc.options.toggleCrouch().get();
            mc.options.toggleCrouch().set(enabled || savedToggleSneak);
        }
    }

    @Override
    public boolean toggleLatched(Binding binding) {
        if (binding == Binding.SPRINT) return mc.options.toggleSprint().get() && mc.options.keySprint.isDown();
        if (binding == Binding.SNEAK) return mc.options.toggleCrouch().get() && mc.options.keyShift.isDown();
        return false;
    }

    @Override
    public void setSmoothCamera(boolean on) {
        mc.options.smoothCamera = on;
    }

    @Override
    public void setPerspective(int perspective) {
        CameraType[] types = CameraType.values();
        mc.options.setCameraType(types[Math.max(0, Math.min(types.length - 1, perspective))]);
    }

    @Override
    public long dayTime() {
        if (mc.level == null) return -1;
        //? if >=26.1 {
        /*return mc.level.getOverworldClockTime(); // 26.1 world clocks; day time was always the overworld's
        *///?} else {
        return mc.level.getDayTime();
        //?}
    }

    @Override
    public Path screenshotsDir() {
        return mc.gameDirectory.toPath().resolve("screenshots");
    }

    @Override
    public void debugIncomingChat(String text) {
        FabricCompat.serverMessage(mc, Component.literal(text));
    }

    @Override
    public void playPing() {
        mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_PLING, 1.5f));
    }

    // ------------------------------------------------------------------ FPS Boost / graphics API

    /** Sets one option from its text value (skipped if unchanged or unparseable), recording the old value in {@code backup}. */
    private <T> void set(Map<String, String> backup, Map<String, String> want, String id, net.minecraft.client.OptionInstance<T> opt,
                         java.util.function.Function<String, T> parse) {
        String w = want.get(id);
        if (w == null) return;
        T value;
        try {
            value = parse.apply(w);
        } catch (RuntimeException e) {
            log.warn("video option {}={} not understood: {}", id, w, e.toString());
            return;
        }
        T old = opt.get();
        if (value.equals(old)) return;
        backup.put(id, old instanceof Enum ? ((Enum<?>) old).name() : String.valueOf(old));
        opt.set(value); // vanilla's own update hooks run (chunk rebuild, texture reload for mipmaps, ...)
    }

    @Override
    public Map<String, String> applyVideo(Map<String, String> v) {
        net.minecraft.client.Options o = mc.options;
        Map<String, String> b = new java.util.LinkedHashMap<String, String>();
        set(b, v, "renderDistance", o.renderDistance(), Integer::valueOf);
        set(b, v, "simulationDistance", o.simulationDistance(), Integer::valueOf);
        set(b, v, "entityDistance", o.entityDistanceScaling(), Double::valueOf);
        set(b, v, "particles", o.particles(), ParticleStatus::valueOf);
        set(b, v, "clouds", o.cloudStatus(), net.minecraft.client.CloudStatus::valueOf);
        set(b, v, "smoothLighting", o.ambientOcclusion(), Boolean::valueOf);
        set(b, v, "biomeBlend", o.biomeBlendRadius(), Integer::valueOf);
        set(b, v, "entityShadows", o.entityShadows(), Boolean::valueOf);
        set(b, v, "mipmaps", o.mipmapLevels(), Integer::valueOf);
        set(b, v, "menuBlur", o.menuBackgroundBlurriness(), Integer::valueOf);
        set(b, v, "chunkUpdates", o.prioritizeChunkUpdates(), net.minecraft.client.PrioritizeChunkUpdates::valueOf);
        set(b, v, "vsync", o.enableVsync(), Boolean::valueOf);
        set(b, v, "maxFps", o.framerateLimit(), Integer::valueOf);
        //? if >=1.21.6
        set(b, v, "cloudRange", o.cloudRange(), Integer::valueOf);
        //? if >=1.21.11 {
        set(b, v, "cutoutLeaves", o.cutoutLeaves(), Boolean::valueOf);
        set(b, v, "improvedTransparency", o.improvedTransparency(), Boolean::valueOf);
        set(b, v, "vignette", o.vignette(), Boolean::valueOf);
        set(b, v, "weatherRadius", o.weatherRadius(), Integer::valueOf);
        //?} else {
        /*set(b, v, "graphics", o.graphicsMode(), net.minecraft.client.GraphicsStatus::valueOf);
        *///?}
        o.save();
        return b;
    }

    @Override
    public void restoreOptions(Map<String, String> previous) {
        applyVideo(previous);
    }

    @Override
    public int videoOption(String id) {
        if ("renderDistance".equals(id)) return mc.options.renderDistance().get();
        if ("simulationDistance".equals(id)) return mc.options.simulationDistance().get();
        return -1;
    }

    @Override
    public String gpuName() {
        try {
            //? if >=26.2 {
            /*return com.mojang.blaze3d.systems.RenderSystem.getDevice().getDeviceInfo().name();
            *///?} elif >=1.21.5 {
            return com.mojang.blaze3d.systems.RenderSystem.getDevice().getRenderer();
            //?} else {
            /*return com.mojang.blaze3d.platform.GlUtil.getRenderer();
            *///?}
        } catch (RuntimeException e) {
            return "";
        }
    }

    @Override
    public String gpuKind() {
        try {
            //? if >=26.2 {
            /*String t = com.mojang.blaze3d.systems.RenderSystem.getDevice().getDeviceInfo().type().name().toLowerCase(java.util.Locale.ROOT);
            return t.equals("other") ? "" : t;
            *///?} else {
            return "";
            //?}
        } catch (RuntimeException e) {
            return "";
        }
    }

    @Override
    public String graphicsApi() {
        //? if >=26.2 {
        /*return mc.options.preferredGraphicsBackend().get().getSerializedName();
        *///?} else {
        // The bundled VulkanMod (RendererSwitch); a separately installed one is the player's to manage.
        return RendererSwitch.bundled ? (RendererSwitch.vulkanNext() ? "vulkan" : "opengl") : null;
        //?}
    }

    @Override
    public String rendererStatus() {
        return RendererSwitch.status();
    }

    @Override
    public String bundledNotice() {
        if (!RendererSwitch.bundled || "none".equals(RendererSwitch.state)) return "";
        return "Includes VulkanMod " + RendererSwitch.version + " by Collateral, under the GNU LGPL 3.0 (source: github.com/xCollateral/VulkanMod)."
                + " The licence texts and the exact source link are in the MW19 jar: THIRD_PARTY_NOTICES.txt, META-INF/licenses.";
    }

    @Override
    public void setGraphicsApi(String api) {
        //? if >=26.2 {
        /*for (net.minecraft.client.PreferredGraphicsApi a : net.minecraft.client.PreferredGraphicsApi.values()) {
            if (a.getSerializedName().equals(api)) mc.options.preferredGraphicsBackend().set(a);
        }
        mc.options.save();
        *///?} else {
        if (RendererSwitch.bundled) RendererSwitch.choose("vulkan".equals(api) ? "vulkan" : "opengl");
        //?}
    }

    // ------------------------------------------------------------------ ScreenHost

    @Override
    public void openGui() {
        Screen s = FabricCompat.screen(mc);
        if (!(s instanceof Mw19Screen) || ((Mw19Screen) s).isHome()) FabricCompat.setScreen(mc, new Mw19Screen(s));
    }

    @Override
    public void closeGui() {
        Screen s = FabricCompat.screen(mc);
        if (s instanceof Mw19Screen && !((Mw19Screen) s).isHome()) s.onClose();
    }

    @Override
    public Kind current() {
        Screen s = FabricCompat.screen(mc);
        if (s == null) return Kind.NONE;
        if (s instanceof Mw19Screen) return ((Mw19Screen) s).isHome() ? Kind.TITLE : Kind.OURS;
        if (s instanceof TitleScreen) return Kind.TITLE;
        if (s instanceof PauseScreen) return Kind.PAUSE;
        if (s instanceof ChatScreen) return Kind.CHAT;
        if (s instanceof AbstractContainerScreen) return Kind.INVENTORY;
        return Kind.OTHER;
    }

    @Override
    public void openSingleplayer() {
        FabricCompat.setScreen(mc, new net.minecraft.client.gui.screens.worldselection.SelectWorldScreen(FabricCompat.screen(mc)));
    }

    /** Same as vanilla's title screen: the online-play safety notice until the player has accepted it. */
    @Override
    public void openMultiplayer() {
        Screen parent = FabricCompat.screen(mc);
        FabricCompat.setScreen(mc, mc.options.skipMultiplayerWarning
                ? new net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen(parent)
                : new net.minecraft.client.gui.screens.multiplayer.SafetyScreen(parent));
    }

    @Override
    public void openOptions() {
        Screen parent = FabricCompat.screen(mc);
        //? if >=26.1 && <26.3 {
        /*FabricCompat.setScreen(mc, new net.minecraft.client.gui.screens.options.OptionsScreen(parent, mc.options, false));
        *///?} else {
        FabricCompat.setScreen(mc, new net.minecraft.client.gui.screens.options.OptionsScreen(parent, mc.options));
        //?}
    }

    private static final String MOD_MENU_SCREEN = "com.terraformersmc.modmenu.gui.ModsScreen";

    @Override
    public boolean hasModList() {
        return FabricLoader.getInstance().isModLoaded("modmenu");
    }

    /** Mod Menu's screen by reflection (no compile dependency); falls back to vanilla's title screen, which has its button. */
    @Override
    public void openModList() {
        Screen parent = FabricCompat.screen(mc);
        try {
            Screen s = (Screen) Class.forName(MOD_MENU_SCREEN).getConstructor(Screen.class).newInstance(parent);
            FabricCompat.setScreen(mc, s);
        } catch (ReflectiveOperationException | ClassCastException | LinkageError e) {
            log.warn("Mod Menu screen unavailable ({}), showing the vanilla title screen", e.toString());
            openVanillaTitle();
        }
    }

    @Override
    public void openVanillaTitle() {
        Mw19Fabric.vanillaTitleOnce = true;
        FabricCompat.setScreen(mc, new TitleScreen());
    }

    @Override
    public void openPause() {
        mc.pauseGame(false);
    }

    @Override
    public void closeScreen() {
        FabricCompat.setScreen(mc, null);
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
        FabricCompat.localMessage(mc, Component.literal(formatted));
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
