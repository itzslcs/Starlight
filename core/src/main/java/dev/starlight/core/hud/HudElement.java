package dev.starlight.core.hud;

import dev.starlight.api.hud.Anchor;
import dev.starlight.api.hud.HudStyle;
import dev.starlight.api.util.Json;

import java.util.LinkedHashMap;
import java.util.Map;

/** Persisted placement + look of one HUD element. Offsets point inwards from the anchor. */
public final class HudElement implements HudStyle {
    public Anchor anchor;
    public boolean autoAnchor = true;
    /** Offset in GUI units, or fraction of the screen when {@link #percent}. */
    public float x, y;
    public boolean percent;
    public float scale = 1f;
    public float opacity = 1f;
    public boolean background = true;
    public int bgColor = 0x90101216;
    public boolean border;
    public int borderColor = 0x40FFFFFF;
    public boolean shadow = true;
    public float radius = 3f;
    public float padding = 3f;
    public int textColor = 0xFFFFFFFF;
    public int accentColor = 0xFFFF8A3D;

    /** Absolute box of the last layout pass (GUI units, includes padding, after scale). */
    public float bx, by, bw, bh;

    public HudElement(Anchor anchor, float x, float y) {
        this.anchor = anchor;
        this.x = x;
        this.y = y;
    }

    public HudElement copy() {
        HudElement e = new HudElement(anchor, x, y);
        e.fromJson(toJson());
        return e;
    }

    @Override
    public int textColor() {
        return textColor;
    }

    @Override
    public int accentColor() {
        return accentColor;
    }

    @Override
    public boolean textShadow() {
        return shadow;
    }

    public Map<String, Object> toJson() {
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        m.put("anchor", anchor.name());
        m.put("autoAnchor", autoAnchor);
        m.put("x", (double) x);
        m.put("y", (double) y);
        m.put("percent", percent);
        m.put("scale", (double) scale);
        m.put("opacity", (double) opacity);
        m.put("background", background);
        m.put("bgColor", (double) (bgColor & 0xFFFFFFFFL));
        m.put("border", border);
        m.put("borderColor", (double) (borderColor & 0xFFFFFFFFL));
        m.put("shadow", shadow);
        m.put("radius", (double) radius);
        m.put("padding", (double) padding);
        m.put("textColor", (double) (textColor & 0xFFFFFFFFL));
        m.put("accentColor", (double) (accentColor & 0xFFFFFFFFL));
        return m;
    }

    public void fromJson(Map<String, Object> m) {
        try {
            anchor = Anchor.valueOf(Json.str(m, "anchor", anchor.name()));
        } catch (IllegalArgumentException ignored) {
            // keep current
        }
        autoAnchor = Json.bool(m, "autoAnchor", autoAnchor);
        percent = Json.bool(m, "percent", percent);
        x = clampOffset((float) Json.num(m, "x", x));
        y = clampOffset((float) Json.num(m, "y", y));
        scale = clamp((float) Json.num(m, "scale", scale), 0.25f, 4f);
        opacity = clamp((float) Json.num(m, "opacity", opacity), 0.05f, 1f);
        background = Json.bool(m, "background", background);
        bgColor = (int) (long) Json.num(m, "bgColor", bgColor & 0xFFFFFFFFL);
        border = Json.bool(m, "border", border);
        borderColor = (int) (long) Json.num(m, "borderColor", borderColor & 0xFFFFFFFFL);
        shadow = Json.bool(m, "shadow", shadow);
        radius = clamp((float) Json.num(m, "radius", radius), 0f, 12f);
        padding = clamp((float) Json.num(m, "padding", padding), 0f, 12f);
        textColor = (int) (long) Json.num(m, "textColor", textColor & 0xFFFFFFFFL);
        accentColor = (int) (long) Json.num(m, "accentColor", accentColor & 0xFFFFFFFFL);
    }

    private float clampOffset(float v) {
        return percent ? clamp(v, -1f, 1f) : clamp(v, -8192f, 8192f);
    }

    static float clamp(float v, float lo, float hi) {
        return Float.isNaN(v) ? lo : Math.max(lo, Math.min(hi, v));
    }
}
