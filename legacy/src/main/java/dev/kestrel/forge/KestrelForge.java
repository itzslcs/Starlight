package dev.kestrel.forge;

import dev.kestrel.core.Keys;
import dev.kestrel.core.Kestrel;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiIngameMenu;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.ICrashCallable;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;

@Mod(modid = "kestrel", name = "Kestrel", version = KestrelForge.VERSION, clientSideOnly = true, acceptedMinecraftVersions = "[1.8.9]")
public final class KestrelForge {
    public static final String VERSION = "0.1.0+mc1.8.9";
    private static final int BUTTON_ID = 0x4B53; // "KS"
    private static Hooks hooks;

    static Hooks hooks() {
        return hooks;
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        hooks = new Hooks(new ForgePlatform());
        hooks.platform().refreshResolution();
        Kestrel.init(hooks.platform(), VERSION);
        MinecraftForge.EVENT_BUS.register(hooks);
        FMLCommonHandler.instance().bus().register(hooks);
        FMLCommonHandler.instance().registerCrashCallable(new ICrashCallable() {
            @Override
            public String getLabel() {
                return "Kestrel";
            }

            @Override
            public String call() {
                return Kestrel.crashReportDetails();
            }
        });
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                Kestrel.onShutdown();
            }
        }, "Kestrel-Shutdown"));
    }

    /** Forge event handlers; each forwards to core in one line. */
    public static final class Hooks {
        private final ForgePlatform platform;
        private final ForgeBackend hud = new ForgeBackend();

        Hooks(ForgePlatform platform) {
            this.platform = platform;
        }

        ForgePlatform platform() {
            return platform;
        }

        @SubscribeEvent
        public void onTick(TickEvent.ClientTickEvent e) {
            if (e.phase == TickEvent.Phase.START) {
                platform.refreshResolution();
                platform.pollServer();
                Kestrel.onTick(false);
            } else {
                Kestrel.onTick(true);
            }
        }

        @SubscribeEvent
        public void onOverlay(RenderGameOverlayEvent.Post e) {
            if (e.type != RenderGameOverlayEvent.ElementType.ALL) return;
            platform.refreshResolution(e.resolution);
            Kestrel.onHudRender(hud.bind(e.resolution.getScaleFactor()), e.resolution.getScaledWidth(), e.resolution.getScaledHeight());
        }

        @SubscribeEvent
        public void onKey(InputEvent.KeyInputEvent e) {
            int code = Keyboard.getEventKey();
            if (code == 0) return;
            int action = Keyboard.getEventKeyState() ? (Keyboard.isRepeatEvent() ? Keys.ACTION_REPEAT : Keys.ACTION_PRESS) : Keys.ACTION_RELEASE;
            Kestrel.onKey(LwjglKeys.toGlfw(code), action, 0);
        }

        @SubscribeEvent
        public void onMouse(MouseEvent e) {
            if (e.button >= 0) Kestrel.onMouseButton(e.button, e.buttonstate ? Keys.ACTION_PRESS : Keys.ACTION_RELEASE);
        }

        @SubscribeEvent
        public void onGuiInit(GuiScreenEvent.InitGuiEvent.Post e) {
            if ((e.gui instanceof GuiMainMenu || e.gui instanceof GuiIngameMenu) && Kestrel.wantMenuButton()) {
                e.buttonList.add(new GuiButton(BUTTON_ID, 6, 6, 20, 20, "K"));
            }
        }

        @SubscribeEvent
        public void onButton(GuiScreenEvent.ActionPerformedEvent.Pre e) {
            if (e.button.id == BUTTON_ID && (e.gui instanceof GuiMainMenu || e.gui instanceof GuiIngameMenu)) {
                Kestrel.get().openGui();
                e.setCanceled(true);
            }
        }
    }
}
