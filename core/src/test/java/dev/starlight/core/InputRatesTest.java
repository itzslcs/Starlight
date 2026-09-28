package dev.starlight.core;

import dev.starlight.api.game.Game.Binding;
import dev.starlight.core.modules.InputRates;
import org.junit.jupiter.api.Test;

import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

/** KeyCPS counting: per binding (TestPlatform binds attack to LMB and use to RMB), one-second window. */
class InputRatesTest {
    private final TestPlatform p = new TestPlatform(Paths.get("."));

    @Test
    void countsEachBindingOverTheLastSecond() {
        InputRates r = new InputRates();
        r.record(p, Keys.mouse(0), 1000);
        r.record(p, Keys.mouse(0), 1500);
        r.record(p, Keys.mouse(1), 1500);
        r.record(p, Keys.A, 1500); // not bound to anything tracked
        assertEquals(2, r.rate(Binding.ATTACK, 1600));
        assertEquals(1, r.rate(Binding.USE, 1600));
        assertEquals(0, r.rate(Binding.FORWARD, 1600));
        assertEquals(1, r.rate(Binding.ATTACK, 2000)); // the press at 1000 is a full second old
    }

    @Test
    void keepsTheNewest64Presses() {
        InputRates r = new InputRates();
        for (int i = 0; i < 100; i++) r.record(p, Keys.mouse(0), 10_000 + i);
        assertEquals(64, r.rate(Binding.ATTACK, 10_100));
    }
}
