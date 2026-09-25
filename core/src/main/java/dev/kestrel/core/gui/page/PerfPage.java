package dev.kestrel.core.gui.page;

import dev.kestrel.api.util.Colors;
import dev.kestrel.core.PerfStats;
import dev.kestrel.core.gui.GuiRoot;
import dev.kestrel.core.gui.Ui;
import dev.kestrel.core.module.ModuleManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** Frame-time graph plus our own cost (total and per module). */
public final class PerfPage extends Page {
    private long lastStats;
    private String line1 = "", line2 = "";
    private final List<ModuleManager.State> sorted = new ArrayList<ModuleManager.State>();

    public PerfPage(GuiRoot root) {
        super(root);
    }

    @Override
    public String title() {
        return "Performance";
    }

    @Override
    public String icon() {
        return "perf";
    }

    @Override
    public void render(Ui ui) {
        PerfStats p = root.k.perf;
        if (ui.now - lastStats > 500) {
            lastStats = ui.now;
            float avg = p.avgFrameMs();
            line1 = String.format(Locale.ROOT, "%.0f FPS avg · 1%% low %.0f · 0.1%% low %.0f · p99 %.1f ms",
                    avg > 0 ? 1000 / avg : 0, p.lowFps(0.01f), p.lowFps(0.001f), p.percentile(0.99f));
            line2 = String.format(Locale.ROOT, "Kestrel: %.1f µs/frame (HUD) · %.1f µs/tick · budget 300 µs/frame", p.avgOwnUs(), p.tickUs);
            sorted.clear();
            for (ModuleManager.State s : root.k.modules.all()) if (s.active()) sorted.add(s);
            Collections.sort(sorted, new Comparator<ModuleManager.State>() {
                @Override
                public int compare(ModuleManager.State a, ModuleManager.State b) {
                    return Double.compare(b.renderNanos + b.tickNanos, a.renderNanos + a.tickNanos);
                }
            });
        }
        heading(ui, "Performance", "Measured while the HUD renders (the menu itself is excluded from the budget)");
        ui.g.text(line1, x, y + 26, ui.t.text, false);
        ui.g.text(line2, x, y + 37, p.avgOwnUs() > 300 ? ui.t.bad : ui.t.textDim, false);
        // graph
        float gx = x, gy = y + 50, gw = w, gh = 70;
        ui.g.roundRect(gx, gy, gw, gh, 4, ui.t.surface);
        int n = Math.min(p.count, (int) gw);
        float max = 50f; // ms at the top of the graph
        for (int i = 0; i < n; i++) {
            int idx = (p.idx - n + i + PerfStats.N) % PerfStats.N;
            float ms = p.frameMs[idx];
            float bh = Math.min(gh - 4, ms / max * (gh - 4));
            int col = ms > 33 ? ui.t.bad : ms > 16.7f ? ui.t.warn : ui.t.good;
            ui.g.rect(gx + gw - n + i, gy + gh - 2 - bh, gx + gw - n + i + 1, gy + gh - 2, Colors.fade(col, 0.8f));
        }
        float y60 = gy + gh - 2 - 16.7f / max * (gh - 4);
        ui.g.rect(gx, y60, gx + gw, y60 + 0.5f, 0x40FFFFFF);
        ui.g.text("60 FPS", gx + 3, y60 - 9, ui.t.textDim, false);
        // per-module
        float ty = gy + gh + 8;
        ui.g.text("Active modules by cost (µs per frame + per tick)", x, ty, ui.t.text, false);
        ty += 12;
        for (ModuleManager.State s : sorted) {
            if (ty > y + h - 10) break;
            ui.g.text(s.module.name(), x + 4, ty, ui.t.textDim, false);
            ui.g.textRight(String.format(Locale.ROOT, "%.1f  /  %.1f", s.renderNanos / 1000.0, s.tickNanos / 1000.0), x + w - 4, ty, ui.t.text, false);
            ty += 10;
        }
    }
}
