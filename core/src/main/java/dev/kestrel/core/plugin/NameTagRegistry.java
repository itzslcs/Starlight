package dev.kestrel.core.plugin;

import dev.kestrel.api.Subscription;
import dev.kestrel.api.name.NameDecorator;
import dev.kestrel.api.name.NameTags;
import dev.kestrel.core.Log;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Collects decorators from plugins. Results are cached per player and placement for 500 ms, so a decorator is called
 * at most twice a second per visible player, however often the game asks.
 */
public final class NameTagRegistry implements NameTags {
    private static final long TTL = 500;
    private volatile NameDecorator[] decorators = new NameDecorator[0];
    private final Map<UUID, Object[]> cache = new HashMap<UUID, Object[]>();

    @Override
    public synchronized Subscription register(final NameDecorator d) {
        NameDecorator[] next = new NameDecorator[decorators.length + 1];
        System.arraycopy(decorators, 0, next, 0, decorators.length);
        next[decorators.length] = d;
        decorators = next;
        cache.clear();
        return new Subscription() {
            @Override
            public void cancel() {
                remove(d);
            }
        };
    }

    private synchronized void remove(NameDecorator d) {
        NameDecorator[] cur = decorators;
        int n = 0;
        NameDecorator[] next = new NameDecorator[Math.max(0, cur.length - 1)];
        for (NameDecorator x : cur) if (x != d && n < next.length) next[n++] = x;
        decorators = n == next.length ? next : java.util.Arrays.copyOf(next, n);
        cache.clear();
    }

    public boolean isEmpty() {
        return decorators.length == 0;
    }

    /** Combined suffix (leading space included) or null. Game thread. */
    public String suffix(UUID uuid, String name, NameDecorator.Placement placement) {
        NameDecorator[] ds = decorators;
        if (ds.length == 0 || uuid == null) return null;
        long now = System.currentTimeMillis();
        Object[] slot = cache.get(uuid);
        int i = placement.ordinal() * 2;
        if (slot != null && slot[i + 1] != null && now - (Long) slot[i + 1] < TTL) return (String) slot[i];
        StringBuilder sb = null;
        for (NameDecorator d : ds) {
            String s;
            try {
                s = d.suffix(uuid, name, placement);
            } catch (Throwable t) {
                Log.error("name decorator failed", t);
                s = null;
            }
            if (s == null || s.isEmpty()) continue;
            if (sb == null) sb = new StringBuilder();
            sb.append(' ').append(s);
        }
        if (slot == null) {
            slot = new Object[NameDecorator.Placement.values().length * 2];
            if (cache.size() > 2048) cache.clear();
            cache.put(uuid, slot);
        }
        slot[i] = sb == null ? null : sb.toString();
        slot[i + 1] = now;
        return (String) slot[i];
    }
}
