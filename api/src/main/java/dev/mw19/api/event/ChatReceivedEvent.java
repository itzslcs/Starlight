package dev.mw19.api.event;

/** A chat/system line about to be shown. Reused instance: do not keep a reference. */
public final class ChatReceivedEvent {
    /** Text without formatting codes. */
    public String plain;
    /** Text with legacy '§' codes. */
    public String formatted;
    private boolean cancelled;

    public void reset(String plain, String formatted) {
        this.plain = plain;
        this.formatted = formatted;
        this.cancelled = false;
    }

    /** Hide the line locally (never affects what the server sees). */
    public void cancel() {
        cancelled = true;
    }

    public boolean cancelled() {
        return cancelled;
    }
}
