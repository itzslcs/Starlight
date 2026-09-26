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
