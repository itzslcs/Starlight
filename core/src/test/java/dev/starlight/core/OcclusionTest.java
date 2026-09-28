package dev.starlight.core;

import dev.starlight.core.perf.Occlusion;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Entity culling must hide only what is fully behind solid blocks, and always fail open. */
class OcclusionTest {
    /** A solid wall at x == 5 (y and z in [-20, 20]), optionally with a 1x1 hole at (5, 1, 0). */
    private static Occlusion.Blocks wall(final boolean hole) {
        return (x, y, z) -> x == 5 && y >= -20 && y <= 20 && z >= -20 && z <= 20 && !(hole && y == 1 && z == 0);
    }

    // entity box 1x2x1 at x in [10, 11], camera at (0.5, 1.6, 0.5)
    private static boolean see(Occlusion o, Occlusion.Blocks w, int id, long now) {
        return o.visible(w, id, 10, 0, 0, 11, 2, 1, 0.5, 1.6, 0.5, now);
    }

    @Test
    void openSpaceIsVisibleAndAFullWallHides() {
        Occlusion o = new Occlusion();
        assertTrue(see(o, (x, y, z) -> false, 1, 1000));
        assertFalse(see(o, wall(false), 2, 1000));
    }

    @Test
    void partlyVisibleThroughAHoleIsDrawn() {
        assertTrue(see(new Occlusion(), wall(true), 3, 1000));
    }

    @Test
    void nearbyEntitiesAreAlwaysDrawn() {
        Occlusion o = new Occlusion();
        assertTrue(o.visible(wall(false), 4, 3, 0, 0, 4, 2, 1, 0.5, 1.6, 0.5, 1000), "within 4 blocks");
    }

    @Test
    void resultsAreCachedThenRechecked() {
        Occlusion o = new Occlusion();
        assertFalse(see(o, wall(false), 5, 1000));
        assertFalse(see(o, (x, y, z) -> false, 5, 1100), "fresh cache: the wall is gone but the old answer holds");
        assertTrue(see(o, (x, y, z) -> false, 5, 1300), "stale: rechecked");
    }

    @Test
    void outOfBudgetUnknownEntitiesAreDrawn() {
        Occlusion o = new Occlusion();
        Occlusion.Blocks slow = wall(false);
        int drawn = 0;
        for (int id = 100; id < 3000; id++) if (see(o, slow, id, 1000)) drawn++;
        assertTrue(drawn > 0, "once the per-frame lookup budget is spent, unchecked entities are drawn");
        o.newFrame(1000);
        assertFalse(see(o, slow, 99999, 1000), "a new frame restores the budget");
    }
}
