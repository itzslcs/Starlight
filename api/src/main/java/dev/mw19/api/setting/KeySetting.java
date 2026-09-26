package dev.mw19.api.setting;

/**
 * A key binding. Codes are GLFW key constants on every target (the 1.8.9 adapter translates);
 * mouse buttons are {@code MOUSE_BASE + button}; {@link #NONE} means unbound.
 */
public final class KeySetting extends Setting<Integer> {
    public static final int NONE = -1;
    public static final int MOUSE_BASE = 1000;

    public KeySetting(String id, String name, String description, int def) {
        super(id, name, description, def);
    }

    public int key() {
        return value;
    }

    public boolean bound() {
        return value != NONE;
    }

    @Override
    public Object toJson() {
        return value.doubleValue();
    }

    @Override
    public void fromJson(Object json) {
        if (json instanceof Number) set(((Number) json).intValue());
    }
}
