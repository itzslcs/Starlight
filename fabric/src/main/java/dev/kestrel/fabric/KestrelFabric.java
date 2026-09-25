package dev.kestrel.fabric;

import dev.kestrel.core.Kestrel;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

/** Entrypoint + the static bridge the mixins call (keeps mixin bodies one line). */
public final class KestrelFabric implements ClientModInitializer {
    private static FabricPlatform platform;
    private static final FabricBackend HUD = new FabricBackend();

    @Override
    public void onInitializeClient() {
        String version = FabricLoader.getInstance().getModContainer("kestrel")
                .map(c -> c.getMetadata().getVersion().getFriendlyString()).orElse("dev");
        platform = new FabricPlatform();
        Kestrel.init(platform, version);
        Runtime.getRuntime().addShutdownHook(new Thread(Kestrel::onShutdown, "Kestrel-Shutdown"));
    }

    public static void tick(boolean end) {
        if (platform == null) return;
        if (!end) platform.tick();
        Kestrel.onTick(end);
    }

    public static void hud(GuiGraphics g) {
        Minecraft mc = Minecraft.getInstance();
        Kestrel.onHudRender(HUD.bind(g), mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight());
    }
}
