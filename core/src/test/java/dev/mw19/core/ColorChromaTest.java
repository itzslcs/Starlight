package dev.mw19.core;

import dev.mw19.api.setting.ColorSetting;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** debug-log 2026-09-26: chroma hue was computed in float from epoch millis and only moved every ~2 minutes. */
class ColorChromaTest {
    @Test
    void chromaMovesWithinASecondAtRealTimestamps() {
        ColorSetting c = new ColorSetting("c", "C", "", 0xFFFF0000, true);
        long now = 1790380000000L; // 2026-09-26
        assertNotEquals(c.argb(now), c.argb(now + 250), "hue must change within a quarter second");
        assertEquals(c.argb(now), c.argb(now + 4000), "one cycle is 4 s at speed 1");
    }
}
