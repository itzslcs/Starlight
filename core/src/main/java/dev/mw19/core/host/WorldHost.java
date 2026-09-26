package dev.mw19.core.host;

import dev.mw19.api.net.Http;
import dev.mw19.api.util.Json;
import dev.mw19.core.Log;
import dev.mw19.core.Mw19;
import dev.mw19.core.net.Upnp;
import dev.mw19.core.platform.Host;
import dev.mw19.core.skin.SkinService;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 * Hosting the open singleplayer world: opens it to the local network (vanilla "Open to LAN"), and on request also to
 * the internet by asking the router (UPnP) to forward the port. Internet access turns the whitelist on, so only invited
 * players can join; the forward is removed when hosting stops, the world closes or the game exits.
 */
public final class WorldHost {
    public enum Net { OFF, WORKING, OPEN, FAILED }

    private static final int LEASE_S = 3600;
    private static final long RENEW_MS = 25 * 60_000L;

    private final Mw19 k;
    private final ExecutorService worker = Executors.newSingleThreadExecutor(new ThreadFactory() {
        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, "MW19-Host");
            t.setDaemon(true);
            return t;
        }
    });
    private volatile Net net = Net.OFF;
    private volatile String netMessage = "", publicAddress;
    private volatile Upnp.Gateway gateway;
    private volatile int mappedPort = -1;
    private volatile long renewAt;
    private boolean wasAvailable;
    private final List<String[]> invited = new ArrayList<String[]>(); // {name, uuid}
    private String inviteMessage = "";

    public WorldHost(Mw19 k) {
        this.k = k;
    }

    private Host host() {
        return k.platform.host();
    }

    public boolean available() {
        return host().available();
    }

    public int port() {
        return host().port();
    }

    public Net net() {
        return net;
    }

    public String netMessage() {
        return netMessage;
    }

    public String publicAddress() {
        return publicAddress;
    }

    public List<String[]> invited() {
        return Collections.unmodifiableList(invited);
    }

    public String inviteMessage() {
        return inviteMessage;
    }

    /** Opens the world to the network. Returns the port, or -1 when it could not. */
    public int open(int gameMode, boolean commands) {
        int p = port();
        if (p > 0) return p;
        p = host().open(gameMode, commands);
        if (p > 0) Log.info("hosting world on port " + p);
        return p;
    }

    /** "address:port" on the local network, or null when not open. */
    public String lanAddress() {
        int p = port();
        String ip = localIp();
        return p <= 0 || ip == null ? null : ip + ":" + p;
    }

    /** Asks the router to forward the port and turns the whitelist on (invited players only). */
    public void openInternet() {
        final int p = port();
        if (p <= 0 || net == Net.WORKING) return;
        net = Net.WORKING;
        netMessage = "Asking your router to open port " + p + "…";
        host().setWhitelist(true);
        for (String[] i : invited) host().allow(UUID.fromString(i[1]), i[0]);
        worker.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    Upnp.Gateway g = gateway != null ? gateway : Upnp.discover(3000);
                    if (g == null) {
                        fail("No router answered. UPnP may be off in your router's settings; you can forward port " + p + " (TCP) by hand instead.");
                        return;
                    }
                    gateway = g;
                    Upnp.map(g, p, "MW19 world", LEASE_S);
                    mappedPort = p;
                    String ip = Upnp.externalIp(g);
                    publicAddress = ip == null || ip.isEmpty() ? null : ip + ":" + p;
                    if (ip != null && isPrivate(ip)) {
                        net = Net.OPEN;
                        netMessage = "Your router is behind another network (" + ip + "), so friends outside it probably cannot reach you.";
                    } else {
                        net = Net.OPEN;
                        netMessage = "Open to the internet. Invited players join with the address below.";
                    }
                    renewAt = System.currentTimeMillis() + RENEW_MS;
                    Log.info("UPnP: forwarded port " + p + " to " + g.localAddress);
                } catch (IOException e) {
                    fail("The router refused: " + e.getMessage());
                } catch (RuntimeException e) {
                    fail("Port forwarding failed: " + e);
                }
            }
        });
    }

    private void fail(String msg) {
        net = Net.FAILED;
        netMessage = msg;
        Log.warn("UPnP: " + msg);
    }

    /** Removes the forward; the world stays open on the local network (whitelist off again). */
    public void closeInternet() {
        if (host().available()) host().setWhitelist(false);
        final Upnp.Gateway g = gateway;
        final int p = mappedPort;
        mappedPort = -1;
        publicAddress = null;
        net = Net.OFF;
        netMessage = "";
        if (g == null || p <= 0) return;
        worker.execute(new Runnable() {
            @Override
            public void run() {
                unmap(g, p);
            }
        });
    }

    private static void unmap(Upnp.Gateway g, int p) {
        try {
            Upnp.unmap(g, p);
            Log.info("UPnP: removed the forward for port " + p);
        } catch (IOException e) {
            Log.warn("UPnP: could not remove the forward for port " + p + ": " + e.getMessage());
        }
    }

    /** Looks the name up (Mojang profile API) and adds the player to the whitelist. */
    public void invite(final String name) {
        if (!SkinService.validName(name)) {
            inviteMessage = "Player names are 3-16 letters, digits or _";
            return;
        }
        for (String[] i : invited) {
            if (i[0].equalsIgnoreCase(name)) {
                inviteMessage = i[0] + " is already invited";
                return;
            }
        }
        inviteMessage = "Looking up " + name + "…";
        k.http.getJson("https://api.mojang.com/users/profiles/minecraft/" + name, 60_000, new Http.Callback() {
            @Override
            public void done(int status, Object json, String error) {
                String id = Json.str(Json.obj(json), "id", null), real = Json.str(Json.obj(json), "name", name);
                if (id == null || !id.matches("[0-9a-fA-F]{32}")) {
                    inviteMessage = status == 404 || status == 204 || status == 200 ? "No player called " + name : "Lookup failed: " + error;
                    return;
                }
                UUID uuid = UUID.fromString(id.replaceFirst("(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})", "$1-$2-$3-$4-$5"));
                invited.add(new String[]{real, uuid.toString()});
                if (host().available()) host().allow(uuid, real);
                inviteMessage = real + " can join";
            }
        });
    }

    public void uninvite(String[] entry) {
        invited.remove(entry);
        if (host().available()) host().disallow(UUID.fromString(entry[1]), entry[0]);
    }

    /** Game thread, every tick: drops the forward when the world closed, renews the lease while hosting. */
    public void tick() {
        boolean avail = host().available();
        if (wasAvailable && !avail) {
            invited.clear();
            inviteMessage = "";
        }
        wasAvailable = avail;
        if (mappedPort > 0 && (!avail || host().port() != mappedPort)) {
            closeInternet();
            return;
        }
        if (mappedPort > 0 && System.currentTimeMillis() > renewAt) {
            renewAt = System.currentTimeMillis() + RENEW_MS;
            final Upnp.Gateway g = gateway;
            final int p = mappedPort;
            worker.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        Upnp.map(g, p, "MW19 world", LEASE_S);
                    } catch (IOException e) {
                        Log.warn("UPnP: renewing the forward failed: " + e.getMessage());
                    }
                }
            });
        }
    }

    /** Game exit: remove the forward synchronously (bounded by the router timeouts). */
    public void shutdown() {
        Upnp.Gateway g = gateway;
        int p = mappedPort;
        mappedPort = -1;
        if (g != null && p > 0) unmap(g, p);
        worker.shutdownNow();
    }

    /** This computer's IPv4 address on the local network (first non-loopback, site-local address), or null. */
    static String localIp() {
        try {
            Enumeration<NetworkInterface> ifs = NetworkInterface.getNetworkInterfaces();
            while (ifs != null && ifs.hasMoreElements()) {
                NetworkInterface ni = ifs.nextElement();
                if (!ni.isUp() || ni.isLoopback() || ni.isVirtual()) continue;
                for (Enumeration<InetAddress> as = ni.getInetAddresses(); as.hasMoreElements(); ) {
                    InetAddress a = as.nextElement();
                    if (a instanceof Inet4Address && a.isSiteLocalAddress()) return a.getHostAddress();
                }
            }
        } catch (IOException e) {
            Log.warn("host: no local address: " + e);
        }
        return null;
    }

    /** RFC 1918, carrier-grade NAT (100.64/10) and link-local ranges. */
    static boolean isPrivate(String ip) {
        String[] p = ip.split("\\.");
        if (p.length != 4) return false;
        try {
            int a = Integer.parseInt(p[0]), b = Integer.parseInt(p[1]);
            return a == 10 || (a == 172 && b >= 16 && b <= 31) || (a == 192 && b == 168) || (a == 100 && b >= 64 && b <= 127)
                    || (a == 169 && b == 254) || a == 127;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
