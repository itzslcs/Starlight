package dev.starlight.core;

import dev.starlight.api.util.Json;
import dev.starlight.core.modules.TierFormat;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** The Tier Tagger's reading of the MCTiers v2 profile schema (fixture captured from the live API). */
class TierFormatTest {
    // https://mctiers.com/api/v2/profile/d219c8ee-d32e-4da2-b22e-0aa69d36c88a, 2026-09-27 (trimmed to three modes)
    private static final String PROFILE = "{\"uuid\":\"d219c8ee-d32e-4da2-b22e-0aa69d36c88a\",\"name\":\"Marlowww\",\"region\":\"NA\","
            + "\"points\":450,\"overall\":1,\"rankings\":{"
            + "\"sword\":{\"tier\":1,\"pos\":0,\"peak_tier\":1,\"peak_pos\":0,\"attained\":1784635481,\"retired\":true},"
            + "\"axe\":{\"tier\":1,\"pos\":1,\"peak_tier\":1,\"peak_pos\":1,\"attained\":1784635489,\"retired\":true},"
            + "\"uhc\":{\"tier\":3,\"pos\":1,\"peak_tier\":2,\"peak_pos\":0,\"attained\":1784635499,\"retired\":false}}}";

    private static Map<String, Object> rankings() {
        return Json.obj(Json.obj(Json.parse(PROFILE)).get("rankings"));
    }

    @Test
    void schemaFieldsArePresent() {
        Map<String, Object> sword = Json.obj(rankings().get("sword"));
        for (String k : new String[]{"tier", "pos", "peak_tier", "peak_pos", "retired"}) assertTrue(sword.containsKey(k), k);
    }

    @Test
    void bestPicksTheHighestTierAndMarksRetired() {
        assertEquals("§8[§cRHT1§8]", TierFormat.format(rankings(), "Best", true, false));
        assertEquals("§8[§cRHT1 §7\u2694§8]", TierFormat.format(rankings(), "Best", true, true), "the gamemode shows as a glyph, not its name");
    }

    @Test
    void everyModeOfTheSettingHasItsOwnGlyph() {
        String[] modes = {"sword", "vanilla", "pot", "nethop", "smp", "uhc", "axe", "mace", "bed", "bow", "creeper",
                "debuff", "dia_crystal", "dia_smp", "elytra", "manhunt", "minecart", "og_vanilla", "speed", "trident"};
        java.util.Set<String> seen = new java.util.HashSet<String>();
        for (String m : modes) {
            Map<String, Object> one = Json.obj(Json.parse("{\"" + m + "\":{\"tier\":2,\"pos\":0,\"retired\":false}}"));
            String tag = TierFormat.format(one, m, true, true);
            String glyph = tag.substring(tag.indexOf("§7") + 2, tag.length() - 3);
            assertEquals(1, glyph.length(), m + " has no single-character glyph: " + glyph);
            assertTrue(seen.add(glyph), m + " reuses the glyph of another mode");
        }
    }

    @Test
    void anUnknownModeKeepsItsName() {
        Map<String, Object> one = Json.obj(Json.parse("{\"ltms\":{\"tier\":2,\"pos\":0,\"retired\":false}}"));
        assertEquals("§8[§6HT2 §7ltms§8]", TierFormat.format(one, "ltms", true, true), "a custom list's own mode has no glyph");
    }

    @Test
    void oneModeLowHighAndUnranked() {
        assertEquals("§8[§cRLT1§8]", TierFormat.format(rankings(), "axe", true, false));
        assertEquals("§8[§eLT3§8]", TierFormat.format(rankings(), "uhc", true, false)); // not retired: current tier, not peak
        assertNull(TierFormat.format(rankings(), "mace", true, false));
    }
}
