package dev.starlight.core.event;

/** Mouse wheel while no screen is open. Set {@code consumed} to stop vanilla (hotbar scroll). Reused instance. */
public final class ScrollEvent {
    public double amount;
    public boolean consumed;
}
