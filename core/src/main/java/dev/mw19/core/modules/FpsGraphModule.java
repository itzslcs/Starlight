package dev.mw19.core.modules;

import dev.mw19.api.hud.Anchor;
import dev.mw19.api.hud.HudStyle;
import dev.mw19.api.module.HudModule;
import dev.mw19.api.module.Rule;
import dev.mw19.api.render.Renderer;
import dev.mw19.api.setting.NumberSetting;
import dev.mw19.api.util.Colors;
import dev.mw19.core.Mw19;
import dev.mw19.core.PerfStats;

/** Live frame-time graph: one bar per recent frame, green under 16.7 ms, yellow under 33 ms, red above. */
public final class FpsGraphModule extends HudModule {
    private final NumberSetting bars = add(new NumberSetting("bars", "Frames shown", "How many recent frames to draw", 60, 20, 120, 10));
    private final NumberSetting tall = add(new NumberSetting("height", "Height", "Graph height", 24, 12, 60, 2));

    public FpsGraphModule() {
        super("fps_graph", "FPS Graph", "Frame times as a live graph (spot stutters)", Rule.ALLOWED, false, Anchor.TOP, 0, 42);
    }

    @Override
    public float width(Renderer r) {
        return bars.intValue() + 4;
    }

    @Override
    public float height(Renderer r) {
        return tall.intValue() + 4;
    }

    @Override
    public void render(Renderer r, HudStyle style, boolean preview) {
        PerfStats p = Mw19.get().perf;
        int n = Math.min(p.count, bars.intValue());
        float h = tall.intValue(), max = 50f;
        r.rect(0, 0, width(r), h + 4, 0x66000000);
        float y60 = 2 + h - 16.7f / max * h;
        r.rect(2, y60, 2 + bars.intValue(), y60 + 0.5f, 0x55FFFFFF);
        for (int i = 0; i < n; i++) {
            float ms = p.frameMs[(p.idx - n + i + PerfStats.N) % PerfStats.N];
            float bh = Math.min(h, ms / max * h);
            int c = ms > 33 ? 0xFFFF5C5C : ms > 16.7f ? 0xFFFFC53D : 0xFF3DD68C;
            float x = 2 + bars.intValue() - n + i;
            r.rect(x, 2 + h - bh, x + 1, 2 + h, Colors.fade(c, 0.85f));
        }
    }

    @Override
    public boolean wantsBackground() {
        return false;
    }
}
