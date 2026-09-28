package dev.starlight.core.modules;

import dev.starlight.api.hud.Anchor;
import dev.starlight.api.module.Rule;
import dev.starlight.api.setting.BoolSetting;
import dev.starlight.core.Starlight;

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.lang.reflect.Method;

/**
 * Memory use of the game JVM and process CPU load. Refreshes twice a second on a background thread: reading the CPU
 * load can take milliseconds, which on the render thread showed up as the costliest HUD module (85 µs/frame average
 * on the 2026-09-26 smoke's Performance page).
 */
public final class SystemModule extends TextHud {
    private final BoolSetting cpu = add(new BoolSetting("cpu", "CPU", "Also show process CPU load", true));
    private final OperatingSystemMXBean os = ManagementFactory.getOperatingSystemMXBean();
    private volatile Method cpuLoad;
    private volatile boolean triedCpu;
    private volatile int memPct, usedMb, maxMb, cpuPct = -1;
    private volatile boolean sampling;
    private long lastSample;
    private final Runnable sample = new Runnable() {
        @Override
        public void run() {
            try {
                Runtime rt = Runtime.getRuntime();
                long used = rt.totalMemory() - rt.freeMemory(), max = rt.maxMemory();
                usedMb = (int) (used >> 20);
                maxMb = (int) (max >> 20);
                memPct = max > 0 ? (int) (used * 100 / max) : 0;
                cpuPct = cpu.on() ? cpuPercent() : -1;
            } finally {
                sampling = false;
            }
        }
    };

    public SystemModule() {
        super("system", "Memory / CPU", "Game memory use and CPU load", Rule.ALLOWED, false, Anchor.TOP, 0, 28);
    }

    @Override
    protected long key() {
        long now = System.currentTimeMillis();
        if (now - lastSample >= 500 && !sampling) {
            lastSample = now;
            sampling = true;
            Starlight.get().scheduler.runAsync(sample);
        }
        return ((long) memPct << 40) | ((long) usedMb << 20) | ((cpuPct + 1) & 0xFFFF);
    }

    /** com.sun.management.OperatingSystemMXBean#getProcessCpuLoad, looked up once; -1 if unavailable. */
    private int cpuPercent() {
        if (!triedCpu) {
            triedCpu = true;
            try {
                // Via the exported interface: the implementation class is not accessible on Java 9+.
                Class<?> iface = Class.forName("com.sun.management.OperatingSystemMXBean");
                cpuLoad = iface.isInstance(os) ? iface.getMethod("getProcessCpuLoad") : null;
            } catch (Exception e) {
                cpuLoad = null;
            }
        }
        if (cpuLoad == null) return -1;
        try {
            double v = ((Number) cpuLoad.invoke(os)).doubleValue();
            return v < 0 ? -1 : (int) Math.round(v * 100);
        } catch (Exception e) {
            cpuLoad = null;
            return -1;
        }
    }

    @Override
    protected String build() {
        String s = "RAM " + memPct + "% (" + usedMb + "/" + maxMb + " MB)";
        return cpuPct >= 0 ? s + "  CPU " + cpuPct + "%" : s;
    }
}
