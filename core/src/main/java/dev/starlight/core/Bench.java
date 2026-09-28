package dev.starlight.core;

import dev.starlight.core.module.ModuleManager;
import dev.starlight.core.platform.ScreenHost;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Benchmark ({@code scripts/bench.sh}, {@code -Dstarlight.bench=1}): one fixed scene measured in phases, so every optimisation
 * gets before/after numbers (docs/PERF.md). The scene is a stone floor and wall at y=200 in a fixed-seed world, 120
 * villagers (no AI) behind the wall and 14 in front, with the camera facing the wall. Phases: baseline (culling off,
 * the player's options), culling on, Fast Chests on (the storage room above the villagers), the Low preset, then all
 * three (undone at the end). Software rendering exaggerates GPU-side savings, so compare phases within one run, not
 * against other machines.
 */
final class Bench {
    /** "warmup" (JIT, chunk loading) is not reported; "baseline-end" repeats the baseline to expose drift. */
    /** -Dstarlight.bench.scene=chests (bench.sh BENCH_SCENE=chests): a storage room in plain view, Fast Chests off / on / off. */
    private static final boolean CHESTS = "chests".equals(System.getProperty("starlight.bench.scene"));
    private static final String[] PHASES = CHESTS ? new String[]{"warmup", "baseline", "fastchests", "baseline-end"}
            : new String[]{"warmup", "baseline", "culling", "fastchests", "boost", "culling+fastchests+boost", "baseline-end"};
    private static final int WARMUP_TICKS = 200, BOOST_WARMUP_TICKS = 500, PHASE_TICKS = 300;
    private final Starlight k;
    private final boolean legacy;
    private final List<String> setup = new ArrayList<String>();
    private final float[] frames = new float[30000];
    private int stage, phase, sent, n;
    private long ticks, stageTicks, lastFrame;
    private boolean recording;
    private long gcCount0, gcMs0;

    Bench(Starlight k) {
        this.k = k;
        this.legacy = "1.8.9".equals(k.platform.minecraftVersion());
        Log.info("BENCH: armed");
    }

    private void next() {
        stage++;
        stageTicks = 0;
    }

    void tick() {
        ticks++;
        stageTicks++;
        ScreenHost.Kind screen = k.platform.screens().current();
        if (ticks > 20 * 60 * 8) {
            finish("timed out in stage " + stage);
            return;
        }
        switch (stage) {
            case 0:
                if (screen == ScreenHost.Kind.TITLE && stageTicks > 60) {
                    k.platform.openWorld("starlight-bench", 20260926L);
                    next();
                }
                break;
            case 1:
                if (k.platform.inWorld() && screen == ScreenHost.Kind.NONE && stageTicks > 60) {
                    buildScene();
                    next();
                }
                break;
            case 2: // a few commands per tick, then let chunks and entities settle
                for (int i = 0; i < 6 && sent < setup.size(); i++) k.platform.chat().sendCommand(setup.get(sent++));
                if (sent >= setup.size() && stageTicks > setup.size() / 6 + 100) next();
                break;
            case 3:
                runPhases();
                break;
            default:
                break;
        }
    }

    private void buildScene() {
        if (CHESTS) { // 41 x 25 single chests on a floor, filling the view (fill places them unconnected)
            setup.add("weather clear");
            setup.add("time set 6000");
            setup.add("fill -24 199 -4 24 199 34 minecraft:stone");
            setup.add("fill -24 200 -4 24 210 34 minecraft:air");
            setup.add("fill -20 200 6 20 200 30 minecraft:chest");
            setup.add("tp @p 0.5 205 0.5 0 30");
            Log.info("BENCH: scene has 1025 chests in view");
            return;
        }
        String stone = legacy ? "stone" : "minecraft:stone";
        String mob = legacy ? "Villager" : "minecraft:villager";
        String tag = legacy ? "{NoAI:1,Silent:1,PersistenceRequired:1}" : "{NoAI:1b,Silent:1b,PersistenceRequired:1b}";
        setup.add("weather clear");
        setup.add("time set 6000");
        setup.add("tp @p 0.5 205 0.5 0 0");
        setup.add("fill -24 199 -8 24 199 40 " + stone);
        setup.add("fill -24 200 12 24 215 12 " + stone);
        setup.add("tp @p 0.5 200 0.5 0 0");
        for (int x = -21; x <= 21; x += 3) {
            for (int z = 15; z <= 36; z += 3) setup.add("summon " + mob + " " + x + ".5 200 " + z + ".5 " + tag);
        }
        for (int x = -12; x <= 12; x += 4) {
            for (int z = 5; z <= 9; z += 4) setup.add("summon " + mob + " " + x + ".5 200 " + z + ".5 " + tag);
        }
        int villagers = setup.size() - 6;
        // A storage room behind the wall (block entity culling): chests above the villagers' heads.
        String chest = legacy ? "chest" : "minecraft:chest";
        for (int x = -22; x <= 22; x += 2) {
            for (int z = 14; z <= 38; z += 2) setup.add("setblock " + x + " 203 " + z + " " + chest);
        }
        Log.info("BENCH: scene has " + villagers + " villagers and " + (setup.size() - 6 - villagers) + " chests");
    }

    private void runPhases() {
        if (stageTicks == 1) {
            configure(PHASES[phase]);
        } else if (stageTicks == warmup()) {
            n = 0;
            lastFrame = 0;
            gcCount0 = gcCount();
            gcMs0 = gcMs();
            recording = true;
        } else if (stageTicks == warmup() + PHASE_TICKS) {
            recording = false;
            if (!"warmup".equals(PHASES[phase])) report(PHASES[phase]);
            phase++;
            stageTicks = 0;
            if (phase == PHASES.length) finish(null);
        }
    }

    /** Option changes and Fast Chests' resource reload rebuild every chunk section; those phases wait until that is done. */
    private int warmup() {
        String p = PHASES[phase];
        return "warmup".equals(p) || "baseline".equals(p) ? WARMUP_TICKS : BOOST_WARMUP_TICKS;
    }

    private void configure(String p) {
        ModuleManager.State culling = k.modules.get("entity_culling"), fast = k.modules.get("fast_chests");
        if (culling != null) k.modules.setEnabled(culling, p.contains("culling"));
        if (fast != null && (fast.suspend() & ModuleManager.SUSPEND_UNAVAILABLE) == 0) k.modules.setEnabled(fast, p.contains("fastchests"));
        if (p.contains("boost") && !k.video.active()) k.video.apply(dev.starlight.core.perf.VideoPreset.LOW, true);
        if (!p.contains("boost") && k.video.active()) k.video.undo();
        Log.info("BENCH: phase " + p);
    }

    /** Called once per rendered frame (HUD pass). */
    void frame() {
        long now = System.nanoTime();
        if (recording && lastFrame != 0 && n < frames.length) frames[n++] = (now - lastFrame) / 1_000_000f;
        lastFrame = now;
    }

    private void report(String p) {
        if (n == 0) {
            Log.info("Starlight BENCH mc=" + k.platform.minecraftVersion() + " phase=" + p + " frames=0");
            return;
        }
        float[] s = Arrays.copyOf(frames, n);
        Arrays.sort(s);
        double sum = 0;
        for (float f : s) sum += f;
        int worst = Math.max(1, n / 100);
        double worstSum = 0;
        for (int i = n - worst; i < n; i++) worstSum += s[i];
        String culled = Hooks.entityCulling ? k.occlusion.culled + "/" + k.occlusion.calls + " blocks=" + k.blockOcclusion.culled
                + "/" + k.blockOcclusion.calls : "off";
        int hitches = 0;
        for (float f : s) if (f > 50) hitches++;
        Log.info(String.format(Locale.ROOT, "Starlight BENCH mc=%s phase=%s frames=%d avgFps=%.1f low1=%.1f p99ms=%.2f hitches50=%d gc=%dx/%dms culled=%s ownUs=%.0f",
                k.platform.minecraftVersion(), p, n, 1000 * n / sum, 1000 * worst / worstSum, s[Math.min(n - 1, (int) (n * 0.99))],
                hitches, gcCount() - gcCount0, gcMs() - gcMs0, culled, k.perf.avgOwnUs()));
    }

    private void finish(String why) {
        if (why != null) Log.error("Starlight BENCH FAIL: " + why, null);
        if (k.video.active()) k.video.undo();
        stage = 99;
        k.config.flush();
        k.platform.quit();
    }

    private static long gcCount() {
        long c = 0;
        for (java.lang.management.GarbageCollectorMXBean b : java.lang.management.ManagementFactory.getGarbageCollectorMXBeans()) c += Math.max(0, b.getCollectionCount());
        return c;
    }

    private static long gcMs() {
        long t = 0;
        for (java.lang.management.GarbageCollectorMXBean b : java.lang.management.ManagementFactory.getGarbageCollectorMXBeans()) t += Math.max(0, b.getCollectionTime());
        return t;
    }
}
