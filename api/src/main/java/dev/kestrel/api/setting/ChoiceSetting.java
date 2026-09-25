package dev.kestrel.api.setting;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** One of a fixed list of options (dropdown). */
public final class ChoiceSetting extends Setting<String> {
    private final List<String> options;

    public ChoiceSetting(String id, String name, String description, String def, String... options) {
        super(id, name, description, def);
        this.options = Collections.unmodifiableList(Arrays.asList(options.clone()));
        if (!this.options.contains(def)) throw new IllegalArgumentException(id + ": default not an option");
    }

    @Override
    protected String sanitize(String v) {
        return v != null && options.contains(v) ? v : defaultValue();
    }

    public List<String> options() {
        return options;
    }

    public boolean is(String option) {
        return value.equals(option);
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
