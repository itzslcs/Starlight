package dev.starlight.core.event;

import dev.starlight.api.Subscription;
import dev.starlight.api.event.Events;
import dev.starlight.core.Log;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Exact-class dispatch over copy-on-write arrays: posting is a map lookup plus an indexed loop, no allocation.
 * A handler that throws 5 times is removed (it must not break other listeners or the game).
 * Registration happens on the game thread; posting too.
 */
public final class EventBus implements Events {
    private static final int MAX_FAILURES = 5;
    private final Map<Class<?>, Handler[]> handlers = new HashMap<Class<?>, Handler[]>();

    private static final class Handler {
        final Consumer<Object> fn;
        final String owner;
        int failures;

        @SuppressWarnings("unchecked")
        Handler(Consumer<?> fn, String owner) {
            this.fn = (Consumer<Object>) fn;
            this.owner = owner;
        }
    }

    @Override
    public <E> Subscription on(Class<E> type, Consumer<? super E> handler) {
        return on(type, handler, "core");
    }

    public <E> Subscription on(Class<E> type, Consumer<? super E> handler, String owner) {
        final Class<?> key = type;
        final Handler h = new Handler(handler, owner);
        Handler[] old = handlers.get(key);
        Handler[] next;
        if (old == null) {
            next = new Handler[]{h};
        } else {
            next = new Handler[old.length + 1];
            System.arraycopy(old, 0, next, 0, old.length);
            next[old.length] = h;
        }
        handlers.put(key, next);
        return new Subscription() {
            @Override
            public void cancel() {
                remove(key, h);
            }
        };
    }

    private void remove(Class<?> key, Handler h) {
        Handler[] old = handlers.get(key);
        if (old == null) return;
        int idx = -1;
        for (int i = 0; i < old.length; i++) if (old[i] == h) idx = i;
        if (idx < 0) return;
        if (old.length == 1) {
            handlers.remove(key);
            return;
        }
        Handler[] next = new Handler[old.length - 1];
        System.arraycopy(old, 0, next, 0, idx);
        System.arraycopy(old, idx + 1, next, idx, old.length - idx - 1);
        handlers.put(key, next);
    }

    public boolean hasListeners(Class<?> type) {
        return handlers.containsKey(type);
    }

    public void post(Object event) {
        Handler[] hs = handlers.get(event.getClass());
        if (hs == null) return;
        for (Handler h : hs) {
            try {
                h.fn.accept(event);
            } catch (VirtualMachineError e) {
                throw e;
            } catch (Throwable t) {
                Log.error("event handler of " + h.owner + " failed on " + event.getClass().getSimpleName(), t);
                if (++h.failures >= MAX_FAILURES) {
                    Log.warn("removing event handler of " + h.owner + " after " + MAX_FAILURES + " failures");
                    remove(event.getClass(), h);
                }
            }
        }
    }
}
