package dev.starlight.core.net;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Minimal UPnP Internet Gateway client: finds the home router on the local network and asks it to forward one TCP
 * port to this computer (and to stop again). Talks only to the router; blocking, so call it off the game thread.
 */
public final class Upnp {
    private static final String[] TARGETS = {
        "urn:schemas-upnp-org:device:InternetGatewayDevice:1",
        "urn:schemas-upnp-org:device:InternetGatewayDevice:2",
        "urn:schemas-upnp-org:service:WANIPConnection:1",
    };

    /** A router's port-mapping service. */
    public static final class Gateway {
        final String controlUrl, serviceType;
        /** This computer's address as the router sees it (the target of the forward). */
        public final String localAddress;

        Gateway(String controlUrl, String serviceType, String localAddress) {
            this.controlUrl = controlUrl;
            this.serviceType = serviceType;
            this.localAddress = localAddress;
        }
    }

    public static final class UpnpException extends IOException {
        public final int code;

        UpnpException(int code, String msg) {
            super(msg);
            this.code = code;
        }
    }

    private Upnp() {}

    /** Searches the local network for a router with port mapping. Null when none answers within {@code timeoutMs}. */
    public static Gateway discover(int timeoutMs) throws IOException {
        Set<String> locations = new LinkedHashSet<String>();
        DatagramSocket s = new DatagramSocket();
        try {
            s.setSoTimeout(250);
            InetSocketAddress group = new InetSocketAddress("239.255.255.250", 1900);
            for (String st : TARGETS) {
                byte[] req = ("M-SEARCH * HTTP/1.1\r\nHOST: 239.255.255.250:1900\r\nMAN: \"ssdp:discover\"\r\nMX: 2\r\nST: " + st + "\r\n\r\n")
                        .getBytes(StandardCharsets.US_ASCII);
                s.send(new DatagramPacket(req, req.length, group));
            }
            long end = System.currentTimeMillis() + timeoutMs;
            byte[] buf = new byte[2048];
            while (System.currentTimeMillis() < end) {
                DatagramPacket p = new DatagramPacket(buf, buf.length);
                try {
                    s.receive(p);
                } catch (SocketTimeoutException e) {
                    if (!locations.isEmpty()) break;
                    continue;
                }
                String loc = header(new String(p.getData(), 0, p.getLength(), StandardCharsets.US_ASCII), "location");
                if (loc != null && sameHost(loc, p.getAddress())) locations.add(loc);
            }
        } finally {
            s.close();
        }
        for (String loc : locations) {
            try {
                Gateway g = fromLocation(loc);
                if (g != null) return g;
            } catch (IOException e) {
                // try the next responder
            }
        }
        return null;
    }

    /** Reads a device description and returns its WAN connection service. */
    static Gateway fromLocation(String location) throws IOException {
        URL url = new URL(location);
        if (!"http".equals(url.getProtocol())) return null;
        String xml = new String(http("GET", url, null, null), StandardCharsets.UTF_8);
        return fromDescription(location, xml, localAddressFor(url));
    }

    static Gateway fromDescription(String location, String xml, String localAddress) throws IOException {
        Document doc = parse(xml);
        String base = text(doc.getDocumentElement(), "URLBase");
        URL baseUrl = new URL(base == null || base.isEmpty() ? location : base);
        NodeList services = doc.getElementsByTagNameNS("*", "service");
        for (int i = 0; i < services.getLength(); i++) {
            Element svc = (Element) services.item(i);
            String type = text(svc, "serviceType"), control = text(svc, "controlURL");
            if (type == null || control == null) continue;
            if (!type.contains(":WANIPConnection:") && !type.contains(":WANPPPConnection:")) continue;
            URL c = new URL(baseUrl, control);
            if (!c.getHost().equals(new URL(location).getHost())) continue; // the service must live on the device that answered
            return new Gateway(c.toString(), type, localAddress);
        }
        return null;
    }

    public static String externalIp(Gateway g) throws IOException {
        return text(soap(g, "GetExternalIPAddress", ""), "NewExternalIPAddress");
    }

    /** Forwards {@code port} (TCP) to this computer. Routers that only allow permanent forwards get lease 0. */
    public static void map(Gateway g, int port, String description, int leaseSeconds) throws IOException {
        try {
            soap(g, "AddPortMapping", mapping(g, port, description, leaseSeconds));
        } catch (UpnpException e) {
            if (e.code != 725 || leaseSeconds == 0) throw e; // 725 OnlyPermanentLeasesSupported
            soap(g, "AddPortMapping", mapping(g, port, description, 0));
        }
    }

    public static void unmap(Gateway g, int port) throws IOException {
        soap(g, "DeletePortMapping", "<NewRemoteHost></NewRemoteHost><NewExternalPort>" + port + "</NewExternalPort><NewProtocol>TCP</NewProtocol>");
    }

    private static String mapping(Gateway g, int port, String description, int lease) {
        return "<NewRemoteHost></NewRemoteHost><NewExternalPort>" + port + "</NewExternalPort><NewProtocol>TCP</NewProtocol>"
                + "<NewInternalPort>" + port + "</NewInternalPort><NewInternalClient>" + g.localAddress + "</NewInternalClient>"
                + "<NewEnabled>1</NewEnabled><NewPortMappingDescription>" + escape(description) + "</NewPortMappingDescription>"
                + "<NewLeaseDuration>" + lease + "</NewLeaseDuration>";
    }

    private static Element soap(Gateway g, String action, String args) throws IOException {
        String body = "<?xml version=\"1.0\"?><s:Envelope xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\" "
                + "s:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\"><s:Body><u:" + action + " xmlns:u=\"" + g.serviceType + "\">"
                + args + "</u:" + action + "></s:Body></s:Envelope>";
        byte[] resp;
        try {
            resp = http("POST", new URL(g.controlUrl), "\"" + g.serviceType + "#" + action + "\"", body.getBytes(StandardCharsets.UTF_8));
        } catch (HttpError e) {
            Element err = null;
            try {
                err = parse(new String(e.body, StandardCharsets.UTF_8)).getDocumentElement();
            } catch (IOException ignored) {
                // not a SOAP fault
            }
            String code = err == null ? null : text(err, "errorCode"), desc = err == null ? null : text(err, "errorDescription");
            int c = 0;
            try {
                c = code == null ? 0 : Integer.parseInt(code.trim());
            } catch (NumberFormatException ignored) {
                // keep 0
            }
            throw new UpnpException(c, desc != null ? desc + " (" + c + ")" : "router answered HTTP " + e.status);
        }
        return parse(new String(resp, StandardCharsets.UTF_8)).getDocumentElement();
    }

    private static final class HttpError extends IOException {
        final int status;
        final byte[] body;

        HttpError(int status, byte[] body) {
            super("HTTP " + status);
            this.status = status;
            this.body = body;
        }
    }

    private static byte[] http(String method, URL url, String soapAction, byte[] body) throws IOException {
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        try {
            con.setConnectTimeout(3000);
            con.setReadTimeout(5000);
            con.setInstanceFollowRedirects(false);
            con.setRequestMethod(method);
            if (body != null) {
                con.setDoOutput(true);
                con.setRequestProperty("Content-Type", "text/xml; charset=\"utf-8\"");
                con.setRequestProperty("SOAPAction", soapAction);
                con.setFixedLengthStreamingMode(body.length);
                OutputStream os = con.getOutputStream();
                try {
                    os.write(body);
                } finally {
                    os.close();
                }
            }
            int status = con.getResponseCode();
            InputStream in = status >= 400 ? con.getErrorStream() : con.getInputStream();
            byte[] out = in == null ? new byte[0] : read(in);
            if (status >= 300) throw new HttpError(status, out);
            return out;
        } finally {
            con.disconnect();
        }
    }

    private static byte[] read(InputStream in) throws IOException {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            int n;
            while ((n = in.read(buf)) > 0) {
                out.write(buf, 0, n);
                if (out.size() > 256 * 1024) throw new IOException("router response too large");
            }
            return out.toByteArray();
        } finally {
            in.close();
        }
    }

    /** XML from the router is parsed with DTDs and external entities off. */
    private static Document parse(String xml) throws IOException {
        try {
            DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
            f.setNamespaceAware(true);
            f.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            f.setXIncludeAware(false);
            f.setExpandEntityReferences(false);
            DocumentBuilder b = f.newDocumentBuilder();
            return b.parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IOException("bad XML from router: " + e.getMessage());
        }
    }

    private static String text(Element root, String localName) {
        NodeList l = root.getElementsByTagNameNS("*", localName);
        if (l.getLength() == 0) l = root.getElementsByTagName(localName);
        return l.getLength() == 0 ? null : l.item(0).getTextContent().trim();
    }

    static String header(String response, String name) {
        for (String line : response.split("\r\n")) {
            int c = line.indexOf(':');
            if (c > 0 && line.substring(0, c).trim().toLowerCase(Locale.ROOT).equals(name)) return line.substring(c + 1).trim();
        }
        return null;
    }

    /** The LOCATION must point at the device that answered (and be a local address), not somewhere else. */
    private static boolean sameHost(String location, InetAddress from) {
        try {
            InetAddress host = InetAddress.getByName(new URL(location).getHost());
            return host.equals(from) && (host.isSiteLocalAddress() || host.isLinkLocalAddress() || host.isLoopbackAddress());
        } catch (IOException e) {
            return false;
        }
    }

    /** Our address on the interface that reaches {@code url}'s host. */
    private static String localAddressFor(URL url) throws IOException {
        DatagramSocket s = new DatagramSocket();
        try {
            s.connect(InetAddress.getByName(url.getHost()), url.getPort() > 0 ? url.getPort() : 80);
            return s.getLocalAddress().getHostAddress();
        } finally {
            s.close();
        }
    }

    private static String escape(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
