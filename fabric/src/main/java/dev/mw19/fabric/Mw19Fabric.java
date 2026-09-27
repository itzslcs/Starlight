package dev.mw19.fabric;

import dev.mw19.core.Mw19;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
//? if >=26.1 {
/*import net.minecraft.client.gui.GuiGraphicsExtractor;
*///?} else {
import net.minecraft.client.gui.GuiGraphics;
//?}

/** Entrypoint + the static bridge the mixins call (keeps mixin bodies one line). */
public final class Mw19Fabric implements ClientModInitializer {
    private static FabricPlatform platform;
    private static final FabricBackend HUD = new FabricBackend();

    @Override
    public void onInitializeClient() {
        String version = FabricLoader.getInstance().getModContainer("mw19")
                .map(c -> c.getMetadata().getVersion().getFriendlyString()).orElse("dev");
        platform = new FabricPlatform();
        Mw19.init(platform, version);
        Runtime.getRuntime().addShutdownHook(new Thread(Mw19::onShutdown, "MW19-Shutdown"));
    }

    public static void tick(boolean end) {
        if (platform == null) return;
        if (!end) platform.tick();
        Mw19.onTick(end);
    }

    private static final net.minecraft.core.BlockPos.MutableBlockPos PROBE = new net.minecraft.core.BlockPos.MutableBlockPos();
    private static final dev.mw19.core.perf.Occlusion.Blocks BLOCKS = (x, y, z) -> {
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
        if (!dev.mw19.core.Hooks.entityCulling) return true;
        try {
            if (e == Minecraft.getInstance().getCameraEntity() || e instanceof net.minecraft.world.entity.player.Player
                    || e.isCurrentlyGlowing() || e.shouldShowName()) return true;
            Mw19 k = Mw19.get();
            if (k == null) return true;
            net.minecraft.world.phys.AABB b = e.getBoundingBox();
            return k.occlusion.visible(BLOCKS, e.getId(), b.minX, b.minY, b.minZ, b.maxX, b.maxY, b.maxZ, cx, cy, cz,
                    System.currentTimeMillis());
        } catch (RuntimeException ex) {
            dev.mw19.core.Hooks.entityCulling = false; // stop culling rather than risk the render loop
            dev.mw19.core.Log.error("entity culling disabled after an error", ex);
            return true;
        }
    }

    /**
     * BlockEntityCullingMixin: false when the block entity is fully hidden behind blocks (module on). Renderers the game
     * draws even off screen (beacon beams, end gateways) are never skipped. Fails open.
     */
    public static boolean drawBlockEntity(net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher dispatcher,
                                          net.minecraft.world.level.block.entity.BlockEntity be) {
        if (!dev.mw19.core.Hooks.blockEntityCulling) return true;
        try {
            Mw19 k = Mw19.get();
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
            dev.mw19.core.Hooks.blockEntityCulling = false; // stop culling rather than risk the render loop
            dev.mw19.core.Log.error("block entity culling disabled after an error", ex);
            return true;
        }
    }

    /** PackSourceMixin: MW19's built-in packs join the client's pack scan (Fast Chests). */
    public static void clientPacks(java.util.function.Consumer<net.minecraft.server.packs.repository.Pack> out) {
        FastChests.addPacks(out);
    }

    /** FastChestsMixin: true when the world mesh already draws this chest, so its block entity renderer is skipped. */
    public static boolean bakedChest(net.minecraft.world.level.block.entity.BlockEntity be) {
        return dev.mw19.core.Hooks.fastChests && FastChests.baked(be.getBlockState().getBlock());
    }

    /** SpecialChestMixin: the same for a chest drawn as a block outside the world mesh (chest minecart, block display). */
    public static boolean bakedChest(net.minecraft.world.level.block.Block block) {
        return dev.mw19.core.Hooks.fastChests && FastChests.baked(block);
    }

    private static final FabricBackend MENU = new FabricBackend();

    /** Hitboxes module on (1.21.9+: HitboxesMixin reports the debug entry as enabled instead of changing it). */
    public static volatile boolean hitboxes;

    /** ButtonStyleMixin: a vanilla button's background in the MW19 style; false = vanilla draws its own. */
    //? if >=26.1 {
    /*public static boolean button(GuiGraphicsExtractor g, int x, int y, int w, int h, boolean hovered, boolean active, float alpha) {
    *///?} else {
    public static boolean button(GuiGraphics g, int x, int y, int w, int h, boolean hovered, boolean active, float alpha) {
    //?}
        return Mw19.vanillaButton(MENU.bind(g), x, y, w, h, hovered, active, alpha);
    }

    /**
     * PauseScreenMixin: [MW19 Menu][Packs] under "Back to Game" (the topmost wide button); everything below moves down a
     * row. Nothing is added to the menu-less pause (F3+Esc) or a layout without such a button.
     */
    public static void pauseRow(net.minecraft.client.gui.screens.Screen screen, java.util.function.Consumer<net.minecraft.client.gui.components.Button> add) {
        net.minecraft.client.gui.components.AbstractWidget back = null;
        for (net.minecraft.client.gui.components.events.GuiEventListener l : screen.children()) {
            if (l instanceof net.minecraft.client.gui.components.AbstractWidget w && w.visible && w.getWidth() >= 150
                    && (back == null || w.getY() < back.getY())) back = w;
        }
        if (back == null) return;
        int row = back.getY() + back.getHeight() + 4, half = (back.getWidth() - 4) / 2;
        for (net.minecraft.client.gui.components.events.GuiEventListener l : screen.children()) {
            if (l instanceof net.minecraft.client.gui.components.AbstractWidget w && w != back && w.getY() >= row - 2) w.setY(w.getY() + 24);
        }
        add.accept(net.minecraft.client.gui.components.Button.builder(net.minecraft.network.chat.Component.literal("MW19 Menu"),
                b -> Mw19.openFromPause(false)).bounds(back.getX(), row, half, 20).build());
        add.accept(net.minecraft.client.gui.components.Button.builder(net.minecraft.network.chat.Component.literal("Packs"),
                b -> Mw19.openFromPause(true)).bounds(back.getX() + back.getWidth() - half, row, half, 20).build());
    }

    /** Set by "Vanilla menu": the next title screen is Minecraft's own. */
    public static boolean vanillaTitleOnce;

    /** TitleSwapMixin: vanilla's title screen becomes the MW19 home screen as it is shown (setting on). */
    public static net.minecraft.client.gui.screens.Screen home(net.minecraft.client.gui.screens.Screen screen) {
        if (screen == null || screen.getClass() != net.minecraft.client.gui.screens.TitleScreen.class) return screen;
        if (vanillaTitleOnce) {
            vanillaTitleOnce = false;
            return screen;
        }
        try {
            Mw19 k = Mw19.get();
            return k == null || !k.client.customTitle.on() ? screen : new Mw19Screen(null, k.home());
        } catch (VirtualMachineError e) {
            throw e;
        } catch (Throwable t) {
            dev.mw19.core.Log.error("home screen unavailable, keeping the vanilla title", t);
            return screen;
        }
    }

    //? if >=26.1 {
    /*public static void hud(GuiGraphicsExtractor g) {
    *///?} else {
    public static void hud(GuiGraphics g) {
    //?}
        Minecraft mc = Minecraft.getInstance();
        Mw19.onHudRender(HUD.bind(g), mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight());
    }
}
