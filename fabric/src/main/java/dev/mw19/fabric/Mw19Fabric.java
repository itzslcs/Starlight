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

    //? if >=26.1 {
    /*public static void hud(GuiGraphicsExtractor g) {
    *///?} else {
    public static void hud(GuiGraphics g) {
    //?}
        Minecraft mc = Minecraft.getInstance();
        Mw19.onHudRender(HUD.bind(g), mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight());
    }
}
