package dev.mw19.core;

import dev.mw19.api.util.Json;
import dev.mw19.core.modules.TierFormat;
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
        assertEquals("§8[§cRHT1 §7sword§8]", TierFormat.format(rankings(), "Best", true, true));
    }

    @Test
    void oneModeLowHighAndUnranked() {
        assertEquals("§8[§cRLT1§8]", TierFormat.format(rankings(), "axe", true, false));
        assertEquals("§8[§eLT3§8]", TierFormat.format(rankings(), "uhc", true, false)); // not retired: current tier, not peak
        assertNull(TierFormat.format(rankings(), "mace", true, false));
    }
}
