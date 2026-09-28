package dev.starlight.api.setting;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;

/**
 * A persisted, user-editable value. JSON form uses plain Java types only
 * (Boolean, Double, String, List, Map) so every target shares one codec.
 */
public abstract class Setting<T> {
    private final String id;
    private final String name;
    private final String description;
    private final T defaultValue;
    protected T value;
    private final List<Runnable> listeners = new ArrayList<Runnable>(2);
    private BooleanSupplier visible;

    protected Setting(String id, String name, String description, T defaultValue) {
        this.id = id;
        this.name = name;
        this.description = description == null ? "" : description;
        this.defaultValue = defaultValue;
        this.value = defaultValue;
    }

    public final T get() {
        return value;
    }

    public final void set(T newValue) {
        T clean = sanitize(newValue);
        if (clean == null ? value == null : clean.equals(value)) return;
        value = clean;
        fireChanged();
    }

    /** Clamp/validate; return the default for garbage. */
    protected T sanitize(T v) {
        return v == null ? defaultValue : v;
    }

    protected final void fireChanged() {
        for (int i = 0; i < listeners.size(); i++) listeners.get(i).run();
    }

    public final void reset() {
        set(defaultValue);
        resetExtras();
    }

    protected void resetExtras() {}

    public final T defaultValue() {
        return defaultValue;
    }

    public final void addListener(Runnable r) {
        listeners.add(r);
    }

    /** Only show this setting in the GUI while {@code condition} holds. */
    @SuppressWarnings("unchecked")
    public final <S extends Setting<T>> S visibleWhen(BooleanSupplier condition) {
        this.visible = condition;
        return (S) this;
    }

    public final boolean visible() {
        return visible == null || visible.getAsBoolean();
    }

    public final String id() {
        return id;
    }

    public final String name() {
        return name;
    }

    public final String description() {
        return description;
    }

    public abstract Object toJson();

    /** Must tolerate any JSON value (ignore what it cannot use). */
    public abstract void fromJson(Object json);
}
