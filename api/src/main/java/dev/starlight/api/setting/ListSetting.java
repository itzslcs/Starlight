package dev.starlight.api.setting;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Free-form list of strings (list editor), e.g. chat filter words. */
public final class ListSetting extends Setting<List<String>> {
    private final int maxEntries;

    public ListSetting(String id, String name, String description, List<String> def, int maxEntries) {
        super(id, name, description, Collections.unmodifiableList(new ArrayList<String>(def)));
        this.maxEntries = maxEntries;
    }

    @Override
    protected List<String> sanitize(List<String> v) {
        if (v == null) return defaultValue();
        List<String> out = new ArrayList<String>();
        for (String s : v) {
            if (s != null && out.size() < maxEntries) out.add(s.length() > 256 ? s.substring(0, 256) : s);
        }
        return Collections.unmodifiableList(out);
    }

    public int maxEntries() {
        return maxEntries;
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
