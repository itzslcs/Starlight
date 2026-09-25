package dev.kestrel.core;

import dev.kestrel.core.plugin.PluginDescriptor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PluginDescriptorTest {
    private static final String OK = "{\"id\":\"tiertags\",\"name\":\"Tier Tags\",\"version\":\"1.2.0\",\"api\":\"1.0\","
            + "\"main\":\"dev.x.Main\",\"authors\":[\"a\",\"b\"],\"depends\":{\"lib\":\">=1.1\"}}";

    @Test
    void parsesAndValidates() {
        PluginDescriptor d = PluginDescriptor.parse(OK);
        assertEquals("tiertags", d.id);
        assertEquals(2, d.authors.size());
        assertEquals(">=1.1", d.depends.get("lib"));
        assertTrue(d.apiCompatible(1, 0));
        assertTrue(d.apiCompatible(1, 3));
        assertFalse(d.apiCompatible(2, 0));
        for (String bad : new String[]{"{", "{}", OK.replace("tiertags", "Bad Id"), OK.replace("\"1.0\"", "\"one\""),
                OK.replace("dev.x.Main", "Main"), OK.replace("\"version\":\"1.2.0\",", "")}) {
            assertThrows(IllegalArgumentException.class, () -> PluginDescriptor.parse(bad), bad);
        }
        PluginDescriptor newer = PluginDescriptor.parse(OK.replace("\"1.0\"", "\"1.5\""));
        assertFalse(newer.apiCompatible(1, 0), "plugin built for a newer minor API");
    }

    @Test
    void versionRequirements() {
        assertTrue(PluginDescriptor.satisfies("1.2.0", "*"));
        assertTrue(PluginDescriptor.satisfies("1.2.0", ">=1.1"));
        assertTrue(PluginDescriptor.satisfies("1.10.0", ">=1.9"));
        assertFalse(PluginDescriptor.satisfies("1.0.9", ">=1.1"));
        assertTrue(PluginDescriptor.satisfies("2.0.0", "2.0"));
        assertFalse(PluginDescriptor.satisfies("2.0.1", "2.0.0"));
        assertEquals(0, PluginDescriptor.compare("1.0.0-beta", "1.0"));
    }
}
