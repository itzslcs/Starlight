package dev.kestrel.core;

import dev.kestrel.api.Logger;

/** Core logging; routed to the game's logger once the platform is up, stderr before that. */
public final class Log {
    private static volatile Logger sink;

    private Log() {}

    public static void bind(Logger logger) {
        sink = logger;
    }

    public static void info(String msg) {
        Logger s = sink;
        if (s != null) s.info(msg);
        else System.out.println("[Kestrel] " + msg);
    }

    public static void warn(String msg) {
        Logger s = sink;
        if (s != null) s.warn(msg);
        else System.err.println("[Kestrel] WARN " + msg);
    }

    public static void error(String msg, Throwable t) {
        Logger s = sink;
        if (s != null) {
            s.error(msg, t);
        } else {
            System.err.println("[Kestrel] ERROR " + msg);
            if (t != null) t.printStackTrace();
        }
    }
}
