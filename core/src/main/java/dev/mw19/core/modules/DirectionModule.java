package dev.mw19.core.modules;

import dev.mw19.api.hud.Anchor;
import dev.mw19.api.hud.HudStyle;
import dev.mw19.api.module.HudModule;
import dev.mw19.api.module.Rule;
import dev.mw19.api.render.Renderer;
import dev.mw19.api.setting.BoolSetting;
import dev.mw19.api.setting.NumberSetting;
import dev.mw19.core.Mw19;

/** Compass tape: marks slide with your yaw (same information as F3 facing). */
public final class DirectionModule extends HudModule {
    private static final String[] LABELS = {"S", "SW", "W", "NW", "N", "NE", "E", "SE"};
    private final NumberSetting width = add(new NumberSetting("width", "Width", "Tape width", 140, 60, 300, 10));
    private final BoolSetting degrees = add(new BoolSetting("degrees", "Degrees", "Show the exact heading under the tape", true));

    private int shownDeg = Integer.MIN_VALUE;
    private String degText = "";

    public DirectionModule() {
        super("direction", "Direction", "A compass tape for your heading", Rule.ALLOWED, false, Anchor.TOP, 0, 4);
    }

    @Override
    public float width(Renderer r) {
        return width.floatValue();
    }

    @Override
    public float height(Renderer r) {
        return degrees.on() ? 21 : 11;
    }

    @Override
    public void render(Renderer r, HudStyle style, boolean preview) {
        float yaw = Mw19.get().platform.yaw();
        float w = width.floatValue(), half = w / 2f, span = 90f; // degrees visible either side
        float heading = ((yaw % 360) + 360) % 360; // 0 = south (+Z) in Minecraft
        r.pushClip(0, 0, w, 11);
        for (int deg = 0; deg < 360; deg += 15) {
            float diff = deg - heading;
            if (diff > 180) diff -= 360;
            if (diff < -180) diff += 360;
            if (Math.abs(diff) > span) continue;
            float x = half + diff / span * half;
            if (deg % 45 == 0) {
                String l = LABELS[deg / 45];
                int col = "N".equals(l) ? style.accentColor() : style.textColor();
                r.text(l, x - r.textWidth(l) / 2f, 1, col, style.textShadow());
            } else {
                r.rect(x - 0.5f, 3, x + 0.5f, 7, style.textColor() & 0x80FFFFFF);
            }
        }
        r.popClip();
        r.rect(half - 0.5f, 9, half + 0.5f, 11, style.accentColor());
        if (degrees.on()) {
            int d = Math.round(heading) % 360;
            if (d != shownDeg) {
                shownDeg = d;
                degText = d + "°";
            }
            r.text(degText, half - r.textWidth(degText) / 2f, 12, style.textColor(), style.textShadow());
        }
    }
}
