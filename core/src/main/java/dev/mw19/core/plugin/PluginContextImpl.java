package dev.mw19.core.plugin;

import dev.mw19.api.ConfigStore;
import dev.mw19.api.Gui;
import dev.mw19.api.Mw19Api;
import dev.mw19.api.Logger;
import dev.mw19.api.PluginContext;
import dev.mw19.api.Scheduler;
import dev.mw19.api.Subscription;
import dev.mw19.api.event.Events;
import dev.mw19.api.game.Game;
import dev.mw19.api.module.Module;
import dev.mw19.api.name.NameDecorator;
import dev.mw19.api.name.NameTags;
import dev.mw19.api.net.Http;
import dev.mw19.core.Mw19;
import dev.mw19.core.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * What one plugin sees. Every subscription, module, decorator and panel it creates is tracked so disabling the plugin
 * removes all of it. Deliberately offers no way to reach the session/access token.
 */
final class PluginContextImpl implements PluginContext {
    private final Mw19 k;
    private final PluginDescriptor d;
    private final String owner;
    private final List<Subscription> subs = new ArrayList<Subscription>();
    private final PluginConfigStore config;
    private boolean closed;

    PluginContextImpl(Mw19 k, PluginDescriptor d) {
        this.k = k;
        this.d = d;
        this.owner = "plugin:" + d.id;
        this.config = new PluginConfigStore(k.config.pluginDataDir.resolve(d.id + ".json"), k.scheduler);
    }

    private Subscription track(Subscription s) {
        synchronized (subs) {
            if (closed) {
                s.cancel();
            } else {
                subs.add(s);
            }
        }
        return s;
    }

    void close() {
        List<Subscription> copy;
        synchronized (subs) {
            closed = true;
            copy = new ArrayList<Subscription>(subs);
            subs.clear();
        }
        for (Subscription s : copy) {
            try {
                s.cancel();
            } catch (Throwable t) {
                Log.error("plugin " + d.id + " cleanup", t);
            }
        }
        k.modules.unregisterOwner(owner);
        config.flush();
    }

    @Override
    public String pluginId() {
        return d.id;
    }

    @Override
    public String apiVersion() {
        return Mw19Api.VERSION;
    }

    private final Logger logger = new Logger() {
        @Override
        public void info(String msg) {
            Log.info("[" + d.id + "] " + msg);
        }

        @Override
        public void warn(String msg) {
            Log.warn("[" + d.id + "] " + msg);
        }

        @Override
        public void error(String msg, Throwable t) {
            Log.error("[" + d.id + "] " + msg, t);
        }
    };

    @Override
    public Logger logger() {
        return logger;
    }

    private final Events events = new Events() {
        @Override
        public <E> Subscription on(Class<E> type, Consumer<? super E> handler) {
            return track(k.events.on(type, handler, owner));
        }
    };

    @Override
    public Events events() {
        return events;
    }

    @Override
    public Game game() {
        return k.platform;
    }

    private final Scheduler scheduler = new Scheduler() {
        @Override
        public void runOnMain(Runnable task) {
            k.scheduler.runOnMain(task);
        }

        @Override
        public void runLater(int ticks, Runnable task) {
            k.scheduler.runLater(ticks, task);
        }

        @Override
        public Subscription every(int ticks, Runnable task) {
            return track(k.scheduler.every(ticks, task));
        }

        @Override
        public void runAsync(Runnable task) {
            k.scheduler.runAsync(task);
        }
    };

    @Override
    public Scheduler scheduler() {
        return scheduler;
    }

    @Override
    public ConfigStore config() {
        return config;
    }

    private final NameTags nameTags = new NameTags() {
        @Override
        public Subscription register(NameDecorator decorator) {
            return track(k.nameTags.register(decorator));
        }
    };

    @Override
    public NameTags nameTags() {
        return nameTags;
    }

    @Override
    public Http http() {
        return k.http;
    }

    private final Gui gui = new Gui() {
        @Override
        public Subscription registerPanel(String title, Panel panel) {
            return track(k.panels.add(d.name + ": " + title, panel));
        }

        @Override
        public void toast(String title, String message) {
            k.toast(title, message, k.theme.accent);
        }
    };

    @Override
    public Gui gui() {
        return gui;
    }

    @Override
    public void registerModule(Module module) {
        k.register(module, owner);
    }
}
