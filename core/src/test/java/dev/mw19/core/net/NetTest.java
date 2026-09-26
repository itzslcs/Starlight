package dev.mw19.core.net;

import com.sun.net.httpserver.HttpServer;
import dev.mw19.core.event.SchedulerImpl;
import dev.mw19.core.skin.SkinServiceAccess;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/** UPnP, skin upload and local-address checks against a local fake server (no real network). */
class NetTest {
    private HttpServer server;
    private String base;
    private final List<String> soap = new ArrayList<String>();
    private volatile String auth, contentType;
    private volatile byte[] uploaded;
    private volatile int uploadStatus = 200;
    private volatile boolean refuseLease;

    @BeforeEach
    void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        base = "http://127.0.0.1:" + server.getAddress().getPort();
        server.createContext("/rootDesc.xml", ex -> reply(ex, 200, "<?xml version=\"1.0\"?><root xmlns=\"urn:schemas-upnp-org:device-1-0\"><device>"
                + "<serviceList><service><serviceType>urn:schemas-upnp-org:service:Layer3Forwarding:1</serviceType><controlURL>/l3f</controlURL></service></serviceList>"
                + "<deviceList><device><deviceList><device><serviceList><service><serviceType>urn:schemas-upnp-org:service:WANIPConnection:1</serviceType>"
                + "<controlURL>/ctl/IPConn</controlURL></service></serviceList></device></deviceList></device></deviceList></device></root>"));
        server.createContext("/evil.xml", ex -> reply(ex, 200, "<?xml version=\"1.0\"?><!DOCTYPE r [<!ENTITY x SYSTEM \"file:///etc/passwd\">]><root>&x;</root>"));
        server.createContext("/ctl/IPConn", ex -> {
            String body = new String(readAll(ex.getRequestBody()), StandardCharsets.UTF_8);
            String action = ex.getRequestHeaders().getFirst("SOAPAction");
            synchronized (soap) {
                soap.add(action + " " + body);
            }
            if (action.contains("GetExternalIPAddress")) {
                reply(ex, 200, "<s:Envelope xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\"><s:Body><u:GetExternalIPAddressResponse xmlns:u=\"x\">"
                        + "<NewExternalIPAddress>203.0.113.7</NewExternalIPAddress></u:GetExternalIPAddressResponse></s:Body></s:Envelope>");
            } else if (action.contains("AddPortMapping") && refuseLease && !body.contains("<NewLeaseDuration>0<")) {
                reply(ex, 500, "<s:Envelope xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\"><s:Body><s:Fault><detail><UPnPError xmlns=\"urn:schemas-upnp-org:control-1-0\">"
                        + "<errorCode>725</errorCode><errorDescription>OnlyPermanentLeasesSupported</errorDescription></UPnPError></detail></s:Fault></s:Body></s:Envelope>");
            } else {
                reply(ex, 200, "<s:Envelope xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\"><s:Body/></s:Envelope>");
            }
        });
        server.createContext("/pack.zip", ex -> {
            byte[] b = PACK;
            ex.sendResponseHeaders(200, b.length);
            OutputStream os = ex.getResponseBody();
            os.write(b);
            os.close();
        });
        server.createContext("/skins", ex -> {
            auth = ex.getRequestHeaders().getFirst("Authorization");
            contentType = ex.getRequestHeaders().getFirst("Content-Type");
            uploaded = readAll(ex.getRequestBody());
            reply(ex, uploadStatus, "{}");
        });
        server.start();
    }

    @AfterEach
    void stop() {
        server.stop(0);
    }

    private static void reply(com.sun.net.httpserver.HttpExchange ex, int status, String body) throws IOException {
        byte[] b = body.getBytes(StandardCharsets.UTF_8);
        ex.sendResponseHeaders(status, b.length);
        OutputStream os = ex.getResponseBody();
        os.write(b);
        os.close();
    }

    private static byte[] readAll(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        int n;
        while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
        return out.toByteArray();
    }

    @Test
    void mapsAndUnmapsThroughTheRouter() throws Exception {
        Upnp.Gateway g = Upnp.fromLocation(base + "/rootDesc.xml");
        assertNotNull(g);
        assertEquals("127.0.0.1", g.localAddress);
        Upnp.map(g, 25565, "MW19 world", 3600);
        assertEquals("203.0.113.7", Upnp.externalIp(g));
        Upnp.unmap(g, 25565);
        synchronized (soap) {
            assertEquals(3, soap.size());
            assertTrue(soap.get(0).startsWith("\"urn:schemas-upnp-org:service:WANIPConnection:1#AddPortMapping\""), soap.get(0));
            assertTrue(soap.get(0).contains("<NewExternalPort>25565</NewExternalPort>"));
            assertTrue(soap.get(0).contains("<NewInternalClient>127.0.0.1</NewInternalClient>"));
            assertTrue(soap.get(0).contains("<NewLeaseDuration>3600</NewLeaseDuration>"));
            assertTrue(soap.get(2).contains("DeletePortMapping"));
        }
    }

    @Test
    void retriesWithPermanentLeaseWhenTheRouterAsks() throws Exception {
        refuseLease = true;
        Upnp.Gateway g = Upnp.fromLocation(base + "/rootDesc.xml");
        Upnp.map(g, 40000, "MW19 world", 3600);
        synchronized (soap) {
            assertEquals(2, soap.size());
            assertTrue(soap.get(1).contains("<NewLeaseDuration>0</NewLeaseDuration>"));
        }
    }

    @Test
    void rejectsRouterXmlWithEntities() {
        assertThrows(IOException.class, () -> Upnp.fromLocation(base + "/evil.xml"));
    }

    @Test
    void ssdpHeaderParsing() {
        assertEquals("http://192.168.1.1:5000/rootDesc.xml",
                Upnp.header("HTTP/1.1 200 OK\r\nCACHE-CONTROL: max-age=120\r\nLocation: http://192.168.1.1:5000/rootDesc.xml\r\n\r\n", "location"));
        assertNull(Upnp.header("HTTP/1.1 200 OK\r\n\r\n", "location"));
    }

    @Test
    void skinUploadSendsMultipartWithBearerToken() throws Exception {
        SchedulerImpl main = new SchedulerImpl();
        HttpClient http = new HttpClient(main, "test");
        byte[] png = dev.mw19.core.PacksAndSkinsTest.png(64, 64);
        AtomicReference<String> result = new AtomicReference<String>();
        SkinServiceAccess.upload(http, base + "/skins", "eyJ.a.b", png, true, (ok, msg) -> result.set(ok + " " + msg));
        pump(main, result);
        assertTrue(result.get().startsWith("true"), result.get());
        assertEquals("Bearer eyJ.a.b", auth);
        assertTrue(contentType.startsWith("multipart/form-data; boundary=MW19"), contentType);
        String body = new String(uploaded, StandardCharsets.ISO_8859_1);
        assertTrue(body.contains("name=\"variant\"\r\n\r\nslim\r\n"), body);
        assertTrue(body.contains("name=\"file\"; filename=\"skin.png\"\r\nContent-Type: image/png\r\n\r\n\u0089PNG"), body);

        uploadStatus = 401;
        result.set(null);
        SkinServiceAccess.upload(http, base + "/skins", "eyJ.a.b", png, false, (ok, msg) -> result.set(ok + " " + msg));
        pump(main, result);
        assertTrue(result.get().startsWith("false Your login has expired"), result.get());

        result.set(null);
        SkinServiceAccess.upload(http, base + "/skins", null, png, false, (ok, msg) -> result.set(ok + " " + msg));
        assertTrue(result.get().startsWith("false Sign in"), result.get());
        http.shutdown();
        main.shutdown();
    }

    private static void pump(SchedulerImpl main, AtomicReference<String> until) throws InterruptedException {
        for (int i = 0; i < 500 && until.get() == null; i++) {
            main.tick();
            Thread.sleep(10);
        }
        assertNotNull(until.get(), "no callback");
    }

    private static final byte[] PACK = new byte[70_000];

    static {
        for (int i = 0; i < PACK.length; i++) PACK[i] = (byte) (i * 31);
    }

    @Test
    void downloadsVerifyHashAndSizeAndLeaveNoPartialFile(@org.junit.jupiter.api.io.TempDir java.nio.file.Path dir) throws Exception {
        SchedulerImpl main = new SchedulerImpl();
        HttpClient http = new HttpClient(main, "test");
        String sha = HttpClient.hex(java.security.MessageDigest.getInstance("SHA-512").digest(PACK));
        java.nio.file.Path ok = dir.resolve("ok.zip"), bad = dir.resolve("bad.zip"), big = dir.resolve("big.zip");
        AtomicReference<String> r = new AtomicReference<String>();
        http.download(base + "/pack.zip", ok, sha, 1 << 20, null, (s, b, e) -> r.set("s=" + s + " e=" + e));
        pump(main, r);
        assertEquals("s=200 e=null", r.get());
        assertArrayEquals(PACK, java.nio.file.Files.readAllBytes(ok));
        r.set(null);
        http.download(base + "/pack.zip", bad, "00" + sha.substring(2), 1 << 20, null, (s, b, e) -> r.set("e=" + e));
        pump(main, r);
        assertEquals("e=checksum mismatch", r.get());
        assertFalse(java.nio.file.Files.exists(bad));
        r.set(null);
        http.download(base + "/pack.zip", big, null, 50_000, null, (s, b, e) -> r.set("e=" + e));
        pump(main, r);
        assertEquals("e=file too large", r.get());
        assertFalse(java.nio.file.Files.exists(big));
        try (java.util.stream.Stream<java.nio.file.Path> left = java.nio.file.Files.list(dir)) {
            assertEquals(1, left.count(), "only ok.zip, no .part files");
        }
        r.set(null);
        http.download("http://example.com/pack.zip", dir.resolve("x.zip"), null, 1 << 20, null, (s, b, e) -> r.set("e=" + e));
        assertEquals("e=unsupported URL", r.get()); // plain http is refused outright
        http.shutdown();
        main.shutdown();
    }

    @Test
    void localAddresses() throws Exception {
        for (String u : new String[]{"http://localhost:8080/x", "http://127.0.0.1/p.zip", "http://10.0.0.5/", "http://192.168.1.1/",
                "http://172.16.0.1/", "http://[::1]:80/", "http://[fd12::1]/", "http://169.254.1.1/", "http://router.lan/", "http://nas.local/",
                "http://2130706433/", "http://0.0.0.0/"}) {
            assertTrue(LocalAddress.isLocal(new URL(u), false), u);
        }
        for (String u : new String[]{"https://8.8.8.8/pack.zip", "https://cdn.example.com/pack.zip"}) {
            assertFalse(LocalAddress.isLocal(new URL(u), false), u);
        }
    }
}
