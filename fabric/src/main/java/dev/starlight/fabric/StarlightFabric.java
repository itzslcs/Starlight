package dev.starlight.fabric;

import dev.starlight.core.Starlight;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
//? if >=26.1 {
/*import net.minecraft.client.gui.GuiGraphicsExtractor;
*///?} else {
import net.minecraft.client.gui.GuiGraphics;
//?}

/** Entrypoint + the static bridge the mixins call (keeps mixin bodies one line). */
public final class StarlightFabric implements ClientModInitializer {
    private static FabricPlatform platform;
    private static final FabricBackend HUD = new FabricBackend();

    @Override
    public void onInitializeClient() {
        String version = FabricLoader.getInstance().getModContainer("starlight_client")
                .map(c -> c.getMetadata().getVersion().getFriendlyString()).orElse("dev");
        platform = new FabricPlatform();
        Starlight.init(platform, version);
        Runtime.getRuntime().addShutdownHook(new Thread(Starlight::onShutdown, "Starlight-Shutdown"));
    }

    public static void tick(boolean end) {
        if (platform == null) return;
        if (!end) platform.tick();
        Starlight.onTick(end);
    }

    private static final net.minecraft.core.BlockPos.MutableBlockPos PROBE = new net.minecraft.core.BlockPos.MutableBlockPos();
    private static final dev.starlight.core.perf.Occlusion.Blocks BLOCKS = (x, y, z) -> {
        net.minecraft.client.multiplayer.ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return false;
        net.minecraft.world.level.block.state.BlockState s = level.getBlockState(PROBE.set(x, y, z));
        //? if >=1.21.2 {
        return s.isSolidRender();
        //?} else {
        /*return s.isSolidRender(level, PROBE);
        *///?}
    };

    /** EntityCullingMixin: false when the entity is fully hidden behind blocks (module on). Fails open. */
    public static boolean drawEntity(net.minecraft.world.entity.Entity e, double cx, double cy, double cz) {
        if (!dev.starlight.core.Hooks.entityCulling) return true;
        try {
            if (e == Minecraft.getInstance().getCameraEntity() || e instanceof net.minecraft.world.entity.player.Player
                    || e.isCurrentlyGlowing() || e.shouldShowName()) return true;
            Starlight k = Starlight.get();
            if (k == null) return true;
            net.minecraft.world.phys.AABB b = e.getBoundingBox();
            return k.occlusion.visible(BLOCKS, e.getId(), b.minX, b.minY, b.minZ, b.maxX, b.maxY, b.maxZ, cx, cy, cz,
                    System.currentTimeMillis());
        } catch (RuntimeException ex) {
            dev.starlight.core.Hooks.entityCulling = false; // stop culling rather than risk the render loop
            dev.starlight.core.Log.error("entity culling disabled after an error", ex);
            return true;
        }
    }

    /**
     * BlockEntityCullingMixin: false when the block entity is fully hidden behind blocks (module on). Renderers the game
     * draws even off screen (beacon beams, end gateways) are never skipped. Fails open.
     */
    public static boolean drawBlockEntity(net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher dispatcher,
                                          net.minecraft.world.level.block.entity.BlockEntity be) {
        if (!dev.starlight.core.Hooks.blockEntityCulling) return true;
        try {
            Starlight k = Starlight.get();
            if (k == null) return true;
            var renderer = dispatcher.getRenderer(be);
            if (renderer == null) return true;
            //? if >=1.21.6 {
            if (renderer.shouldRenderOffScreen()) return true;
            //?} else {
            /*if (renderer.shouldRenderOffScreen(be)) return true;
            *///?}
            Minecraft mc = Minecraft.getInstance();
            //? if >=26.2 {
            /*net.minecraft.world.phys.Vec3 cam = mc.gameRenderer.mainCamera().position();
            *///?} elif >=1.21.6 {
            net.minecraft.world.phys.Vec3 cam = mc.gameRenderer.getMainCamera().position();
            //?} else {
            /*net.minecraft.world.phys.Vec3 cam = mc.gameRenderer.getMainCamera().getPosition();
            *///?}
            net.minecraft.core.BlockPos p = be.getBlockPos();
            long key = p.asLong();
            return k.blockOcclusion.visible(BLOCKS, (int) (key ^ (key >>> 32)), p.getX() - 1, p.getY() - 1, p.getZ() - 1,
                    p.getX() + 2, p.getY() + 2, p.getZ() + 2, cam.x, cam.y, cam.z, System.currentTimeMillis());
        } catch (RuntimeException ex) {
            dev.starlight.core.Hooks.blockEntityCulling = false; // stop culling rather than risk the render loop
            dev.starlight.core.Log.error("block entity culling disabled after an error", ex);
            return true;
        }
    }

    /** PackSourceMixin: Starlight's built-in packs join the client's pack scan (Fast Chests). */
    public static void clientPacks(java.util.function.Consumer<net.minecraft.server.packs.repository.Pack> out) {
        FastChests.addPacks(out);
    }

    /** FastChestsMixin: true when the world mesh already draws this chest, so its block entity renderer is skipped. */
    public static boolean bakedChest(net.minecraft.world.level.block.entity.BlockEntity be) {
        return dev.starlight.core.Hooks.fastChests && FastChests.baked(be.getBlockState().getBlock());
    }

    /** SpecialChestMixin: the same for a chest drawn as a block outside the world mesh (chest minecart, block display). */
    public static boolean bakedChest(net.minecraft.world.level.block.Block block) {
        return dev.starlight.core.Hooks.fastChests && FastChests.baked(block);
    }

    private static final FabricBackend MENU = new FabricBackend();

    /** Hitboxes module on (1.21.9+: HitboxesMixin reports the debug entry as enabled instead of changing it). */
    public static volatile boolean hitboxes;

    /** ButtonStyleMixin: a vanilla button's background in the Starlight style; false = vanilla draws its own. */
    //? if >=26.1 {
    /*public static boolean button(GuiGraphicsExtractor g, int x, int y, int w, int h, boolean hovered, boolean active, float alpha) {
    *///?} else {
    public static boolean button(GuiGraphics g, int x, int y, int w, int h, boolean hovered, boolean active, float alpha) {
    //?}
        return Starlight.vanillaButton(MENU.bind(g), x, y, w, h, hovered, active, alpha);
    }

    /** SliderStyleMixin: a vanilla slider's track and handle in the Starlight style; false = vanilla draws its own. */
    //? if >=26.1 {
    /*public static boolean slider(GuiGraphicsExtractor g, int x, int y, int w, int h, float value, boolean hovered, boolean active, float alpha) {
    *///?} else {
    public static boolean slider(GuiGraphics g, int x, int y, int w, int h, float value, boolean hovered, boolean active, float alpha) {
    //?}
        return Starlight.vanillaSlider(MENU.bind(g), x, y, w, h, value, hovered, active, alpha);
    }

    /**
     * ScreenBackdropMixin (and PauseScreenMixin from 26.2): the Starlight backdrop behind a vanilla screen; false =
     * vanilla's background. Inventories, chat and the death screen keep vanilla's: there the world is the point. So do
     * the screens 26.2 made see-through over the world (isInGameUi: signs, books, command blocks). From 26.2 vanilla's
     * background ends with the HUD's deferred subtitles, so a replaced background draws them too.
     */
    //? if >=26.1 {
    /*public static boolean backdrop(net.minecraft.client.gui.screens.Screen s, GuiGraphicsExtractor g) {
    *///?} else {
    public static boolean backdrop(net.minecraft.client.gui.screens.Screen s, GuiGraphics g) {
    //?}
        if (s instanceof net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?> || s instanceof net.minecraft.client.gui.screens.ChatScreen
                || s instanceof net.minecraft.client.gui.screens.DeathScreen) return false;
        //? if >=26.2 {
        /*if (s.isInGameUi() || !Starlight.menuBackdrop(MENU.bind(g), s.width, s.height, Minecraft.getInstance().level != null)) return false;
        Minecraft.getInstance().gui.hud.extractDeferredSubtitles();
        return true;
        *///?} else {
        return Starlight.menuBackdrop(MENU.bind(g), s.width, s.height, Minecraft.getInstance().level != null);
        //?}
    }

    /**
     * PauseScreenMixin: [Starlight Menu][Packs] under "Back to Game" (the topmost wide button); everything below moves down a
     * row. Nothing is added to the menu-less pause (F3+Esc) or a layout without such a button.
     */
    /** The topmost wide button ("Back to Game"). Buttons only: up to 1.21.8 the "Game Menu" title is a screen-wide
     *  text widget among the children, and the row went under it (debug-log 2026-09-27). */
    static net.minecraft.client.gui.components.AbstractWidget pauseAnchor(net.minecraft.client.gui.screens.Screen screen) {
        net.minecraft.client.gui.components.AbstractWidget back = null;
        for (net.minecraft.client.gui.components.events.GuiEventListener l : screen.children()) {
            if (l instanceof net.minecraft.client.gui.components.AbstractButton w && w.visible && w.getWidth() >= 150
                    && !isPauseRow(w) && (back == null || w.getY() < back.getY())) back = w;
        }
        return back;
    }

    static boolean isPauseRow(net.minecraft.client.gui.components.AbstractWidget w) {
        String s = w.getMessage().getString();
        return "Starlight Menu".equals(s) || "Packs".equals(s);
    }

    public static void pauseRow(net.minecraft.client.gui.screens.Screen screen, java.util.function.Consumer<net.minecraft.client.gui.components.Button> add) {
        net.minecraft.client.gui.components.AbstractWidget back = pauseAnchor(screen);
        if (back == null) return;
        int row = back.getY() + back.getHeight() + 4, half = (back.getWidth() - 4) / 2;
        for (net.minecraft.client.gui.components.events.GuiEventListener l : screen.children()) {
            if (l instanceof net.minecraft.client.gui.components.AbstractWidget w && w != back && w.getY() >= row - 2) w.setY(w.getY() + 24);
        }
        add.accept(net.minecraft.client.gui.components.Button.builder(net.minecraft.network.chat.Component.literal("Starlight Menu"),
                b -> Starlight.openFromPause(false)).bounds(back.getX(), row, half, 20).build());
        add.accept(net.minecraft.client.gui.components.Button.builder(net.minecraft.network.chat.Component.literal("Packs"),
                b -> Starlight.openFromPause(true)).bounds(back.getX() + back.getWidth() - half, row, half, 20).build());
    }

    /** Set by "Vanilla menu": the next title screen is Minecraft's own. */
    public static boolean vanillaTitleOnce;

    /** TitleSwapMixin: vanilla's title screen becomes the Starlight home screen as it is shown (setting on). */
    public static net.minecraft.client.gui.screens.Screen home(net.minecraft.client.gui.screens.Screen screen) {
        if (screen == null || screen.getClass() != net.minecraft.client.gui.screens.TitleScreen.class) return screen;
        if (vanillaTitleOnce) {
            vanillaTitleOnce = false;
            return screen;
        }
        try {
            Starlight k = Starlight.get();
            return k == null || !k.client.customTitle.on() ? screen : new StarlightScreen(null, k.home());
        } catch (VirtualMachineError e) {
            throw e;
        } catch (Throwable t) {
            dev.starlight.core.Log.error("home screen unavailable, keeping the vanilla title", t);
            return screen;
        }
    }

    //? if >=26.1 {
    /*public static void hud(GuiGraphicsExtractor g) {
    *///?} else {
    public static void hud(GuiGraphics g) {
    //?}
        Minecraft mc = Minecraft.getInstance();
        Starlight.onHudRender(HUD.bind(g), mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight());
    }
}
