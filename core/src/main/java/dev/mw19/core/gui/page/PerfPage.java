package dev.mw19.core.gui.page;

import dev.mw19.api.util.Colors;
import dev.mw19.core.PerfStats;
import dev.mw19.core.gui.GuiRoot;
import dev.mw19.core.gui.Ui;
import dev.mw19.core.module.ModuleManager;
import dev.mw19.core.perf.VideoPreset;

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

    private final dev.mw19.core.gui.widget.Button[] presets = new dev.mw19.core.gui.widget.Button[VideoPreset.values().length];
    private final dev.mw19.core.gui.widget.Button auto, undo;
    private final dev.mw19.core.gui.widget.Toggle vulkan;

    public PerfPage(final GuiRoot root) {
        super(root);
        for (final VideoPreset v : VideoPreset.values()) {
            dev.mw19.core.gui.widget.Button b = new dev.mw19.core.gui.widget.Button(v.label, dev.mw19.core.gui.widget.Button.Style.SECONDARY, new Runnable() {
                @Override
                public void run() {
                    choose(v, "");
                }
            });
            b.tooltip = v.blurb;
            presets[v.ordinal()] = b;
        }
        auto = new dev.mw19.core.gui.widget.Button("Auto", dev.mw19.core.gui.widget.Button.Style.GHOST, new Runnable() {
            @Override
            public void run() {
                dev.mw19.core.perf.HardwareTier.Suggestion s = root.k.video.suggestion();
                choose(s.preset, " (picked for " + s.reason + ")");
            }
        });
        auto.tooltip = "The preset that suits the graphics card MW19 detected";
        undo = new dev.mw19.core.gui.widget.Button("Undo", dev.mw19.core.gui.widget.Button.Style.GHOST, new Runnable() {
            @Override
            public void run() {
                root.k.video.undo();
                root.k.toast("Graphics", "Your own video settings are back.", root.k.theme.good);
            }
        });
        undo.tooltip = "Puts back the video settings you had before the first preset";
        vulkan = new dev.mw19.core.gui.widget.Toggle(new dev.mw19.core.gui.widget.Toggle.Model() {
            @Override
            public boolean get() {
                return "vulkan".equals(root.k.platform.graphicsApi());
            }

            @Override
            public void set(boolean v) {
                root.k.platform.setGraphicsApi(v ? "vulkan" : "default");
                root.k.toast("Renderer", (v ? "Vulkan" : "OpenGL") + " takes effect after a restart (Vulkan falls back to OpenGL if the GPU cannot run it).", root.k.theme.warn);
            }
        });
    }

    private void choose(VideoPreset v, String why) {
        int n = root.k.video.apply(v, false);
        root.k.toast("Graphics: " + v.label, n + " video settings changed" + why + ". Undo restores yours.", root.k.theme.good);
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
            line2 = String.format(Locale.ROOT, "MW19: %.0f µs/frame · %.0f µs/tick (budget 300 µs/frame)", p.avgOwnUs(), p.tickUs);
            sorted.clear();
            for (ModuleManager.State s : root.k.modules.all()) if (s.active()) sorted.add(s);
            Collections.sort(sorted, new Comparator<ModuleManager.State>() {
                @Override
                public int compare(ModuleManager.State a, ModuleManager.State b) {
                    return Double.compare(b.renderNanos + b.tickNanos, a.renderNanos + a.tickNanos);
                }
            });
        }
        heading(ui, "Performance", "Graphics presets for the most FPS, and what MW19 itself costs");
        // presets: Potato .. High, Auto, Undo
        VideoPreset cur = root.k.video.current();
        float by = y + 28, gap = 3, aw = 34, uw = 36, pw = (w - aw - uw - gap * 6) / presets.length;
        for (int i = 0; i < presets.length; i++) {
            presets[i].style = cur != null && cur.ordinal() == i ? dev.mw19.core.gui.widget.Button.Style.PRIMARY : dev.mw19.core.gui.widget.Button.Style.SECONDARY;
            presets[i].bounds(x + i * (pw + gap), by, pw, 16).render(ui);
        }
        auto.bounds(x + presets.length * (pw + gap) + gap, by, aw, 16).render(ui);
        undo.enabled = root.k.video.active();
        undo.bounds(x + w - uw, by, uw, 16).render(ui);
        dev.mw19.core.perf.HardwareTier.Suggestion sug = root.k.video.suggestion();
        String gpu = root.k.platform.gpuName();
        ui.g.text(ui.g.ellipsize("Detected: " + (gpu.isEmpty() ? "unknown graphics" : gpu) + "  ·  suggests " + sug.preset.label, w), x, by + 20, ui.t.textDim, false);
        ui.g.text(ui.g.ellipsize(line1, w), x, by + 34, ui.t.text, false);
        ui.g.text(ui.g.ellipsize(line2, w), x, by + 45, p.avgOwnUs() > 300 ? ui.t.bad : ui.t.textDim, false);
        float top = by + 58;
        if (dev.mw19.core.Hooks.entityCulling && root.k.occlusion.calls > 0) {
            dev.mw19.core.perf.Occlusion oc = root.k.occlusion;
            ui.g.text(ui.g.ellipsize("Entity culling: " + (oc.culled * 100 / oc.calls) + "% of entity draws skipped (" + oc.culled + " of " + oc.calls
                    + " in the last second)", w), x, top, ui.t.textDim, false);
            top += 12;
        }
        if (root.k.platform.graphicsApi() != null) {
            ui.g.roundRect(x, top, w, 18, 4, ui.t.surface);
            ui.g.text("Vulkan renderer", x + 6, top + 5, ui.t.text, false);
            ui.g.text("after restart · falls back to OpenGL", x + 90, top + 5, ui.t.textDim, false);
            vulkan.bounds(x + w - 30, top + 3, 24, 12).render(ui);
            top += 22;
        }
        // graph
        float gx = x, gy = top, gw = w, gh = 56;
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

    @Override
    public boolean mouseClicked(Ui ui, int button) {
        for (dev.mw19.core.gui.widget.Button b : presets) if (b.mouseClicked(ui, button)) return true;
        return auto.mouseClicked(ui, button) || undo.mouseClicked(ui, button)
                || (root.k.platform.graphicsApi() != null && vulkan.mouseClicked(ui, button));
    }
}
