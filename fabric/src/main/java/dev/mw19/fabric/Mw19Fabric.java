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
