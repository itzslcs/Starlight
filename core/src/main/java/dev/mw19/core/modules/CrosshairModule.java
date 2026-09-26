package dev.mw19.core.modules;

import dev.mw19.api.module.Category;
import dev.mw19.api.module.Module;
import dev.mw19.api.module.Rule;
import dev.mw19.api.setting.BoolSetting;
import dev.mw19.api.setting.ChoiceSetting;
import dev.mw19.api.setting.ColorSetting;
import dev.mw19.api.setting.NumberSetting;
import dev.mw19.core.Hooks;
import dev.mw19.core.Mw19;
import dev.mw19.core.module.Overlay;
import dev.mw19.core.platform.Platform;
import dev.mw19.core.platform.ScreenHost;
import dev.mw19.core.render.Gfx;

/** Replaces the vanilla crosshair with a configurable one (same position, same purpose). */
public final class CrosshairModule extends Module implements Overlay {
    private final ChoiceSetting style = add(new ChoiceSetting("style", "Style", "Crosshair shape", "Cross", "Cross", "Dot", "Circle", "T", "Plus + dot"));
    private final NumberSetting size = add(new NumberSetting("size", "Size", "Arm length", 4, 0.5, 12, 0.25));
    private final NumberSetting gap = add(new NumberSetting("gap", "Gap", "Space around the centre", 1.5, 0, 8, 0.25));
    private final NumberSetting thickness = add(new NumberSetting("thickness", "Thickness", "Line width", 1, 0.25, 4, 0.25));
    private final NumberSetting scale = add(new NumberSetting("scale", "Scale", "Shrinks or grows the whole crosshair", 1, 0.25, 3, 0.05, "×"));
    private final ColorSetting color = add(new ColorSetting("color", "Colour", "Crosshair colour", 0xFFFFFFFF));
    private final BoolSetting outline = add(new BoolSetting("outline", "Outline", "Dark outline for visibility", true));
    private final BoolSetting cooldown = add(new BoolSetting("cooldown", "Attack cooldown", "Show the attack cooldown under the crosshair (1.9+)", true));
    private final BoolSetting thirdPerson = add(new BoolSetting("third_person", "In third person", "Also draw in third-person views", false));

    public CrosshairModule() {
        super("crosshair", "Custom Crosshair", "Your own crosshair shape and colour", Category.VISUAL, Rule.ALLOWED, false);
    }

    @Override
    public void onEnable() {
        Hooks.hideCrosshair = true;
    }

    @Override
    public void onDisable() {
        Hooks.hideCrosshair = false;
    }

    @Override
    public void renderOverlay(Gfx g, float sw, float sh) {
        Platform p = Mw19.get().platform;
        if (!p.inWorld() || p.hideGui() || p.screens().current() == ScreenHost.Kind.OURS) return;
        if (p.perspective() != 0 && !thirdPerson.on()) return;
        float cx = Math.round(sw / 2f * g.guiScale()) / g.guiScale(), cy = Math.round(sh / 2f * g.guiScale()) / g.guiScale();
        int c = color.argb(g.millis()), o = 0xC0000000;
        float k = scale.floatValue();
        float t = thickness.floatValue() * k, s = size.floatValue() * k, gp = gap.floatValue() * k, h = t / 2f;
        String st = style.get();
        if (st.equals("Dot") || st.equals("Plus + dot")) {
            if (outline.on()) g.roundRect(cx - h - 0.5f, cy - h - 0.5f, t + 1, t + 1, (t + 1) / 2f, o);
            g.roundRect(cx - h, cy - h, t, t, t / 2f, c);
        }
        if (st.equals("Circle")) {
            float r = gp + s;
            if (outline.on()) g.roundOutline(cx - r - 0.5f, cy - r - 0.5f, r * 2 + 1, r * 2 + 1, r + 0.5f, t + 1, o);
            g.roundOutline(cx - r, cy - r, r * 2, r * 2, r, t, c);
        }
        if (st.equals("Cross") || st.equals("T") || st.equals("Plus + dot")) {
            arm(g, cx - h, cy - gp - s, cx + h, cy - gp, c, o, !st.equals("T")); // up (skipped for T)
            arm(g, cx - h, cy + gp, cx + h, cy + gp + s, c, o, true);            // down
            arm(g, cx - gp - s, cy - h, cx - gp, cy + h, c, o, true);            // left
            arm(g, cx + gp, cy - h, cx + gp + s, cy + h, c, o, true);            // right
        }
        if (cooldown.on()) {
            float cd = p.attackCooldown();
            if (cd < 1f) {
                float w = 16, y = cy + gp + s + 3;
                g.rect(cx - w / 2, y, cx + w / 2, y + 1.5f, 0x80000000);
                g.rect(cx - w / 2, y, cx - w / 2 + w * cd, y + 1.5f, c);
            }
        }
    }

    private void arm(Gfx g, float x1, float y1, float x2, float y2, int c, int o, boolean draw) {
        if (!draw) return;
        if (outline.on()) g.rect(x1 - 0.5f, y1 - 0.5f, x2 + 0.5f, y2 + 0.5f, o);
        g.rect(x1, y1, x2, y2, c);
    }
}
