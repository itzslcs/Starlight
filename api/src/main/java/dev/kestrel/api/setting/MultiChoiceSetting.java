package dev.kestrel.api.setting;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** Any subset of a fixed list of options (multi-select). Selection is kept in option order. */
public final class MultiChoiceSetting extends Setting<List<String>> {
    private final List<String> options;

    public MultiChoiceSetting(String id, String name, String description, List<String> def, String... options) {
        super(id, name, description, Collections.unmodifiableList(new ArrayList<String>(def)));
        this.options = Collections.unmodifiableList(Arrays.asList(options.clone()));
    }

    @Override
    protected List<String> sanitize(List<String> v) {
        if (v == null) return defaultValue();
        List<String> out = new ArrayList<String>();
        for (String o : options) if (v.contains(o)) out.add(o);
        return Collections.unmodifiableList(out);
    }

    public List<String> options() {
        return options;
    }

    public boolean has(String option) {
        return value.contains(option);
    }

    public void toggle(String option) {
        List<String> next = new ArrayList<String>(value);
        if (!next.remove(option)) next.add(option);
        set(next);
    }

    @Override
    public Object toJson() {
        return new ArrayList<Object>(value);
    }

    @Override
    public void fromJson(Object json) {
        if (!(json instanceof List)) return;
        List<String> in = new ArrayList<String>();
        for (Object o : (List<?>) json) if (o instanceof String) in.add((String) o);
        set(in);
    }
}
