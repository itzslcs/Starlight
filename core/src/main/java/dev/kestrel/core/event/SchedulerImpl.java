package dev.kestrel.core.event;

import dev.kestrel.api.Scheduler;
import dev.kestrel.api.Subscription;
import dev.kestrel.core.Guard;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public final class SchedulerImpl implements Scheduler {
    private final ConcurrentLinkedQueue<Runnable> mainQueue = new ConcurrentLinkedQueue<Runnable>();
    private final List<Timed> timed = new ArrayList<Timed>();
    private final List<Timed> adding = new ArrayList<Timed>();
    private final ExecutorService async;
    private long tick;

    private static final class Timed {
        long due;
        final int period; // 0 = one-shot
        final Runnable task;
        boolean cancelled;

        Timed(long due, int period, Runnable task) {
            this.due = due;
            this.period = period;
            this.task = task;
        }
    }

    public SchedulerImpl() {
        final AtomicInteger n = new AtomicInteger();
        async = Executors.newFixedThreadPool(2, new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r, "Kestrel-Worker-" + n.incrementAndGet());
                t.setDaemon(true);
                t.setPriority(Thread.NORM_PRIORITY - 1);
                return t;
            }
        });
    }

    @Override
    public void runOnMain(Runnable task) {
        mainQueue.add(task);
    }

    @Override
    public void runLater(int ticks, Runnable task) {
        synchronized (adding) {
            adding.add(new Timed(tick + Math.max(1, ticks), 0, task));
        }
    }

    @Override
    public Subscription every(int ticks, Runnable task) {
        final Timed t = new Timed(tick + Math.max(1, ticks), Math.max(1, ticks), task);
        synchronized (adding) {
            adding.add(t);
        }
        return new Subscription() {
            @Override
            public void cancel() {
                t.cancelled = true;
            }
        };
    }

    @Override
    public void runAsync(final Runnable task) {
        async.execute(new Runnable() {
            @Override
            public void run() {
                Guard.run("async task", task);
            }
        });
    }

    /** Game thread, start of every client tick. */
    public void tick() {
        tick++;
        Runnable r;
        int budget = 256; // never stall a tick on a flood of callbacks
        while (budget-- > 0 && (r = mainQueue.poll()) != null) Guard.run("scheduled task", r);
        synchronized (adding) {
            if (!adding.isEmpty()) {
                timed.addAll(adding);
                adding.clear();
            }
        }
        for (int i = timed.size() - 1; i >= 0; i--) {
            Timed t = timed.get(i);
            if (t.cancelled) {
                timed.remove(i);
            } else if (tick >= t.due) {
                Guard.run("timed task", t.task);
                if (t.period > 0) t.due = tick + t.period;
                else timed.remove(i);
            }
        }
    }

    public void shutdown() {
        async.shutdownNow();
    }
}
