package dev.mw19.core.platform;

public interface ChatAccess {
    /** Local-only line (never sent). '§' codes allowed. */
    void showLocal(String formatted);

    /** Sends a chat message as the player. Only ever called in direct response to a user action or an opt-in GRAY module. */
    void sendMessage(String message);

    /** Sends "/command" (without the slash). Same restrictions as {@link #sendMessage}. */
    void sendCommand(String command);
}
