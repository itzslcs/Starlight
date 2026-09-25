package dev.kestrel.api;

public interface Logger {
    void info(String msg);

    void warn(String msg);

    void error(String msg, Throwable t);
}
