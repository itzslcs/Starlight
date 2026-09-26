package dev.mw19.core;

import dev.mw19.api.util.Json;
import dev.mw19.core.config.Migrations;
import dev.mw19.core.config.ProfileCodec;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ProfileCodecTest {
    @SuppressWarnings("unchecked")
    private static Map<String, Object> profile() {
        return (Map<String, Object>) Json.parse("{\"schema\":1,\"name\":\"PvP\",\"modules\":{\"fps\":{\"enabled\":true,"
                + "\"settings\":{\"format\":\"FPS: 123\"},\"hud\":{\"anchor\":\"TOP_RIGHT\",\"x\":4,\"y\":4}}}}");
    }

    @Test
    void roundTrip() {
        String s = ProfileCodec.encode(profile());
        assertTrue(s.startsWith(ProfileCodec.PREFIX));
        assertFalse(s.contains("\n"));
        assertEquals(profile(), ProfileCodec.decode("  " + s + "\n"));
    }

    @Test
    void rejectsDamage() {
        String s = ProfileCodec.encode(profile());
        String badCrc = s.substring(0, s.lastIndexOf(':') + 1) + "deadbeef";
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> ProfileCodec.decode(badCrc));
        assertTrue(e.getMessage().contains("checksum"));
        assertThrows(IllegalArgumentException.class, () -> ProfileCodec.decode("hello"));
        assertThrows(IllegalArgumentException.class, () -> ProfileCodec.decode(s.substring(0, s.length() / 2) + ":00"));
        assertThrows(IllegalArgumentException.class, () -> ProfileCodec.decode(ProfileCodec.PREFIX + "@@@:1"));
    }

    @Test
    void importMigratesOldSchemaAndRejectsNewer() {
        @SuppressWarnings("unchecked")
        Map<String, Object> v0 = (Map<String, Object>) Json.parse("{\"name\":\"Old\",\"enabled\":[\"fps\"],\"settings\":{\"fps.format\":\"123\"}}");
        Map<String, Object> out = ProfileCodec.decode(ProfileCodec.encode(v0));
        assertEquals(1, Migrations.schemaOf(out));
        Map<String, Object> fps = Json.obj(Json.obj(out.get("modules")).get("fps"));
        assertEquals(Boolean.TRUE, fps.get("enabled"));
        assertEquals("123", Json.obj(fps.get("settings")).get("format"));

        Map<String, Object> future = profile();
        future.put("schema", 99.0);
        assertThrows(IllegalArgumentException.class, () -> ProfileCodec.decode(ProfileCodec.encode(future)));
    }
}
