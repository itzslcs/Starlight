package dev.mw19.core.module;

import dev.mw19.api.module.HudModule;
import dev.mw19.api.module.Module;
import dev.mw19.core.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Owns every module's runtime state. A module is ACTIVE when the user enabled it and no suspension applies.
 * All lifecycle callbacks go through {@link #guard}: 5 failures disable the module for the session.
 */
public final class ModuleManager {
    public static final int SUSPEND_SERVER = 1, SUSPEND_SAFE = 2, SUSPEND_FAILED = 4, SUSPEND_UNAVAILABLE = 8;
    /**
     * Held until the game is fully constructed. Fabric runs mod entrypoints before Minecraft has loaded its options,
     * so a module enabled by the saved profile could not touch game state in onEnable (debug-log 2026-09-26).
     */
    public static final int SUSPEND_STARTING = 16;
    private boolean starting;
    public static final int MAX_FAILURES = 5;

    public static final class State {
        public final Module module;
        public final String owner;
        boolean enabled;
        boolean favorite;
        int suspend;
        int failures;
        boolean active;
        /** Exponential moving average of ns spent per frame (render) and per tick. */
        public double renderNanos, tickNanos;
        final Runnable tickCall;

        State(final Module module, String owner) {
            this.module = module;
            this.owner = owner;
            this.tickCall = new Runnable() {
                @Override
                public void run() {
                    module.onTick();
                }
            };
        }

        public boolean enabled() {
            return enabled;
        }

        public boolean favorite() {
            return favorite;
        }

        public boolean active() {
            return active;
        }

        public int suspend() {
            return suspend;
        }
    }

    public interface Listener {
        /** Something the user can persist changed (enabled/favorite). */
        void changed(State s);

        /** A module was disabled by the error guard. */
        void failed(State s, Throwable cause);
    }

    private final Map<String, State> states = new LinkedHashMap<String, State>();
    private final List<State> order = new ArrayList<State>();
    private State[] activeTick = new State[0];
    private State[] activeHud = new State[0];
    private State[] activeOverlay = new State[0];
    private Listener listener;

    public void setListener(Listener l) {
        this.listener = l;
    }

    public State register(Module m, String owner) {
        if (states.containsKey(m.id())) throw new IllegalArgumentException("duplicate module id " + m.id());
        State s = new State(m, owner);
        // Starts disabled; the profile (or the module default via ConfigManager.applyTo) activates it.
        if (!safeAvailable(s)) s.suspend |= SUSPEND_UNAVAILABLE;
        if (starting) s.suspend |= SUSPEND_STARTING;
        states.put(m.id(), s);
        order.add(s);
        return s;
    }

    /** Removes all modules of an owner (plugin unload), disabling them first. */
    public void unregisterOwner(String owner) {
        for (int i = order.size() - 1; i >= 0; i--) {
            State s = order.get(i);
            if (!s.owner.equals(owner)) continue;
            setActive(s, false);
            order.remove(i);
            states.remove(s.module.id());
        }
        rebuild();
    }

    private boolean safeAvailable(final State s) {
        final boolean[] out = {false};
        guard(s, "available", new Runnable() {
            @Override
            public void run() {
                out[0] = s.module.available();
            }
        });
        return out[0];
    }

    public State get(String id) {
        return states.get(id);
    }

    public List<State> all() {
        return Collections.unmodifiableList(order);
    }

    /** User intent. Does not touch suspensions. */
    public void setEnabled(State s, boolean on) {
        if (s.enabled == on) return;
        s.enabled = on;
        apply(s);
        if (listener != null) listener.changed(s);
    }

    /** Applies a profile's value without firing "changed" per module (caller saves once). */
    public void loadEnabled(State s, boolean on, boolean favorite) {
        s.favorite = favorite;
        if (s.enabled == on) return;
        s.enabled = on;
        apply(s);
    }

    public void setFavorite(State s, boolean on) {
        if (s.favorite == on) return;
        s.favorite = on;
        if (listener != null) listener.changed(s);
    }

    public void setSuspended(State s, int flag, boolean on) {
        int next = on ? (s.suspend | flag) : (s.suspend & ~flag);
        if (next == s.suspend) return;
        s.suspend = next;
        apply(s);
    }

    /** Holds every module registered from now on until {@link #gameReady()} (the client calls this before loading). */
    public void holdUntilGameReady() {
        starting = true;
    }

    /** First client tick: the game exists, so modules the profile enabled start now. */
    /** Returns true the one time it releases the modules (the first game tick). */
    public boolean gameReady() {
        if (!starting) return false;
        starting = false;
        for (State s : order) setSuspended(s, SUSPEND_STARTING, false);
        return true;
    }

    /** Clears FAILED on every module (e.g. after a profile switch, the user gets another try). */
    public void clearFailures() {
        for (State s : order) {
            s.failures = 0;
            setSuspended(s, SUSPEND_FAILED, false);
        }
    }

    private void apply(State s) {
        setActive(s, s.enabled && s.suspend == 0);
        rebuild();
    }

    private void setActive(final State s, boolean on) {
        if (s.active == on) return;
        s.active = on;
        if (on) {
            guard(s, "enable", new Runnable() {
                @Override
                public void run() {
                    s.module.onEnable();
                }
            });
        } else {
            guard(s, "disable", new Runnable() {
                @Override
                public void run() {
                    s.module.onDisable();
                }
            });
        }
    }

    private void rebuild() {
        List<State> t = new ArrayList<State>(), h = new ArrayList<State>(), o = new ArrayList<State>();
        for (State s : order) {
            if (!s.active) continue;
            t.add(s);
            if (s.module instanceof HudModule) h.add(s);
            if (s.module instanceof Overlay) o.add(s);
        }
        activeTick = t.toArray(new State[0]);
        activeHud = h.toArray(new State[0]);
        activeOverlay = o.toArray(new State[0]);
    }

    /** Active modules implementing {@link Overlay}. */
    public State[] activeOverlay() {
        return activeOverlay;
    }

    /** Active HUD modules in registration order. Array is replaced (never mutated) on change. */
    public State[] activeHud() {
        return activeHud;
    }

    public void tick() {
        State[] arr = activeTick;
        for (final State s : arr) {
            if (!s.active) continue;
            long t0 = System.nanoTime();
            guard(s, "tick", s.tickCall);
            s.tickNanos = s.tickNanos * 0.95 + (System.nanoTime() - t0) * 0.05;
        }
    }

    /**
     * Runs {@code r} for module {@code s}; on failure counts it and disables the module after MAX_FAILURES.
     * Returns false if the call threw.
     */
    public boolean guard(State s, String phase, Runnable r) {
        if (r == null) return true;
        try {
            r.run();
            return true;
        } catch (VirtualMachineError e) {
            throw e;
        } catch (Throwable t) {
            s.failures++;
            Log.error("module " + s.module.id() + " failed in " + phase + " (" + s.failures + "/" + MAX_FAILURES + ")", t);
            if (s.failures >= MAX_FAILURES && (s.suspend & SUSPEND_FAILED) == 0) {
                Log.warn("module " + s.module.id() + " disabled after repeated errors");
                s.suspend |= SUSPEND_FAILED;
                if (s.active) {
                    s.active = false;
                    try {
                        s.module.onDisable();
                    } catch (Throwable ignored) {
                        // already failing; do not recurse
                    }
                }
                rebuild();
                if (listener != null) listener.failed(s, t);
            }
            return false;
        }
    }

    public String describeEnabled() {
        StringBuilder sb = new StringBuilder();
        for (State s : order) {
            if (!s.enabled) continue;
            if (sb.length() > 0) sb.append(", ");
            sb.append(s.module.id());
            if (!s.active) sb.append("(suspended:").append(s.suspend).append(')');
        }
        return sb.length() == 0 ? "(none)" : sb.toString();
    }
}
