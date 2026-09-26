package dev.mw19.core;

import dev.mw19.api.util.Json;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/** debug-log 2026-09-26: saves from two threads shared config.json.tmp and one failed (seen at exit on 1.8.9). */
class ConfigRaceTest {
    @Test
    void concurrentSavesNeitherFailNorLoseTheNewest(@TempDir Path dir) throws Exception {
        final Mw19 k = TestPlatform.boot(dir);
        Log.bind(new TestPlatform(dir).logger()); // count errors
        TestPlatform.ERRORS.set(0);
        Thread[] threads = new Thread[4];
        for (int t = 0; t < threads.length; t++) {
            threads[t] = new Thread(new Runnable() {
                @Override
                public void run() {
                    for (int i = 0; i < 40; i++) k.config.flush();
                }
            });
            threads[t].start();
        }
        for (Thread t : threads) t.join();
        assertEquals(0, TestPlatform.ERRORS.get(), "a save failed (see [test] ERROR lines)");
        Path config = dir.resolve("MW19").resolve("config.json");
        assertTrue(Json.obj(Json.parse(new String(Files.readAllBytes(config), "UTF-8"))).containsKey("activeProfile"));
        assertFalse(Files.exists(dir.resolve("MW19").resolve("config.json.tmp")));
    }
}
