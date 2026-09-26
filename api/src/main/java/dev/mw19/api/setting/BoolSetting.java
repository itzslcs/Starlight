package dev.mw19.api.setting;

public final class BoolSetting extends Setting<Boolean> {
    public BoolSetting(String id, String name, String description, boolean def) {
        super(id, name, description, def);
    }

    public boolean on() {
        return value;
    }

    public void toggle() {
        set(!value);
    }

    @Override
    public Object toJson() {
        return value;
    }

    @Override
    public void fromJson(Object json) {
        if (json instanceof Boolean) set((Boolean) json);
    }
}
