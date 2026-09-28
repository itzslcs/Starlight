package dev.starlight.api.setting;

public final class TextSetting extends Setting<String> {
    private final int maxLength;

    public TextSetting(String id, String name, String description, String def, int maxLength) {
        super(id, name, description, def);
        this.maxLength = maxLength;
    }

    @Override
    protected String sanitize(String v) {
        if (v == null) return defaultValue();
        return v.length() > maxLength ? v.substring(0, maxLength) : v;
    }

    public int maxLength() {
        return maxLength;
    }

    @Override
    public Object toJson() {
        return value;
    }

    @Override
    public void fromJson(Object json) {
        if (json instanceof String) set((String) json);
    }
}
