package dev.kestrel.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Guard for the MCTiers v2 schema the Tier Tags addon relies on (field names checked against the live API). */
class TierFormatTest {
    @Test
    void liveSchemaFieldsArePresentInFixture() {
        // Captured from https://mctiers.com/api/v2/profile/{uuid} on 2026-09-25 (docs/DECISIONS D-015).
        String fixture = "{\"uuid\":\"d219c8ee-d32e-4da2-b22e-0aa69d36c88a\",\"name\":\"Marlowww\",\"region\":\"NA\",\"points\":450,"
                + "\"overall\":1,\"rankings\":{\"smp\":{\"tier\":1,\"pos\":0,\"peak_tier\":1,\"peak_pos\":0,\"attained\":1784635503,"
                + "\"retired\":true}}}";
        java.util.Map<String, Object> r = dev.kestrel.api.util.Json.obj(dev.kestrel.api.util.Json.obj(
                dev.kestrel.api.util.Json.parse(fixture)).get("rankings"));
        java.util.Map<String, Object> smp = dev.kestrel.api.util.Json.obj(r.get("smp"));
        for (String k : new String[]{"tier", "pos", "peak_tier", "peak_pos", "retired"}) assertTrue(smp.containsKey(k), k);
    }
}
