package dev.kestrel.core;

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
        Log.info("KESTREL SMOKE PASS hooks[" + k.hooks.describe() + "] plugins[" + plugins.toString().trim() + "] modules["
                + k.modules.describeEnabled() + "] avgFrameMs=" + k.perf.avgFrameMs() + " ownUsPerFrame=" + k.perf.avgOwnUs());
        k.config.flush();
        k.platform.quit();
        stage = 99;
    }

    private void fail(String why) {
        Log.error("KESTREL SMOKE FAIL: " + why, null);
        stage = 99;
        k.platform.quit();
    }
}
