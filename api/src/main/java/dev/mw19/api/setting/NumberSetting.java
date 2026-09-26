package dev.mw19.api.setting;

public final class NumberSetting extends Setting<Double> {
    private final double min;
    private final double max;
    private final double step;
    private final String suffix;

    public NumberSetting(String id, String name, String description, double def, double min, double max, double step, String suffix) {
        super(id, name, description, def);
        if (!(min < max) || step <= 0) throw new IllegalArgumentException(id + ": bad range");
        this.min = min;
        this.max = max;
        this.step = step;
        this.suffix = suffix == null ? "" : suffix;
    }

    public NumberSetting(String id, String name, String description, double def, double min, double max, double step) {
        this(id, name, description, def, min, max, step, "");
    }

    @Override
    protected Double sanitize(Double v) {
        if (v == null || v.isNaN() || v.isInfinite()) return defaultValue();
        double snapped = min + Math.round((v - min) / step) * step;
        return Math.max(min, Math.min(max, snapped));
    }

    public double min() {
        return min;
    }

    public double max() {
        return max;
    }

    public double step() {
        return step;
    }

    public int intValue() {
        return (int) Math.round(value);
    }

    public float floatValue() {
        return value.floatValue();
    }

    /** Position of the value in [0,1]. */
    public double fraction() {
        return (value - min) / (max - min);
    }

    public String format() {
        String num = step >= 1 ? Long.toString(Math.round(value))
                : step >= 0.1 ? String.format(java.util.Locale.ROOT, "%.1f", value)
                : String.format(java.util.Locale.ROOT, "%.2f", value);
        return num + suffix;
    }

    @Override
    public Object toJson() {
        return value;
    }

    @Override
    public void fromJson(Object json) {
        if (json instanceof Number) set(((Number) json).doubleValue());
    }
}
