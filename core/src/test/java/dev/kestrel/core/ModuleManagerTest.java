package dev.kestrel.core;

import dev.kestrel.api.module.Rule;
import dev.kestrel.core.module.ModuleManager;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ModuleManagerTest {
    @Test
    void grayModulesNeverDefaultOn() {
        assertTrue(new TestModules.Plain("a", Rule.ALLOWED, true).defaultEnabled());
        assertFalse(new TestModules.Plain("b", Rule.GRAY, true).defaultEnabled());
        assertFalse(new TestModules.Plain("c", Rule.DISALLOWED_ON_SOME_SERVERS, true).defaultEnabled());
    }

    @Test
    void repeatedFailuresDisableTheModuleNotTheGame() {
        ModuleManager mm = new ModuleManager();
        final int[] failed = {0};
        mm.setListener(new ModuleManager.Listener() {
            @Override
            public void changed(ModuleManager.State s) {}

            @Override
            public void failed(ModuleManager.State s, Throwable cause) {
                failed[0]++;
            }
        });
        TestModules.Plain m = new TestModules.Plain("bad", Rule.ALLOWED, true);
        ModuleManager.State s = mm.register(m, "core");
        mm.setEnabled(s, true);
        m.throwOnTick = true;
        for (int i = 0; i < 20; i++) mm.tick();
        assertEquals(ModuleManager.MAX_FAILURES, m.ticks, "stops calling after the limit");
        assertFalse(s.active());
        assertTrue((s.suspend() & ModuleManager.SUSPEND_FAILED) != 0);
        assertEquals(1, failed[0]);
        assertEquals(1, m.disables);
        mm.clearFailures();
        assertTrue(s.active(), "user gets another try after clearFailures");
        assertEquals(2, m.enables);
    }

    @Test
    void suspensionsStackIndependently() {
        ModuleManager mm = new ModuleManager();
        TestModules.Plain m = new TestModules.Plain("x", Rule.GRAY, false);
        ModuleManager.State s = mm.register(m, "core");
        mm.setEnabled(s, true);
        assertTrue(s.active());
        mm.setSuspended(s, ModuleManager.SUSPEND_SERVER, true);
        mm.setSuspended(s, ModuleManager.SUSPEND_SAFE, true);
        assertFalse(s.active());
        mm.setSuspended(s, ModuleManager.SUSPEND_SERVER, false);
        assertFalse(s.active(), "still paused by competitive-safe");
        mm.setSuspended(s, ModuleManager.SUSPEND_SAFE, false);
        assertTrue(s.active());
        assertEquals(2, m.enables);
        assertEquals(1, m.disables);
    }

    @Test
    void hudListTracksActiveHudModules() {
        ModuleManager mm = new ModuleManager();
        ModuleManager.State h = mm.register(new TestModules.Hud("h"), "core");
        ModuleManager.State p = mm.register(new TestModules.Plain("p", Rule.ALLOWED, true), "core");
        mm.setEnabled(h, true);
        mm.setEnabled(p, true);
        assertEquals(1, mm.activeHud().length);
        mm.setEnabled(h, false);
        assertEquals(0, mm.activeHud().length);
        mm.unregisterOwner("core");
        assertTrue(mm.all().isEmpty());
        assertThrows(IllegalArgumentException.class, () -> new TestModules.Plain("Bad Id", Rule.ALLOWED, false));
    }
}
