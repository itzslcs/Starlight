package dev.kestrel.core;

import dev.kestrel.core.rules.ServerRules;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.*;

class ServerRulesTest {
    private static final String BUNDLED = "{\"schema\":1,\"servers\":[{\"name\":\"Hypixel\",\"match\":[\"hypixel.net\",\"*.hypixel.net\"],"
            + "\"disallow\":[\"freelook\",\"tiertags\"]}]}";

    @Test
    void matchesHostsAndSubdomains() {
        assertTrue(ServerRules.matches("hypixel.net", "Hypixel.NET"));
        assertTrue(ServerRules.matches("hypixel.net", "hypixel.net:25565"));
        assertTrue(ServerRules.matches("*.hypixel.net", "mc.hypixel.net"));
        assertFalse(ServerRules.matches("*.hypixel.net", "hypixel.net"));
        assertFalse(ServerRules.matches("hypixel.net", "nothypixel.net"));
        assertFalse(ServerRules.matches("*.hypixel.net", "evilhypixel.net"));
        assertTrue(ServerRules.matches("hypixel.net", "hypixel.net.\u0000FML\u0000"));
        assertEquals("::1", ServerRules.normalize("[::1]:25565"));
    }

    @Test
    void bundledRulesDisableOnHypixelOnly() {
        ServerRules r = new ServerRules();
        r.load(BUNDLED, null);
        assertEquals(new HashSet<String>(Arrays.asList("freelook", "tiertags")), r.disallowedFor("mc.hypixel.net"));
        assertTrue(r.disallowedFor("example.org").isEmpty());
        assertTrue(r.disallowedFor(null).isEmpty());
    }

    @Test
    void userFileOverridesAndExtends() {
        ServerRules r = new ServerRules();
        r.load(BUNDLED, "{\"schema\":1,\"servers\":[{\"name\":\"Hypixel\",\"match\":[\"hypixel.net\"],\"disallow\":[\"zoom\"]},"
                + "{\"name\":\"Mine\",\"match\":[\"play.example.org\"],\"disallow\":[\"fullbright\"]}]}");
        assertEquals(new HashSet<String>(Arrays.asList("zoom")), r.disallowedFor("hypixel.net"));
        assertTrue(r.disallowedFor("mc.hypixel.net").isEmpty()); // user replaced the patterns
        assertEquals(new HashSet<String>(Arrays.asList("fullbright")), r.disallowedFor("play.example.org"));
        r.load(BUNDLED, "{\"schema\":1,\"replaceDefaults\":true,\"servers\":[]}");
        assertTrue(r.rules().isEmpty());
    }

    @Test
    void rejectsUnknownSchemaAndGarbage() {
        ServerRules r = new ServerRules();
        assertThrows(IllegalArgumentException.class, () -> r.load(BUNDLED, "{\"schema\":2,\"servers\":[]}"));
        assertThrows(IllegalArgumentException.class, () -> r.load(BUNDLED, "{nope"));
    }

    @Test
    void shippedDefaultFileParses() throws Exception {
        ServerRules r = new ServerRules();
        java.io.InputStream in = ServerRules.class.getResourceAsStream("/kestrel/serverrules.json");
        assertNotNull(in);
        java.util.Scanner sc = new java.util.Scanner(in, "UTF-8").useDelimiter("\\A");
        r.load(sc.next(), null);
        assertTrue(r.disallowedFor("hypixel.net").contains("freelook"));
    }
}
