package dev.mw19.forge;

import dev.mw19.core.Hooks;
import dev.mw19.core.Keys;
import dev.mw19.core.chat.ChatLine;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.IChatComponent;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.EntityViewRenderEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import dev.mw19.core.Mw19;
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

@Mod(modid = "mw19", name = "MW19", version = Mw19Forge.VERSION, clientSideOnly = true, acceptedMinecraftVersions = "[1.8.9]")
public final class Mw19Forge {
    public static final String VERSION = "0.1.0+mc1.8.9";
    private static final int BUTTON_ID = 0x4B53; // "KS"
    private static Handlers hooks;

    static Handlers hooks() {
        return hooks;
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        hooks = new Handlers(new ForgePlatform());
        hooks.platform().refreshResolution();
        Mw19.init(hooks.platform(), VERSION);
        MinecraftForge.EVENT_BUS.register(hooks);
        FMLCommonHandler.instance().bus().register(hooks);
        FMLCommonHandler.instance().registerCrashCallable(new ICrashCallable() {
            @Override
            public String getLabel() {
                return "MW19";
            }

            @Override
            public String call() {
                return Mw19.crashReportDetails();
            }
        });
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                Mw19.onShutdown();
            }
        }, "MW19-Shutdown"));
    }

    /** Forge event handlers; each forwards to core in one line. */
    public static final class Handlers {
        private final ForgePlatform platform;
        private final ForgeBackend hud = new ForgeBackend();

        Handlers(ForgePlatform platform) {
            this.platform = platform;
        }

        ForgePlatform platform() {
            return platform;
        }

        @SubscribeEvent
        public void onTick(TickEvent.ClientTickEvent e) {
            if (e.phase == TickEvent.Phase.START) {
                platform.refreshResolution();
                platform.tick();
                Mw19.onTick(false);
            } else {
                Mw19.onTick(true);
            }
        }

        @SubscribeEvent
        public void onOverlay(RenderGameOverlayEvent.Post e) {
            if (e.type != RenderGameOverlayEvent.ElementType.ALL) return;
            platform.refreshResolution(e.resolution);
            Mw19.onHudRender(hud.bind(e.resolution.getScaleFactor()), e.resolution.getScaledWidth(), e.resolution.getScaledHeight());
        }

        @SubscribeEvent
        public void onOverlayPre(RenderGameOverlayEvent.Pre e) {
            if (e.type == RenderGameOverlayEvent.ElementType.CROSSHAIRS && Hooks.hideCrosshair) e.setCanceled(true);
        }

        @SubscribeEvent
        public void onFov(EntityViewRenderEvent.FOVModifier e) {
            float m = Hooks.fov();
            if (m != 1f) e.setFOV(e.getFOV() * m);
        }

        @SubscribeEvent
        public void onAttack(AttackEntityEvent e) {
            if (e.entityPlayer == net.minecraft.client.Minecraft.getMinecraft().thePlayer && e.target != null) Mw19.onAttack(e.target.getEntityId());
        }

        @SubscribeEvent
        public void onChat(ClientChatReceivedEvent e) {
            if (e.type == 2 || e.message == null) return; // action bar
            ChatLine line = chat.reset(e.message.getUnformattedText(), e.message.getFormattedText());
            Mw19.onChat(line);
            if (line.cancel) {
                e.setCanceled(true);
                return;
            }
            if (!line.modified() && !line.track) return;
            // Print ourselves with a per-text line id so "(xN)" stacking can delete the previous copy.
            e.setCanceled(true);
            IChatComponent out = new ChatComponentText("");
            if (line.highlight != 0) out.appendSibling(new ChatComponentText("§" + nearest(line.highlight) + "▍§r"));
            if (line.prefix != null) out.appendSibling(new ChatComponentText(line.prefix));
            out.appendSibling(e.message);
            if (line.repeatSuffix() != null) out.appendSibling(new ChatComponentText(line.repeatSuffix()));
            int id = line.plain.hashCode() | 0x40000000; // never 0 (0 = "no id")
            net.minecraft.client.Minecraft.getMinecraft().ingameGUI.getChatGUI().printChatMessageWithOptionalDeletion(out, id);
        }

        private final ChatLine chat = new ChatLine();

        /** Closest legacy colour code for an ARGB colour (1.8.9 chat has no RGB). */
        private static char nearest(int argb) {
            int[] rgb = {0x000000, 0x0000AA, 0x00AA00, 0x00AAAA, 0xAA0000, 0xAA00AA, 0xFFAA00, 0xAAAAAA,
                    0x555555, 0x5555FF, 0x55FF55, 0x55FFFF, 0xFF5555, 0xFF55FF, 0xFFFF55, 0xFFFFFF};
            int best = 15;
            long bestD = Long.MAX_VALUE;
            for (int i = 0; i < 16; i++) {
                long dr = ((argb >> 16) & 255) - ((rgb[i] >> 16) & 255), dg = ((argb >> 8) & 255) - ((rgb[i] >> 8) & 255), db = (argb & 255) - (rgb[i] & 255);
                long d = dr * dr + dg * dg + db * db;
                if (d < bestD) {
                    bestD = d;
                    best = i;
                }
            }
            return "0123456789abcdef".charAt(best);
        }

        @SubscribeEvent
        public void onKey(InputEvent.KeyInputEvent e) {
            int code = Keyboard.getEventKey();
            if (code == 0) return;
            int action = Keyboard.getEventKeyState() ? (Keyboard.isRepeatEvent() ? Keys.ACTION_REPEAT : Keys.ACTION_PRESS) : Keys.ACTION_RELEASE;
            Mw19.onKey(LwjglKeys.toGlfw(code), action, 0);
        }

        @SubscribeEvent
        public void onMouse(MouseEvent e) {
            if (e.button >= 0) Mw19.onMouseButton(e.button, e.buttonstate ? Keys.ACTION_PRESS : Keys.ACTION_RELEASE);
            if (e.dwheel != 0 && Mw19.onScroll(e.dwheel > 0 ? 1 : -1)) e.setCanceled(true);
        }

        @SubscribeEvent
        public void onGuiInit(GuiScreenEvent.InitGuiEvent.Post e) {
            if ((e.gui instanceof GuiMainMenu || e.gui instanceof GuiIngameMenu) && Mw19.wantMenuButton()) {
                e.buttonList.add(new GuiButton(BUTTON_ID, 6, 6, 20, 20, "K"));
            }
        }

        @SubscribeEvent
        public void onButton(GuiScreenEvent.ActionPerformedEvent.Pre e) {
            if (e.button.id == BUTTON_ID && (e.gui instanceof GuiMainMenu || e.gui instanceof GuiIngameMenu)) {
                Mw19.get().openGui();
                e.setCanceled(true);
            }
        }
    }
}
