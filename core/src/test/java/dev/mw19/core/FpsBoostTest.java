package dev.mw19.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/** FPS Boost keeps the player's original options (first snapshot wins) and Undo puts them back. */
class FpsBoostTest {
    @Test
    void undoRestoresTheOriginalEvenAfterApplyingTwice(@TempDir Path dir) {
        Mw19 k = TestPlatform.boot(dir);
        TestPlatform p = (TestPlatform) k.platform;
        p.options.put("clouds", "FAST");
        assertFalse(k.fpsBoost.active());
        k.fpsBoost.apply();
        k.fpsBoost.apply(); // the second snapshot would be the boosted value
        assertTrue(k.fpsBoost.active());
        assertEquals("OFF", p.options.get("clouds"));
        k.fpsBoost.undo();
        assertEquals("FAST", p.options.get("clouds"));
        assertFalse(k.fpsBoost.active());
    }
}
