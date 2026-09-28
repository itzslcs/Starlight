package dev.starlight.core.modules;

import dev.starlight.api.hud.Anchor;
import dev.starlight.api.module.Rule;
import dev.starlight.api.setting.BoolSetting;
import dev.starlight.core.Starlight;
import dev.starlight.core.platform.Platform;

import java.util.Locale;

/** Horizontal speed in blocks per second, averaged over half a second. GRAY: vanilla shows no speed readout. */
public final class SpeedModule extends TextHud {
    private final BoolSetting vertical = add(new BoolSetting("vertical", "Include vertical", "Count up/down movement too", false));
    private final double[] samples = new double[10];
    private int idx;
    private double lx, ly, lz;
    private boolean have;
    private int tenths;

    public SpeedModule() {
        super("speed", "Speed", "How fast you move (blocks per second)", Rule.GRAY, false, Anchor.TOP_LEFT, 4, 16);
    }

    @Override
    public void onTick() {
        Platform p = Starlight.get().platform;
        if (!p.inWorld()) {
            have = false;
            return;
        }
        double x = p.x(), y = p.y(), z = p.z();
        if (have) {
            double dx = x - lx, dy = vertical.on() ? y - ly : 0, dz = z - lz;
            samples[idx] = Math.sqrt(dx * dx + dy * dy + dz * dz) * 20; // per tick -> per second
            idx = (idx + 1) % samples.length;
        }
        lx = x;
        ly = y;
        lz = z;
        have = true;
    }

    @Override
    protected boolean hasContent() {
        return Starlight.get().platform.inWorld();
    }

    @Override
    protected long key() {
        double s = 0;
        for (double v : samples) s += v;
        tenths = (int) Math.round(s / samples.length * 10);
        return tenths;
    }

    @Override
    protected String build() {
        return String.format(Locale.ROOT, "%.1f b/s", tenths / 10.0);
    }

    @Override
    protected String previewText() {
        return "5.6 b/s";
    }
}
