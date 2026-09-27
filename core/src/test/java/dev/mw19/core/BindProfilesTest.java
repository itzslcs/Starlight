package dev.mw19.core;

import dev.mw19.core.binds.BindProfiles;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** Bind profiles: kept across restarts (the file other instances read), with the default for new instances. */
class BindProfilesTest {
    @Test
    void savedProfilesSurviveARestartWithTheirDefault(@TempDir Path dir) {
        Path file = dir.resolve("bind-profiles.json");
        BindProfiles a = new BindProfiles(file);
        Map<String, Integer> pvp = new LinkedHashMap<String, Integer>();
        pvp.put("key.hotbar.1", Keys.N0 + 1);
        pvp.put("key.hotbar.2", Keys.mouse(4)); // a side mouse button
        pvp.put("key.drop", Keys.NONE);
        a.put("PvP", pvp);
        a.put("Build", new LinkedHashMap<String, Integer>());
        a.setDefault("PvP");
        assertEquals("Profile 3", a.freeName());

        BindProfiles b = new BindProfiles(file); // another instance starting
        assertEquals(java.util.Arrays.asList("PvP", "Build"), b.names());
        assertEquals("PvP", b.defaultName());
        assertEquals(pvp, b.get("PvP"));

        b.delete("PvP");
        assertNull(b.defaultName(), "deleting the default leaves none");
        assertNull(new BindProfiles(file).get("PvP"));
        assertTrue(Files.exists(file));
    }

    @Test
    void applyingAProfileChangesOnlyWhatDiffers() {
        TestPlatform p = new TestPlatform(java.nio.file.Paths.get("."));
        p.binds.put("key.hotbar.1", Keys.N0 + 1);
        p.binds.put("key.hotbar.2", Keys.N0 + 2);
        Map<String, Integer> other = new LinkedHashMap<String, Integer>();
        other.put("key.hotbar.1", Keys.N0 + 1); // same
        other.put("key.hotbar.2", (int) 'Z'); // changed
        other.put("key.saveToolbarActivator", (int) 'C'); // this version has no such binding: skipped
        assertEquals(1, p.applyVanillaBindings(other));
        assertEquals(Integer.valueOf('Z'), p.vanillaBindingMap().get("key.hotbar.2"));
    }

    @Test
    void smokeAndBenchRunsKeepTheirProfilesInTheInstance(@TempDir Path dir) {
        assertEquals(dir.resolve("bind-profiles.json"), BindProfiles.location(dir, true));
    }
}
