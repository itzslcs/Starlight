package dev.starlight.api.module;

import dev.starlight.api.setting.Setting;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A toggleable feature. The host owns the lifecycle: it calls {@link #onEnable()} / {@link #onDisable()} when the
 * module becomes active or inactive (user toggle, profile switch, server rule, competitive-safe, error guard) and
 * {@link #onTick()} every client tick while active. Every callback is guarded: throwing repeatedly disables the module.
 */
public abstract class Module {
    private final String id;
    private final String name;
    private final String description;
    private final Category category;
    private final Rule rule;
    private final boolean defaultEnabled;
    private final List<Setting<?>> settings = new ArrayList<Setting<?>>();

    protected Module(String id, String name, String description, Category category, Rule rule, boolean defaultEnabled) {
        if (!id.matches("[a-z0-9_]{1,48}")) throw new IllegalArgumentException("bad module id: " + id);
        this.id = id;
        this.name = name;
        this.description = description;
        this.category = category;
        this.rule = rule;
        // GRAY/DISALLOWED never default on (docs/RULES_MATRIX.md).
        this.defaultEnabled = defaultEnabled && rule == Rule.ALLOWED;
    }

    protected final <S extends Setting<?>> S add(S setting) {
        for (Setting<?> s : settings) {
            if (s.id().equals(setting.id())) throw new IllegalArgumentException(id + ": duplicate setting " + setting.id());
        }
        settings.add(setting);
        return setting;
    }

    public void onEnable() {}

    public void onDisable() {}

    public void onTick() {}

    /** False hides the module on this target (e.g. shield tweaks on 1.8.9). */
    public boolean available() {
        return true;
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

    public final Category category() {
        return category;
    }

    public final Rule rule() {
        return rule;
    }

    public final boolean defaultEnabled() {
        return defaultEnabled;
    }

    public final List<Setting<?>> settings() {
        return Collections.unmodifiableList(settings);
    }
}
