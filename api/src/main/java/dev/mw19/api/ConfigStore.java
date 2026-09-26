package dev.mw19.api;

/** Small persistent key/value store private to one plugin. Saved automatically. */
public interface ConfigStore {
    String getString(String key, String def);

    void setString(String key, String value);

    boolean getBoolean(String key, boolean def);

    void setBoolean(String key, boolean value);

    double getNumber(String key, double def);

    void setNumber(String key, double value);
}
