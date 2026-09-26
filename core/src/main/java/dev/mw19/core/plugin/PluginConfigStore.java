package dev.mw19.core.plugin;

import dev.mw19.api.ConfigStore;
import dev.mw19.api.Scheduler;
import dev.mw19.api.util.Json;
import dev.mw19.core.Log;
import dev.mw19.core.config.AtomicFiles;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/** plugin-data/<id>.json; saved (atomically) a few ticks after the last change and on disable. */
final class PluginConfigStore implements ConfigStore {
    private final Path file;
    private final Scheduler scheduler;
    private final Map<String, Object> values = new LinkedHashMap<String, Object>();
    private boolean dirty, scheduled;

    PluginConfigStore(Path file, Scheduler scheduler) {
        this.file = file;
        this.scheduler = scheduler;
        try {
            if (Files.exists(file)) values.putAll(Json.obj(Json.parse(AtomicFiles.read(file))));
        } catch (Exception e) {
            Log.warn("plugin config " + file.getFileName() + " unreadable, starting empty: " + e);
        }
    }

    private void changed() {
        dirty = true;
        if (scheduled) return;
        scheduled = true;
        scheduler.runLater(40, new Runnable() {
            @Override
            public void run() {
                scheduled = false;
                flush();
            }
        });
    }

    void flush() {
        if (!dirty) return;
        dirty = false;
        try {
            AtomicFiles.write(file, Json.write(values, true));
        } catch (Exception e) {
            Log.error("could not save " + file, e);
        }
    }

    @Override
    public String getString(String key, String def) {
        return Json.str(values, key, def);
    }

    @Override
    public void setString(String key, String value) {
        values.put(key, value);
        changed();
    }

    @Override
    public boolean getBoolean(String key, boolean def) {
        return Json.bool(values, key, def);
    }

    @Override
    public void setBoolean(String key, boolean value) {
        values.put(key, value);
        changed();
    }

    @Override
    public double getNumber(String key, double def) {
        return Json.num(values, key, def);
    }

    @Override
    public void setNumber(String key, double value) {
        values.put(key, value);
        changed();
    }
}
