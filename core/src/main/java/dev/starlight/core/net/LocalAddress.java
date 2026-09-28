package dev.starlight.core.net;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.URL;
import java.util.Locale;

/** Exploit Protection: whether a server-supplied URL points at this computer or the local network. */
public final class LocalAddress {
    private LocalAddress() {}

    /**
     * True for local host names (localhost, .local, .lan, .home.arpa, .internal) and for loopback, private, link-local,
     * multicast and unique-local addresses. Host names are only resolved when {@code resolve} is set (it blocks).
     */
    public static boolean isLocal(URL url, boolean resolve) {
        String h = url.getHost() == null ? "" : url.getHost().toLowerCase(Locale.ROOT);
        if (h.isEmpty() || h.equals("localhost") || h.endsWith(".localhost") || h.endsWith(".local") || h.endsWith(".lan")
                || h.endsWith(".home.arpa") || h.endsWith(".internal")) return true;
        if (h.startsWith("[") && h.endsWith("]")) h = h.substring(1, h.length() - 1);
        boolean literal = h.indexOf(':') >= 0 || h.matches("[0-9.]+");
        if (!literal && !resolve) return false;
        try {
            for (InetAddress a : InetAddress.getAllByName(h)) if (isLocal(a)) return true;
        } catch (Exception e) {
            return false; // unresolvable: the game's own download fails the same way
        }
        return false;
    }

    static boolean isLocal(InetAddress a) {
        return a.isAnyLocalAddress() || a.isLoopbackAddress() || a.isSiteLocalAddress() || a.isLinkLocalAddress()
                || a.isMulticastAddress() || (a instanceof Inet6Address && (a.getAddress()[0] & 0xFE) == 0xFC);
    }
}
