package dev.kestrel.api.net;

/**
 * Background HTTP GET for JSON with an in-memory LRU+TTL cache and per-host rate limiting.
 * Network access only happens while the calling module is enabled (opt-in).
 */
public interface Http {
    /** Callback runs on the game thread. 2xx and 404 responses are cached for {@code ttlMillis}. */
    void getJson(String url, long ttlMillis, Callback callback);

    interface Callback {
        /** {@code json} is the parsed body (Map/List/String/Double/Boolean) or null; {@code error} is null on success. */
        void done(int status, Object json, String error);
    }
}
