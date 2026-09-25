package dev.kestrel.api.event;

/** Fired at the start and end of every client tick (20/s). Reused instance: do not keep a reference. */
public final class ClientTickEvent {
    public boolean end;
}
