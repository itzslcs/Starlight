package dev.mw19.core.hud;

import dev.mw19.api.module.HudModule;
import dev.mw19.core.module.ModuleManager;
import dev.mw19.core.render.Gfx;

import java.util.HashMap;
import java.util.Map;

/** Draws active HUD modules every frame. The per-frame path allocates nothing (reused call objects). */
public final class HudManager {
    private final ModuleManager modules;
    private final Map<String, HudElement> elements = new HashMap<String, HudElement>();

    private final SizeCall sizeCall = new SizeCall();
    private final RenderCall renderCall = new RenderCall();

    public HudManager(ModuleManager modules) {
        this.modules = modules;
    }

    public HudElement element(HudModule m) {
        HudElement e = elements.get(m.id());
        if (e == null) {
            e = new HudElement(m.defaultAnchor(), m.defaultX(), m.defaultY());
            elements.put(m.id(), e);
        }
        return e;
    }

    public void resetElement(HudModule m) {
        elements.remove(m.id());
    }

    public void setElement(String id, HudElement e) {
        elements.put(id, e);
    }

    public HudElement peek(String id) {
        return elements.get(id);
    }

    /**
     * Lays out and draws every active HUD module.
     * @param preview editor mode: modules draw placeholder content when they have no live data
     */
    public void render(Gfx g, float sw, float sh, boolean preview) {
        ModuleManager.State[] hud = modules.activeHud();
        for (ModuleManager.State s : hud) {
            if (!s.active()) continue;
            HudModule m = (HudModule) s.module;
            HudElement e = element(m);
            long t0 = System.nanoTime();
            sizeCall.m = m;
            sizeCall.g = g;
            sizeCall.preview = preview;
            if (!modules.guard(s, "size", sizeCall)) continue;
            float cw = sizeCall.w, ch = sizeCall.h;
            if (cw <= 0 || ch <= 0) {
                e.bw = e.bh = 0;
                continue;
            }
            HudLayout.place(e, cw, ch, sw, sh);
            g.push();
            g.translate(e.bx, e.by);
            g.scale(e.scale);
            g.pushAlpha(e.opacity);
            float bw = cw + 2 * e.padding, bh = ch + 2 * e.padding;
            if (e.background && m.wantsBackground()) g.roundRect(0, 0, bw, bh, e.radius, e.bgColor);
            if (e.border && m.wantsBackground()) g.roundOutline(0, 0, bw, bh, e.radius, 1f / e.scale, e.borderColor);
            g.translate(e.padding, e.padding);
            renderCall.m = m;
            renderCall.g = g;
            renderCall.style = e;
            renderCall.preview = preview;
            modules.guard(s, "render", renderCall);
            renderCall.g = null;
            g.popAlpha();
            g.pop();
            s.renderNanos = s.renderNanos * 0.95 + (System.nanoTime() - t0) * 0.05;
        }
    }

    private static final class SizeCall implements Runnable {
        HudModule m;
        Gfx g;
        boolean preview;
        float w, h;

        @Override
        public void run() {
            if (!m.visible(preview)) {
                w = h = 0;
                return;
            }
            w = m.width(g);
            h = m.height(g);
        }
    }

    private static final class RenderCall implements Runnable {
        HudModule m;
        Gfx g;
        HudElement style;
        boolean preview;

        @Override
        public void run() {
            m.render(g, style, preview);
        }
    }
}
