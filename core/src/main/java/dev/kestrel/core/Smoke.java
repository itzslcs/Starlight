package dev.kestrel.core;

import dev.kestrel.api.game.Game;
import dev.kestrel.core.module.ModuleManager;
import dev.kestrel.core.platform.ScreenHost;
import dev.kestrel.core.plugin.PluginManager;

/**
 * Self-driving smoke run (-Dkestrel.smoke=1): title → GUI → HUD editor → world → GUI → every module on + chat lines →
 * HUD / HUD-editor screenshots → N seconds in world → checks → quit.
 * Prints "KESTREL SMOKE PASS" or "KESTREL SMOKE FAIL: <reason>"; scripts/smoke.sh checks the log.
 */
final class Smoke {
    private static final int WORLD_SECONDS = Integer.getInteger("kestrel.smoke.seconds", 60);
    private static final long WORLD_TIMEOUT_TICKS = 20L * 60 * 6;

    private final Kestrel k;
    private int stage;
    private String keyCps = "not run";
    /** smoke.sh sets this when xdotool is available: a real OS click must toggle a module in the menu. */
    private static final boolean CLICKS = Boolean.getBoolean("kestrel.smoke.clicks");
    private dev.kestrel.core.module.ModuleManager.State clickTarget;
    private boolean clickBefore;
    private long clickTicks;
    private String guiClick = CLICKS ? "pending" : "skipped (no xdotool)";

    /**
     * debug-log 2026-09-26: every click on the Mods page threw and the smoke never noticed, because it only rendered
     * the menu. Now smoke.sh's xdotool helper clicks the FPS toggle through X11 -> GLFW/SDL/LWJGL -> Minecraft -> our
     * screen, and the run fails unless the module actually flips.
     */
    private void requestClick() {
        dev.kestrel.core.gui.page.ModsPage mods = (dev.kestrel.core.gui.page.ModsPage) k.gui().pages().get(0);
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

    Smoke(Kestrel k) {
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
            case 0: // wait for a stable title screen
                if (screen == ScreenHost.Kind.TITLE && stageTicks > 60) next();
                else if (ticks > 20 * 180) fail("title screen never appeared (screen=" + screen + ")");
                break;
            case 1:
                k.openGui();
                next();
                break;
            case 2:
                if (stageTicks == 30) {
                    if (screen != ScreenHost.Kind.OURS) fail("GUI did not open on title (screen=" + screen + ")");
                    else k.platform.screenshot("kestrel-smoke-1-title-gui");
                } else if (stageTicks == 35 && CLICKS) {
                    requestClick();
                } else if (clickTarget != null) {
                    checkClick();
                } else if (stageTicks == 40) {
                    k.gui().openHudEditor();
                } else if (stageTicks == 70) {
                    k.platform.screenshot("kestrel-smoke-2-hud-editor");
                } else if (stageTicks == 80) {
                    k.gui().close();
                    next();
                }
                break;
            case 3:
                if (stageTicks == 20) {
                    k.platform.openWorld("kestrel-smoke", 20260925L);
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
                    else k.platform.screenshot("kestrel-smoke-3-world-gui");
                } else if (stageTicks == 40) {
                    k.gui().close();
                    next();
                }
                break;
            case 5: // every module on (plugins included), then feed chat through the incoming path
                if (stageTicks == 1) {
                    int n = 0;
                    for (ModuleManager.State s : k.modules.all()) {
                        if ((s.suspend() & ModuleManager.SUSPEND_UNAVAILABLE) != 0) continue;
                        k.modules.setEnabled(s, true);
                        n++;
                    }
                    Log.info("SMOKE: enabled " + n + " modules: " + k.modules.describeEnabled());
                } else if (stageTicks == 20 || stageTicks == 22) {
                    k.platform.debugIncomingChat("Kestrel smoke chat line");
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
                    k.platform.screenshot("kestrel-smoke-4-all-modules");
                } else if (stageTicks == 210) {
                    k.gui().openHudEditor();
                } else if (stageTicks == 240) {
                    k.platform.screenshot("kestrel-smoke-5-hud-editor-world");
                } else if (stageTicks == 250) {
                    k.gui().close();
                    next();
                }
                break;
            case 6:
                if (inWorldTotal >= 20L * WORLD_SECONDS) {
                    k.platform.screenshot("kestrel-smoke-6-final");
                    next();
                }
                break;
            case 7:
                if (stageTicks == 20) finish();
                break;
            default:
                break;
        }
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
        StringBuilder plugins = new StringBuilder();
        for (PluginManager.Entry e : k.plugins.entries()) {
            plugins.append(e.id()).append('=').append(e.state).append(' ');
            if (e.state == PluginManager.State.FAILED) {
                fail("plugin " + e.id() + " failed: " + e.error);
                return;
            }
        }
        Log.info("KESTREL SMOKE PASS hooks[" + k.hooks.describe() + "] guiClick[" + guiClick + "] keycps[" + keyCps + "] plugins[" + plugins.toString().trim() + "] modules["
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
                StringBuilder sb = new StringBuilder("KESTREL SMOKE SHUTDOWN HANG: still running 30 s after quit. Threads:\n");
                for (java.util.Map.Entry<Thread, StackTraceElement[]> e : Thread.getAllStackTraces().entrySet()) {
                    sb.append('"').append(e.getKey().getName()).append("\" ").append(e.getKey().getState()).append('\n');
                    for (StackTraceElement el : e.getValue()) sb.append("    at ").append(el).append('\n');
                }
                System.err.println(sb);
                Log.error(sb.toString(), null);
                Runtime.getRuntime().halt(3);
            }
        }, "Kestrel smoke shutdown watchdog");
        t.setDaemon(true);
        t.start();
    }

    private void fail(String why) {
        Log.error("KESTREL SMOKE FAIL: " + why, null);
        stage = 99;
        k.platform.quit();
    }
}
