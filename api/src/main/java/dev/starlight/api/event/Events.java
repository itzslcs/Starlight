package dev.starlight.api.event;

import dev.starlight.api.Subscription;

import java.util.function.Consumer;

public interface Events {
    /** Handlers run on the game thread; exceptions are caught and logged. */
    <E> Subscription on(Class<E> type, Consumer<? super E> handler);
}
