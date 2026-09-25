package dev.kestrel.core.net;

import dev.kestrel.api.Scheduler;
import dev.kestrel.api.net.Http;
import dev.kestrel.api.util.Json;
import dev.kestrel.core.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 * Background JSON GETs with an LRU+TTL cache, request coalescing, per-host spacing and Retry-After backoff.
 * Only modules the user enabled call this (network is opt-in; see README "Network use").
 */
public final class HttpClient implements Http {
    private static final int MAX_ENTRIES = 1024, MAX_BODY = 4 << 20;
    private static final long MIN_SPACING_MS = 120, ERROR_TTL_MS = 60_000;

    private static final class Cached {
        final long expires;
        final int status;
        final Object json;
        final String error;

        Cached(long expires, int status, Object json, String error) {
            this.expires = expires;
            this.status = status;
            this.json = json;
            this.error = error;
        }
    }

    private final Map<String, Cached> cache = new LinkedHashMap<String, Cached>(64, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Cached> e) {
            return size() > MAX_ENTRIES;
        }
    };
    private final Map<String, List<Callback>> inflight = new HashMap<String, List<Callback>>();
    private final Map<String, Long> hostNext = new HashMap<String, Long>();
    private final ExecutorService pool;
    private final Scheduler main;
    private final String userAgent;

    public HttpClient(Scheduler main, String version) {
        this.main = main;
        this.userAgent = "Kestrel/" + version + " (Minecraft client mod)";
        this.pool = Executors.newFixedThreadPool(2, new ThreadFactory() {
            private int n;

            @Override
            public synchronized Thread newThread(Runnable r) {
                Thread t = new Thread(r, "Kestrel-Net-" + (++n));
                t.setDaemon(true);
                return t;
            }
        });
    }

    @Override
    public void getJson(final String url, final long ttlMillis, final Callback callback) {
        if (!url.startsWith("https://") && !url.startsWith("http://")) {
            callback.done(0, null, "unsupported URL");
            return;
        }
        synchronized (this) {
            Cached e = cache.get(url);
            if (e != null && e.expires > System.currentTimeMillis()) {
                deliver(callback, e);
                return;
            }
            List<Callback> waiting = inflight.get(url);
            if (waiting != null) {
                waiting.add(callback);
                return;
            }
            List<Callback> list = new ArrayList<Callback>();
            list.add(callback);
            inflight.put(url, list);
        }
        pool.execute(new Runnable() {
            @Override
            public void run() {
                Cached e = fetch(url, ttlMillis);
                List<Callback> list;
                synchronized (HttpClient.this) {
                    cache.put(url, e);
                    list = inflight.remove(url);
                }
                if (list != null) for (Callback c : list) deliver(c, e);
            }
        });
    }

    private void deliver(final Callback c, final Cached e) {
        main.runOnMain(new Runnable() {
            @Override
            public void run() {
                c.done(e.status, e.json, e.error);
            }
        });
    }

    private Cached fetch(String url, long ttl) {
        long now = System.currentTimeMillis();
        HttpURLConnection con = null;
        try {
            URL u = new URL(url);
            waitForHost(u.getHost());
            con = (HttpURLConnection) u.openConnection();
            con.setConnectTimeout(5000);
            con.setReadTimeout(10000);
            con.setRequestProperty("User-Agent", userAgent);
            con.setRequestProperty("Accept", "application/json");
            int status = con.getResponseCode();
            if (status == 429 || status == 503) {
                long retry = parseRetryAfter(con.getHeaderField("Retry-After"));
                backoff(u.getHost(), retry);
                return new Cached(now + Math.max(retry, ERROR_TTL_MS), status, null, "rate limited");
            }
            InputStream in = status >= 400 ? con.getErrorStream() : con.getInputStream();
            String body = in == null ? "" : read(in);
            Object json = null;
            try {
                json = body.isEmpty() ? null : Json.parse(body);
            } catch (IllegalArgumentException ignored) {
                // non-JSON error pages are fine: status tells the story
            }
            boolean cacheable = (status >= 200 && status < 300) || status == 404;
            return new Cached(now + (cacheable ? ttl : ERROR_TTL_MS), status, json, status >= 400 && status != 404 ? "HTTP " + status : null);
        } catch (IOException ex) {
            return new Cached(now + ERROR_TTL_MS, 0, null, "offline: " + ex.getClass().getSimpleName());
        } catch (RuntimeException ex) {
            Log.warn("http " + url + " failed: " + ex);
            return new Cached(now + ERROR_TTL_MS, 0, null, ex.toString());
        } finally {
            if (con != null) con.disconnect();
        }
    }

    private static String read(InputStream in) throws IOException {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) {
                out.write(buf, 0, n);
                if (out.size() > MAX_BODY) throw new IOException("response too large");
            }
            return new String(out.toByteArray(), StandardCharsets.UTF_8);
        } finally {
            in.close();
        }
    }

    private static long parseRetryAfter(String v) {
        try {
            return v == null ? 30_000 : Math.min(600_000, Long.parseLong(v.trim()) * 1000);
        } catch (NumberFormatException e) {
            return 30_000;
        }
    }

    private void backoff(String host, long ms) {
        synchronized (hostNext) {
            hostNext.put(host, System.currentTimeMillis() + ms);
        }
    }

    /** Spaces requests to one host (worker threads only). */
    private void waitForHost(String host) throws IOException {
        long wait;
        synchronized (hostNext) {
            long now = System.currentTimeMillis();
            Long next = hostNext.get(host);
            long at = next == null ? now : Math.max(now, next);
            hostNext.put(host, at + MIN_SPACING_MS);
            wait = at - now;
        }
        if (wait > 60_000) throw new IOException("host backing off");
        if (wait > 0) {
            try {
                Thread.sleep(wait);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("interrupted");
            }
        }
    }

    public void shutdown() {
        pool.shutdownNow();
    }
}
