package dev.kestrel.api;

public interface Scheduler {
    /** Runs on the game thread at the start of the next client tick. Safe from any thread. */
    void runOnMain(Runnable task);

    /** Runs on the game thread after {@code ticks} client ticks. */
    void runLater(int ticks, Runnable task);

    /** Repeats every {@code ticks} client ticks on the game thread until the subscription is cancelled. */
    Subscription every(int ticks, Runnable task);

    /** Runs on a background worker. Never touch game state from here. */
    void runAsync(Runnable task);
}
