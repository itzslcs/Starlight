package dev.mw19.api.setting;

import dev.mw19.api.util.Colors;

import java.util.LinkedHashMap;
import java.util.Map;

/** ARGB colour with optional chroma (hue cycling). */
public final class ColorSetting extends Setting<Integer> {
    private final boolean defChroma;
    private boolean chroma;
    private float chromaSpeed = 1f;

    public ColorSetting(String id, String name, String description, int defArgb, boolean defChroma) {
        super(id, name, description, defArgb);
        this.defChroma = defChroma;
        this.chroma = defChroma;
    }

    public ColorSetting(String id, String name, String description, int defArgb) {
        this(id, name, description, defArgb, false);
    }

    public boolean chroma() {
        return chroma;
    }

    public void setChroma(boolean on) {
        if (chroma == on) return;
        chroma = on;
        fireChanged();
    }

    public float chromaSpeed() {
        return chromaSpeed;
    }

    public void setChromaSpeed(float speed) {
        float s = Math.max(0.1f, Math.min(5f, speed));
        if (s == chromaSpeed) return;
        chromaSpeed = s;
        fireChanged();
    }

    /** The colour to draw right now (chroma cycles the hue, keeping the stored alpha/saturation feel). */
    public int argb(long millis) {
        if (!chroma) return value;
        float hue = (millis * chromaSpeed % 4000L) / 4000f;
        return (value & 0xFF000000) | (Colors.hsb(hue, 0.75f, 1f) & 0xFFFFFF);
    }

    @Override
    protected void resetExtras() {
        chroma = defChroma;
        chromaSpeed = 1f;
    }

    @Override
    public Object toJson() {
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        m.put("argb", (double) (value & 0xFFFFFFFFL));
        m.put("chroma", chroma);
        m.put("speed", (double) chromaSpeed);
        return m;
    }

    @Override
    public void fromJson(Object json) {
        if (json instanceof Number) {
            set((int) ((Number) json).longValue());
        } else if (json instanceof Map) {
            Map<?, ?> m = (Map<?, ?>) json;
            if (m.get("argb") instanceof Number) set((int) ((Number) m.get("argb")).longValue());
            if (m.get("chroma") instanceof Boolean) chroma = (Boolean) m.get("chroma");
            if (m.get("speed") instanceof Number) chromaSpeed = ((Number) m.get("speed")).floatValue();
        }
    }
}
