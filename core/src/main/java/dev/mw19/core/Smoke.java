package dev.mw19.core;

import dev.mw19.api.game.Game;
import dev.mw19.core.module.ModuleManager;
import dev.mw19.core.platform.ScreenHost;

/**
 * Self-driving smoke run (-Dmw19.smoke=1): title → GUI → HUD editor → world → GUI → game menus → Fast Chests →
 * crystal/anchor optimizers → every module on + chat lines →
 * HUD / HUD-editor screenshots → N seconds in world → checks → quit.
 * Prints "MW19 SMOKE PASS" or "MW19 SMOKE FAIL: <reason>"; scripts/smoke.sh checks the log.
 */
final class Smoke {
    private static final int WORLD_SECONDS = Integer.getInteger("mw19.smoke.seconds", 60);
    private static final long WORLD_TIMEOUT_TICKS = 20L * 60 * 6;

    private final Mw19 k;
    private int stage;
    private String keyCps = "not run";
    /** smoke.sh sets this when xdotool is available: a real OS click must toggle a module in the menu. */
    private static final boolean CLICKS = Boolean.getBoolean("mw19.smoke.clicks");
    private dev.mw19.core.module.ModuleManager.State clickTarget;
    private boolean clickBefore;
    private long clickTicks;
    private String guiClick = CLICKS ? "pending" : "skipped (no xdotool)";
    private String packs = "not run", packEnable = "not run", host = "not run", exploit = "not run", presets = "not run";
    private String chests = "not run", pauseRow = "not run", optimizers = "not run", skinRefresh = "not run";
    private int chestStep;
    private long chestWait;

    /**
     * debug-log 2026-09-26: every click on the Mods page threw and the smoke never noticed, because it only rendered
     * the menu. Now smoke.sh's xdotool helper clicks the FPS toggle through X11 -> GLFW/SDL/LWJGL -> Minecraft -> our
     * screen, and the run fails unless the module actually flips.
     */
    private void requestClick() {
        dev.mw19.core.gui.page.ModsPage mods = (dev.mw19.core.gui.page.ModsPage) k.gui().pages().get(0);
        float[] c = mods.toggleCenter("fps");
        clickTarget = k.modules.get("fps");
        if (c == null || clickTarget == null) {
            fail("FPS toggle not found on the Mods page");
            return;
        }
        float px = k.gui().menuScale() * k.platform.screens().guiScale();
        clickBefore = clickTarget.enabled();
        clickTicks = 0;
        String pid = java.lang.management.ManagementFactory.getRuntimeMXBean().getName().split("@")[0];
        Log.info("SMOKE CLICK " + System.getenv("DISPLAY") + " " + pid + " " + Math.round(c[0] * px) + " " + Math.round(c[1] * px));
    }

    private void checkClick() {
        if (clickTarget.enabled() != clickBefore) {
            guiClick = "ok";
            Log.info("SMOKE: a real mouse click toggled fps in the menu");
            k.modules.setEnabled(clickTarget, clickBefore);
            clickTarget = null;
            stageTicks = 36; // continue the stage timeline
        } else if (++clickTicks > 20 * 15) {
            fail("a real mouse click on the FPS toggle did not change it (GUI input broken?)");
            clickTarget = null;
        }
    }
    private long ticks, stageTicks, worldTicks, inWorldTotal;

    /** The game's own screenshot, and a line for smoke.sh to grab the X11 screen too (renderers such as VulkanMod
     *  bypass the game's capture path, so only the X11 grab shows what was on screen). */
    private void shot(String name) {
        k.platform.screenshot(name);
        Log.info("SMOKE SHOT " + System.getenv("DISPLAY") + " " + name);
    }

    Smoke(Mw19 k) {
        this.k = k;
        Log.info("SMOKE: armed");
    }

    private void next() {
        stage++;
        stageTicks = 0;
        Log.info("SMOKE: stage " + stage);
    }

    void tick() {
        ticks++;
        stageTicks++;
        ScreenHost.Kind screen = k.platform.screens().current();
        if (stage >= 4 && k.platform.inWorld()) inWorldTotal++;
        switch (stage) {
            case 0: // stable title (the MW19 home screen), then Minecraft's own title once: its MW button is a hook too
                if (screen != ScreenHost.Kind.TITLE) {
                    if (ticks > 20 * 180) fail("title screen never appeared (screen=" + screen + ")");
                    stageTicks = 0;
                } else if (stageTicks == 60) {
                    shot("mw19-smoke-0-home");
                } else if (stageTicks == 70) {
                    k.platform.screens().openVanillaTitle();
                } else if (stageTicks == 90) {
                    next();
                }
                break;
            case 1:
                k.openGui();
                next();
                break;
            case 2:
                if (stageTicks == 30) {
                    if (screen != ScreenHost.Kind.OURS) fail("GUI did not open on title (screen=" + screen + ")");
                    else shot("mw19-smoke-1-title-gui");
                } else if (stageTicks == 35 && CLICKS) {
                    requestClick();
                } else if (clickTarget != null) {
                    checkClick();
                } else if (stageTicks == 40) {
                    k.gui().openHudEditor();
                } else if (stageTicks == 70) {
                    shot("mw19-smoke-2-hud-editor");
                } else if (stageTicks == 80) {
                    k.gui().close();
                    next();
                }
                break;
            case 3:
                if (stageTicks == 20) {
                    k.platform.openWorld("mw19-smoke", 20260925L);
                } else if (k.platform.inWorld() && screen == ScreenHost.Kind.NONE) {
                    worldTicks++;
                    if (worldTicks == 100) next();
                } else if (stageTicks > WORLD_TIMEOUT_TICKS) {
                    fail("world did not load (screen=" + screen + ")");
                }
                break;
            case 4:
                if (stageTicks == 1) k.openGui();
                else if (stageTicks == 30) {
                    if (screen != ScreenHost.Kind.OURS) fail("GUI did not open in world (screen=" + screen + ")");
                    else shot("mw19-smoke-3-world-gui");
                } else if (stageTicks == 40) {
                    k.gui().close();
                } else if (stageTicks == 50) { // Minecraft's own menus in the MW19 style, with the MW19 row on the pause menu
                    k.platform.screens().openPause();
                } else if (stageTicks == 80) {
                    shot("mw19-smoke-3b-pause");
                    pauseRow = k.platform.selfTest("pauserow");
                    if (pauseRow.startsWith("FAIL")) fail("pause menu: " + pauseRow);
                    else if (!k.hooks.backdrop) fail("the pause menu did not draw the MW19 backdrop (hook never fired)");
                } else if (stageTicks == 90) {
                    k.platform.screens().openMultiplayer();
                } else if (stageTicks == 120) {
                    shot("mw19-smoke-3c-multiplayer");
                } else if (stageTicks == 130) {
                    k.platform.screens().openOptions(); // keycaps next to a slider (FOV) on the MW19 backdrop
                } else if (stageTicks == 155) {
                    shot("mw19-smoke-3d-options");
                } else if (stageTicks == 165) {
                    k.platform.screens().closeScreen();
                    next();
                }
                break;
            case 5: // Fast Chests: a chest scene drawn from block models, then by vanilla's block entity renderer, compared
                chestStage(screen);
                break;
            case 6: // the built-in crystal and anchor optimizers against the integrated server
                optimizerStage();
                break;
            case 7: // every module on, then feed chat through the incoming path
                if (stageTicks == 1) {
                    int n = 0;
                    for (ModuleManager.State s : k.modules.all()) {
                        if ((s.suspend() & ModuleManager.SUSPEND_UNAVAILABLE) != 0) continue;
                        k.modules.setEnabled(s, true);
                        n++;
                    }
                    Log.info("SMOKE: enabled " + n + " modules: " + k.modules.describeEnabled());
                } else if (stageTicks == 20 || stageTicks == 22) {
                    k.platform.debugIncomingChat("MW19 smoke chat line");
                } else if (stageTicks == 198) {
                    // KeyCPS end to end minus the input mixins (whose firing HookWatchdog reports separately): the
                    // platform's binding lookup must map these to attack/use, and the screenshot shows the counts.
                    long now = System.currentTimeMillis();
                    for (int i = 0; i < 7; i++) k.rates.record(k.platform, Keys.mouse(0), now);
                    for (int i = 0; i < 4; i++) k.rates.record(k.platform, Keys.mouse(1), now);
                } else if (stageTicks == 199) {
                    long now = System.currentTimeMillis();
                    keyCps = "lmb=" + k.rates.rate(Game.Binding.ATTACK, now) + " rmb=" + k.rates.rate(Game.Binding.USE, now);
                    if (!"lmb=7 rmb=4".equals(keyCps)) fail("KeyCPS counted " + keyCps + ", expected lmb=7 rmb=4");
                } else if (stageTicks == 200) {
                    shot("mw19-smoke-4-all-modules");
                } else if (stageTicks == 210) {
                    k.gui().openHudEditor();
                } else if (stageTicks == 240) {
                    shot("mw19-smoke-5-hud-editor-world");
                } else if (stageTicks == 250) {
                    k.gui().close();
                    next();
                }
                break;
            case 8: // 0.3.0 screens: Skins, Packs (a live Modrinth search), Host World (Open to LAN), exploit self-check
                if (stageTicks == 1) {
                    k.gui().openPage(dev.mw19.core.gui.page.SkinsPage.class);
                    try {
                        java.nio.file.Files.createDirectories(k.skins.folder());
                        java.nio.file.Files.write(k.skins.folder().resolve("mw19-smoke.png"), testSkin());
                    } catch (java.io.IOException e) {
                        fail("could not write the test skin: " + e);
                    }
                    if (!k.gui().page(dev.mw19.core.gui.page.SkinsPage.class).select("mw19-smoke.png")) fail("test skin not listed on the Skins page");
                } else if (stageTicks == 30) {
                    if (screen != ScreenHost.Kind.OURS) fail("Skins page did not open (screen=" + screen + ")");
                    else shot("mw19-smoke-7-skins");
                } else if (stageTicks == 34) {
                    // After a skin change the game's start-up profile is swapped for the new one (no account here: the
                    // self-test swaps a fresh profile in and back out)
                    skinRefresh = k.platform.selfTest("skinrefresh");
                    Log.info("SMOKE: skin refresh " + skinRefresh);
                    if (skinRefresh.startsWith("FAIL")) fail("skin refresh: " + skinRefresh);
                } else if (stageTicks == 40) {
                    k.gui().openPage(dev.mw19.core.gui.page.PacksPage.class);
                } else if (stageTicks == 150) {
                    // Enabling a pack (the Packs page's last step) with a tiny local pack: no download needed.
                    try {
                        java.nio.file.Path zip = k.platform.packs().folder().resolve("mw19-smoke-pack.zip");
                        java.nio.file.Files.createDirectories(zip.getParent());
                        java.util.zip.ZipOutputStream z = new java.util.zip.ZipOutputStream(java.nio.file.Files.newOutputStream(zip));
                        z.putNextEntry(new java.util.zip.ZipEntry("pack.mcmeta"));
                        z.write("{\"pack\":{\"pack_format\":34,\"description\":\"MW19 smoke\"}}".getBytes("UTF-8"));
                        z.closeEntry();
                        z.close();
                        if (!k.platform.packs().enable("mw19-smoke-pack.zip")) fail("the game did not find the smoke resource pack");
                    } catch (java.io.IOException e) {
                        fail("could not write the smoke resource pack: " + e);
                    }
                } else if (stageTicks == 230) {
                    java.util.List<String> on = k.platform.packs().enabled();
                    packEnable = on.isEmpty() ? "none enabled" : "top=" + on.get(0);
                    if (on.isEmpty() || !"mw19-smoke-pack.zip".equals(on.get(0))) fail("enabling a resource pack did not stick (" + packEnable + ")");
                } else if (stageTicks == 240) {
                    packs = k.gui().page(dev.mw19.core.gui.page.PacksPage.class).status(); // network: reported, not required
                    Log.info("SMOKE: packs " + packs);
                    shot("mw19-smoke-8-packs");
                } else if (stageTicks == 250) {
                    int port = k.host.open(0, false);
                    host = "port=" + port + " lan=" + k.host.lanAddress();
                    Log.info("SMOKE: host " + host);
                    if (port <= 0 || k.host.port() != port) fail("Host World did not open the world (" + host + ")");
                    else k.gui().openHost();
                } else if (stageTicks == 280) {
                    shot("mw19-smoke-9-host");
                } else if (stageTicks == 300) {
                    // Graphics presets: what first-run detection picked, then Medium and Undo (render distance is readable).
                    dev.mw19.core.perf.VideoPreset auto = k.video.current();
                    presets = "auto=" + (auto == null ? "none" : auto.label) + " (" + k.video.suggestion().reason + ")";
                    int before = k.platform.videoOption("renderDistance");
                    k.video.apply(dev.mw19.core.perf.VideoPreset.MEDIUM, false);
                    int medium = k.platform.videoOption("renderDistance");
                    k.gui().openPage(dev.mw19.core.gui.page.PerfPage.class);
                    k.video.undo();
                    int after = k.platform.videoOption("renderDistance");
                    presets += " rd " + before + "->" + medium + "->" + after;
                    Log.info("SMOKE: presets " + presets);
                    // a kept-state run (smoke.sh KEEP_STATE) had its first run earlier, and the last run undid the preset
                    if (auto == null && !k.video.firstRunDone(k.config.freshInstall)) fail("first-run graphics detection did not apply a preset");
                    else if (medium != 10 || after != before) fail("presets: render distance " + before + " -> " + medium + " -> " + after);
                } else if (stageTicks == 330) {
                    shot("mw19-smoke-9b-performance");
                    Log.info(String.format(java.util.Locale.ROOT, "SMOKE: menu draws in %.0f us/frame (Performance page)", k.perf.menuUs));
                    k.gui().openPage(dev.mw19.core.gui.page.AboutPage.class);
                } else if (stageTicks == 338) {
                    shot("mw19-smoke-9c-about"); // credits of the built-in ports and the bundled VulkanMod
                } else if (stageTicks == 339) {
                    k.gui().mouseScrolled(0, 0, -20); // through the GUI's scroll path: the notices at the end must be reachable
                } else if (stageTicks == 347) {
                    shot("mw19-smoke-9d-about-scrolled");
                } else if (stageTicks == 350) {
                    exploit = k.platform.selfTest("exploit"); // last: its probe toast would cover the screenshots
                    Log.info("SMOKE: exploit protection " + exploit);
                    if (exploit.startsWith("FAIL")) fail("exploit protection self-test: " + exploit);
                } else if (stageTicks == 360) {
                    k.gui().close();
                    next();
                }
                break;
            case 9:
                if (inWorldTotal >= 20L * WORLD_SECONDS) {
                    shot("mw19-smoke-10-final");
                    next();
                }
                break;
            case 10:
                if (stageTicks == 20) finish();
                break;
            default:
                break;
        }
    }

    /** Stage 5: chests in front of the camera, captured with Fast Chests on (the default), then off, then on again. */
    private void chestStage(ScreenHost.Kind screen) {
        if (stageTicks == 1) {
            if (!k.platform.supports("fast_chests")) {
                chests = "n/a";
                next();
                return;
            }
            String v = k.platform.minecraftVersion();
            String[] cmds = {
                    // game rules have snake_case ids from 1.21.11 (the old spelling is rejected there)
                    v.startsWith("26.") || v.equals("1.21.11") ? "gamerule send_command_feedback false" : "gamerule sendCommandFeedback false",
                    "gamemode spectator", "time set 6000", "weather clear",
                    "fill -4 198 -4 14 198 12 minecraft:stone", "fill -4 199 -4 14 204 12 minecraft:air",
                    // front row faces the camera: single, double (left half connects east), trapped, ender, side, back
                    "setblock 0 199 4 minecraft:chest[facing=north]",
                    "setblock 2 199 4 minecraft:chest[facing=north,type=left]", "setblock 3 199 4 minecraft:chest[facing=north,type=right]",
                    "setblock 5 199 4 minecraft:trapped_chest[facing=north]", "setblock 7 199 4 minecraft:ender_chest[facing=north]",
                    "setblock 9 199 4 minecraft:chest[facing=east]", "setblock 11 199 4 minecraft:chest[facing=south]",
                    // back row: a double chest along z, a copper chest (1.21.9+), a chest minecart and a block display
                    "setblock 0 199 8 minecraft:chest[facing=west,type=left]", "setblock 0 199 7 minecraft:chest[facing=west,type=right]",
                    "setblock 11 199 8 minecraft:copper_chest[facing=north]",
                    "summon minecraft:chest_minecart 4.5 199 8.5",
                    "summon minecraft:block_display 7 199 8 {block_state:{Name:\"minecraft:chest\",Properties:{facing:\"north\"}}}",
                    CHEST_VIEW};
            for (String c : cmds) k.platform.chat().sendCommand(c);
            chestStep = 0;
            return;
        }
        ModuleManager.State fast = k.modules.get("fast_chests");
        long since = stageTicks - chestWait;
        // Each view: shot, then 10 ticks before the camera moves (smoke.sh grabs the X11 screen ~0.3 s after a shot).
        switch (chestStep) {
            case 0: // chunks meshed with the chests in them (pack on): front view
            case 5: // vanilla block entity renderer again (pack off)
                if (chestStep == 0 && stageTicks < 100 || chestStep == 5 && since < 60) return;
                shot(chestStep == 0 ? "mw19-smoke-6a-chests-fast" : "mw19-smoke-6b-chests-vanilla");
                break;
            case 1:
            case 6:
                if (since < 10) return;
                k.platform.chat().sendCommand("tp @s 5.5 207 5 0 90"); // from above
                break;
            case 2:
            case 7:
                if (since < 10) return;
                shot(chestStep == 2 ? "mw19-smoke-6c-chests-fast-top" : "mw19-smoke-6d-chests-vanilla-top");
                if (chestStep == 2) {
                    chests = k.platform.selfTest("fastchests");
                    Log.info("SMOKE: fast chests " + chests);
                    if (!chests.startsWith("ok")) {
                        fail("Fast Chests: " + chests);
                        return;
                    }
                }
                break;
            case 3:
            case 8:
                if (since < 10) return;
                k.platform.chat().sendCommand(CHEST_VIEW);
                k.modules.setEnabled(fast, chestStep == 8); // resources reload without, then with the pack
                break;
            case 4: // vanilla models back (the chest model is empty again) / the pack's models in again
            case 9:
                boolean on = chestStep == 9;
                if (since > 20L * 90) {
                    fail("Fast Chests: resources did not reload within 90 s after turning it " + (on ? "on" : "off"));
                    return;
                }
                if (stageTicks % 10 != 0 || screen != ScreenHost.Kind.NONE) return;
                if (("0".equals(k.platform.selfTest("chestfaces"))) == on) return;
                break;
            case 10:
                if (since < 40) return;
                k.platform.chat().sendCommand("gamemode creative");
                chests += " off/on reloads ok";
                next();
                return;
            default:
                return;
        }
        chestWait = stageTicks;
        chestStep++;
    }

    private static final String CHEST_VIEW = "tp @s 5.5 202 -1.5 0 30";

    /**
     * Stage 6: Marlow's Crystal Optimizer and Hero's Anchor Optimizer (Fabric ports). Each is checked right after the
     * client acts and again after the server answered, on obsidian away from the chests, because the explosions are
     * real. The player is put back before each step (explosions push a creative player). Last step: a server opt-out
     * through the packet listener, after which a hit must be left alone.
     */
    private void optimizerStage() {
        if (stageTicks == 1) {
            if (!k.platform.supports("crystal_optimizer") || !k.platform.supports("anchor_optimizer")) {
                optimizers = "n/a";
                next();
                return;
            }
            k.modules.setEnabled(k.modules.get("crystal_optimizer"), true);
            k.modules.setEnabled(k.modules.get("anchor_optimizer"), true);
            for (String c : new String[]{"gamemode creative", "fill -26 198 -8 -14 198 6 minecraft:obsidian",
                    "fill -26 199 -8 -14 204 6 minecraft:air", OPT_SPOT, "summon minecraft:end_crystal -19.5 199 -0.5 {ShowBottom:0b}"}) {
                k.platform.chat().sendCommand(c);
            }
            optimizers = "";
            return;
        }
        String r = null;
        if (stageTicks == 40) {
            r = "codec=" + k.platform.selfTest("crystal:codec");
        } else if (stageTicks == 45) {
            r = "hit=" + k.platform.selfTest("crystal:hit");
        } else if (stageTicks == 85) {
            r = "removed=" + k.platform.selfTest("crystal:gone");
            k.platform.chat().sendCommand(OPT_SPOT);
            k.platform.chat().sendCommand("setblock -20 199 0 minecraft:respawn_anchor[charges=1]");
        } else if (stageTicks == 115) {
            r = "anchor=" + k.platform.selfTest("anchor:use:-20:199:0");
        } else if (stageTicks == 155) {
            r = "exploded=" + k.platform.selfTest("anchor:gone:-20:199:0");
            k.platform.chat().sendCommand(OPT_SPOT);
            k.platform.chat().sendCommand("summon minecraft:end_crystal -19.5 199 -0.5 {ShowBottom:0b}");
        } else if (stageTicks == 185) {
            r = "optout=" + k.platform.selfTest("crystal:optout");
        } else if (stageTicks == 190) {
            r = "honoured=" + k.platform.selfTest("crystal:optout-check");
        } else if (stageTicks == 250) {
            shot("mw19-smoke-6e-optimizers"); // upstream's opt-out notice is in chat 2 s after the opt-out
        } else if (stageTicks == 260) {
            next();
        }
        if (r == null) return;
        optimizers += (optimizers.isEmpty() ? "" : " ") + r;
        Log.info("SMOKE: optimizers " + r);
        if (r.contains("FAIL")) fail("optimizers: " + r);
    }

    private static final String OPT_SPOT = "tp @s -20.5 199 -3.5 0 0";

    /** A valid 64x64 RGBA PNG in the skin layout: coloured head, body, arms and legs (uncompressed rows, zlib stream). */
    static byte[] testSkin() throws java.io.IOException {
        int w = 64, h = 64;
        byte[] raw = new byte[h * (1 + w * 4)];
        for (int y = 0; y < h; y++) {
            raw[y * (1 + w * 4)] = 0; // filter: none
            for (int x = 0; x < w; x++) {
                int rgb = y < 16 ? (x < 32 ? 0x3A6FD8 : 0x000000) // head (+ transparent hat layer)
                        : y < 32 ? (x < 16 ? 0xE0B030 : x < 40 ? 0xC03030 : x < 56 ? 0x30A050 : 0) // right leg, body, right arm
                        : y >= 48 && x >= 16 && x < 48 ? (x < 32 ? 0xE0B030 : 0x30A050) : 0; // left leg, left arm
                boolean opaque = rgb != 0 || (y < 16 && x < 32);
                int o = y * (1 + w * 4) + 1 + x * 4;
                raw[o] = (byte) (rgb >> 16);
                raw[o + 1] = (byte) (rgb >> 8);
                raw[o + 2] = (byte) rgb;
                raw[o + 3] = (byte) (opaque ? 255 : 0);
            }
        }
        java.io.ByteArrayOutputStream z = new java.io.ByteArrayOutputStream();
        java.util.zip.DeflaterOutputStream d = new java.util.zip.DeflaterOutputStream(z);
        d.write(raw);
        d.close();
        java.io.ByteArrayOutputStream png = new java.io.ByteArrayOutputStream();
        png.write(new byte[]{(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A});
        java.nio.ByteBuffer ihdr = java.nio.ByteBuffer.allocate(13).putInt(w).putInt(h).put((byte) 8).put((byte) 6).put((byte) 0).put((byte) 0).put((byte) 0);
        chunk(png, "IHDR", ihdr.array());
        chunk(png, "IDAT", z.toByteArray());
        chunk(png, "IEND", new byte[0]);
        return png.toByteArray();
    }

    private static void chunk(java.io.ByteArrayOutputStream out, String type, byte[] data) throws java.io.IOException {
        byte[] t = type.getBytes("US-ASCII");
        out.write(java.nio.ByteBuffer.allocate(4).putInt(data.length).array());
        out.write(t);
        out.write(data);
        java.util.zip.CRC32 crc = new java.util.zip.CRC32();
        crc.update(t);
        crc.update(data);
        out.write(java.nio.ByteBuffer.allocate(4).putInt((int) crc.getValue()).array());
    }

    private void finish() {
        String missing = k.hooks.missing(true);
        if (!missing.isEmpty()) {
            fail("hooks never fired: " + missing);
            return;
        }
        if (!k.hooks.chat) {
            fail("chat hook never fired");
            return;
        }
        StringBuilder failed = new StringBuilder();
        for (ModuleManager.State s : k.modules.all()) {
            if ((s.suspend() & ModuleManager.SUSPEND_FAILED) != 0) failed.append(s.module.id()).append(' ');
        }
        if (failed.length() > 0) {
            fail("modules disabled by errors: " + failed.toString().trim());
            return;
        }
        java.util.List<ModuleManager.State> costly = new java.util.ArrayList<ModuleManager.State>(k.modules.all());
        java.util.Collections.sort(costly, new java.util.Comparator<ModuleManager.State>() {
            @Override
            public int compare(ModuleManager.State a, ModuleManager.State b) {
                return Double.compare(b.renderNanos, a.renderNanos);
            }
        });
        StringBuilder costs = new StringBuilder();
        for (int i = 0; i < 5 && i < costly.size(); i++) {
            costs.append(costly.get(i).module.id()).append('=').append(String.format(java.util.Locale.ROOT, "%.0f", costly.get(i).renderNanos / 1000.0)).append("us ");
        }
        Log.info("SMOKE: top HUD costs " + costs.toString().trim());
        Log.info("MW19 SMOKE PASS hooks[" + k.hooks.describe() + "] guiClick[" + guiClick + "] keycps[" + keyCps + "] presets[" + presets + "] packs[" + packs + "] packEnable[" + packEnable + "] host[" + host
                + "] exploit[" + exploit + "] pause[" + pauseRow + "] chests[" + chests + "] optimizers[" + optimizers + "] skin[" + skinRefresh + "] renderer[" + k.platform.rendererStatus() + "] modules["
                + k.modules.describeEnabled() + "] avgFrameMs=" + k.perf.avgFrameMs() + " ownUsPerFrame=" + k.perf.avgOwnUs());
        k.config.flush();
        shutdownWatchdog();
        k.platform.quit();
        stage = 99;
    }

    /**
     * A stuck shutdown would hold a smoke run until its 25-minute timeout with nothing to show for it (debug-log
     * 2026-09-25, 26.1.1). After 30 s this dumps every Java thread to the log and stderr and halts with exit code 3,
     * which smoke.sh reports as a failure.
     */
    private static void shutdownWatchdog() {
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(30000);
                } catch (InterruptedException e) {
                    return;
                }
                StringBuilder sb = new StringBuilder("MW19 SMOKE SHUTDOWN HANG: still running 30 s after quit. Threads:\n");
                for (java.util.Map.Entry<Thread, StackTraceElement[]> e : Thread.getAllStackTraces().entrySet()) {
                    sb.append('"').append(e.getKey().getName()).append("\" ").append(e.getKey().getState()).append('\n');
                    for (StackTraceElement el : e.getValue()) sb.append("    at ").append(el).append('\n');
                }
                System.err.println(sb);
                Log.error(sb.toString(), null);
                Runtime.getRuntime().halt(3);
            }
        }, "MW19 smoke shutdown watchdog");
        t.setDaemon(true);
        t.start();
    }

    private void fail(String why) {
        Log.error("MW19 SMOKE FAIL: " + why, null);
        stage = 99;
        k.platform.quit();
    }
}
