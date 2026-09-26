package dev.mw19.api.event;

import dev.mw19.api.Subscription;

import java.util.function.Consumer;

public interface Events {
    /** Handlers run on the game thread; exceptions are caught and logged. */
    <E> Subscription on(Class<E> type, Consumer<? super E> handler);
}
