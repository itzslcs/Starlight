package dev.starlight.forge;

import dev.starlight.core.Hooks;
import dev.starlight.core.Keys;
import dev.starlight.core.chat.ChatLine;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.IChatComponent;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.EntityViewRenderEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import dev.starlight.core.Starlight;
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

@Mod(modid = "starlight_client", name = "Starlight", useMetadata = true, clientSideOnly = true, acceptedMinecraftVersions = "[1.8.9]")
public final class StarlightForge {
    private static final int BUTTON_ID = 0x4B53; // "KS"
    private static final ForgeBackend MENU = new ForgeBackend();

    /** GuiButtonMixin: a vanilla button's background in the Starlight style; false = vanilla draws its own. */
    public static boolean button(int x, int y, int w, int h, boolean hovered, boolean enabled) {
        return Starlight.vanillaButton(MENU.bind(guiScale()), x, y, w, h, hovered, enabled, 1f);
    }

    /** GuiScreenMixin: the Starlight backdrop behind a vanilla screen; false = vanilla's. Inventories, chat and the death
     *  screen keep vanilla's: there the world is the point. */
    public static boolean backdrop(net.minecraft.client.gui.GuiScreen s) {
        if (s instanceof net.minecraft.client.gui.inventory.GuiContainer || s instanceof net.minecraft.client.gui.GuiChat
                || s instanceof net.minecraft.client.gui.GuiGameOver) return false;
        return Starlight.menuBackdrop(MENU.bind(guiScale()), s.width, s.height, net.minecraft.client.Minecraft.getMinecraft().theWorld != null);
    }

    /** The GUI scale factor as ScaledResolution computes it, without allocating one per button. */
    private static int guiScale() {
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getMinecraft();
        int want = mc.gameSettings.guiScale == 0 ? 1000 : mc.gameSettings.guiScale, f = 1;
        while (f < want && mc.displayWidth / (f + 1) >= 320 && mc.displayHeight / (f + 1) >= 240) f++;
        if (mc.isUnicode() && f % 2 != 0 && f != 1) f--;
        return f;
    }
    private static final net.minecraft.util.BlockPos.MutableBlockPos PROBE = new net.minecraft.util.BlockPos.MutableBlockPos();
    private static final dev.starlight.core.perf.Occlusion.Blocks BLOCKS = new dev.starlight.core.perf.Occlusion.Blocks() {
        @Override
        public boolean occludes(int x, int y, int z) {
            net.minecraft.client.multiplayer.WorldClient w = net.minecraft.client.Minecraft.getMinecraft().theWorld;
            return w != null && w.getBlockState(PROBE.set(x, y, z)).getBlock().isOpaqueCube();
        }
    };

    /** RenderManagerMixin: false when the entity is fully hidden behind blocks (Entity Culling on). Fails open. */
    /** NetHandlerPlayClientMixin: Exploit Protection looks at a server resource pack request first (true = cancel). */
    public static boolean guardResourcePack(net.minecraft.client.network.NetHandlerPlayClient handler,
                                            net.minecraft.network.play.server.S48PacketResourcePackSend packet) {
        try {
            return ForgeExploitGuard.intercept(handler, packet);
        } catch (RuntimeException e) {
            dev.starlight.core.Log.error("exploit protection: resource pack check failed, letting vanilla handle it", e);
            return false;
        }
    }

    public static boolean drawEntity(net.minecraft.entity.Entity e, double cx, double cy, double cz) {
        if (!Hooks.entityCulling) return true;
        try {
            if (e == net.minecraft.client.Minecraft.getMinecraft().getRenderViewEntity() || e instanceof net.minecraft.entity.player.EntityPlayer
                    || (e.hasCustomName() && e.getAlwaysRenderNameTagForRender())) return true;
            Starlight k = Starlight.get();
            if (k == null) return true;
            net.minecraft.util.AxisAlignedBB b = e.getEntityBoundingBox();
            // 1.8.9 hands culling the viewer's interpolated FEET position; rays must start at the camera (eyes, or behind
            // the player in third person), or a mob seen over a one-block wall counts as hidden (debug-log 2026-09-26).
            net.minecraft.util.Vec3 eye = net.minecraft.client.renderer.ActiveRenderInfo.getPosition();
            return k.occlusion.visible(BLOCKS, e.getEntityId(), b.minX, b.minY, b.minZ, b.maxX, b.maxY, b.maxZ,
                    cx + eye.xCoord, cy + eye.yCoord, cz + eye.zCoord, System.currentTimeMillis());
        } catch (RuntimeException ex) {
            Hooks.entityCulling = false; // stop culling rather than risk the render loop
            dev.starlight.core.Log.error("entity culling disabled after an error", ex);
            return true;
        }
    }

    /**
     * TileEntityRendererDispatcherMixin: false when the tile entity (chest, sign, banner, skull...) is fully hidden behind
     * blocks. Renderers that force rendering (beacon beams) are never skipped. Fails open.
     */
    public static boolean drawTileEntity(net.minecraft.tileentity.TileEntity te) {
        if (!Hooks.blockEntityCulling) return true;
        try {
            Starlight k = Starlight.get();
            if (k == null) return true;
            net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer<net.minecraft.tileentity.TileEntity> r =
                    net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher.instance.getSpecialRenderer(te);
            if (r == null || r.forceTileEntityRender()) return true;
            net.minecraft.util.BlockPos p = te.getPos();
            net.minecraft.util.Vec3 eye = net.minecraft.client.renderer.ActiveRenderInfo.getPosition();
            double cx = net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher.staticPlayerX + eye.xCoord;
            double cy = net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher.staticPlayerY + eye.yCoord;
            double cz = net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher.staticPlayerZ + eye.zCoord;
            long key = p.toLong();
            return k.blockOcclusion.visible(BLOCKS, (int) (key ^ (key >>> 32)), p.getX() - 1, p.getY() - 1, p.getZ() - 1,
                    p.getX() + 2, p.getY() + 2, p.getZ() + 2, cx, cy, cz, System.currentTimeMillis());
        } catch (RuntimeException ex) {
            Hooks.blockEntityCulling = false;
            dev.starlight.core.Log.error("block entity culling disabled after an error", ex);
            return true;
        }
    }

    /** Set by "Vanilla menu": the next main menu is Minecraft's own. */
    static boolean vanillaTitleOnce;
    private static Handlers hooks;

    static Handlers hooks() {
        return hooks;
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        hooks = new Handlers(new ForgePlatform());
        hooks.platform().refreshResolution();
        Starlight.init(hooks.platform(), net.minecraftforge.fml.common.Loader.instance().activeModContainer().getVersion());
        if (net.minecraftforge.fml.common.Loader.isModLoaded("mw19") && Starlight.get() != null) {
            // Starlight is MW19 renamed (DECISIONS D-033); Forge has no "breaks", so say it plainly instead
            dev.starlight.core.Log.warn("the old MW19 jar is installed next to Starlight: remove it from the mods folder");
            Starlight.get().toast("Two copies", "Remove the old MW19 jar from your mods folder: Starlight replaces it.", Starlight.get().theme.bad);
        }
        MinecraftForge.EVENT_BUS.register(hooks);
        FMLCommonHandler.instance().bus().register(hooks);
        FMLCommonHandler.instance().registerCrashCallable(new ICrashCallable() {
            @Override
            public String getLabel() {
                return "Starlight";
            }

            @Override
            public String call() {
                return Starlight.crashReportDetails();
            }
        });
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                Starlight.onShutdown();
            }
        }, "Starlight-Shutdown"));
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
                Starlight.onTick(false);
            } else {
                Starlight.onTick(true);
            }
        }

        @SubscribeEvent
        public void onOverlay(RenderGameOverlayEvent.Post e) {
            if (e.type != RenderGameOverlayEvent.ElementType.ALL) return;
            platform.refreshResolution(e.resolution);
            Starlight.onHudRender(hud.bind(e.resolution.getScaleFactor()), e.resolution.getScaledWidth(), e.resolution.getScaledHeight());
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
            if (e.entityPlayer == net.minecraft.client.Minecraft.getMinecraft().thePlayer && e.target != null) Starlight.onAttack(e.target.getEntityId());
        }

        @SubscribeEvent
        public void onChat(ClientChatReceivedEvent e) {
            if (e.type == 2 || e.message == null) return; // action bar
            ChatLine line = chat.reset(e.message.getUnformattedText(), e.message.getFormattedText());
            Starlight.onChat(line);
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
            Starlight.onKey(LwjglKeys.toGlfw(code), action, 0);
        }

        @SubscribeEvent
        public void onMouse(MouseEvent e) {
            if (e.button >= 0) Starlight.onMouseButton(e.button, e.buttonstate ? Keys.ACTION_PRESS : Keys.ACTION_RELEASE);
            if (e.dwheel != 0 && Starlight.onScroll(e.dwheel > 0 ? 1 : -1)) e.setCanceled(true);
        }

        /** The main menu becomes the Starlight home screen as it opens (setting on; "Vanilla menu" shows it once). */
        @SubscribeEvent
        public void onGuiOpen(net.minecraftforge.client.event.GuiOpenEvent e) {
            if (e.gui == null || e.gui.getClass() != GuiMainMenu.class) return;
            if (vanillaTitleOnce) {
                vanillaTitleOnce = false;
                return;
            }
            Starlight k = Starlight.get();
            if (k != null && k.client.customTitle.on()) e.gui = new StarlightGuiScreen(null, k.home());
        }

        @SubscribeEvent
        public void onGuiInit(GuiScreenEvent.InitGuiEvent.Post e) {
            if (!Starlight.wantMenuButton()) return;
            if (e.gui instanceof GuiMainMenu) e.buttonList.add(new GuiButton(BUTTON_ID, 6, 6, 20, 20, "MW"));
            if (e.gui instanceof GuiIngameMenu) pauseRow(e.buttonList);
        }

        /** [Starlight Menu][Packs] under "Back to Game" (the topmost wide button); the rest of the menu moves down a row. */
        private void pauseRow(java.util.List<GuiButton> buttons) {
            GuiButton back = null;
            for (GuiButton b : buttons) if (b.visible && b.getButtonWidth() >= 150 && (back == null || b.yPosition < back.yPosition)) back = b;
            if (back == null) return;
            int row = back.yPosition + 24, half = (back.getButtonWidth() - 4) / 2;
            for (GuiButton b : buttons) if (b != back && b.yPosition >= row - 2) b.yPosition += 24;
            buttons.add(new GuiButton(BUTTON_ID, back.xPosition, row, half, 20, "Starlight Menu"));
            buttons.add(new GuiButton(BUTTON_ID + 1, back.xPosition + back.getButtonWidth() - half, row, half, 20, "Packs"));
        }

        @SubscribeEvent
        public void onButton(GuiScreenEvent.ActionPerformedEvent.Pre e) {
            if (e.gui instanceof GuiMainMenu && e.button.id == BUTTON_ID) {
                Starlight.get().openGui();
                e.setCanceled(true);
            } else if (e.gui instanceof GuiIngameMenu && (e.button.id == BUTTON_ID || e.button.id == BUTTON_ID + 1)) {
                Starlight.openFromPause(e.button.id == BUTTON_ID + 1);
                e.setCanceled(true);
            }
        }
    }
}
