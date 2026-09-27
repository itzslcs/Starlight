package dev.mw19.core;

import dev.mw19.core.config.ProfileCodec;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** The client was renamed Kestrel -> MW19 (2026-09-26); existing settings and shared codes keep working. */
class RenameCompatTest {
    @Test
    void oldProfileCodesStillImport() {
        Map<String, Object> m = new HashMap<String, Object>();
        m.put("name", "PvP");
        String code = ProfileCodec.encode(m);
        assertTrue(code.startsWith("MW19-P1:"));
        String old = "KESTREL-P1:" + code.substring("MW19-P1:".length());
        assertEquals("PvP", ProfileCodec.decode(old).get("name"));
    }

    @Test
    void oldConfigFolderMovesOnce(@TempDir Path game) throws Exception {
        Files.createDirectories(game.resolve("Kestrel"));
        Files.write(game.resolve("Kestrel/config.json"), "{}".getBytes(StandardCharsets.UTF_8));
        Path dir = Mw19.configDir(game);
        assertEquals(game.resolve("MW19"), dir);
        assertTrue(Files.exists(dir.resolve("config.json")));
        assertFalse(Files.exists(game.resolve("Kestrel")));
        Files.createDirectories(game.resolve("Kestrel")); // a later stray folder never overwrites MW19/
        assertEquals(dir, Mw19.configDir(game));
        assertTrue(Files.exists(dir.resolve("config.json")));
    }

    @Test
    void themeNamesFromBefore080Migrate() {
        for (String[] pair : new String[][]{{"MW19", "Starlight"}, {"Violet", "Nebula"}, {"Forest", "Aurora"}, {"Rose", "Sakura"},
                {"Glacier", "Glacier"}, {"White", "White"}}) {
            Map<String, Object> client = new HashMap<String, Object>();
            client.put("theme", pair[0]);
            Map<String, Object> root = new HashMap<String, Object>();
            root.put("schema", 1.0);
            root.put("client", client);
            dev.mw19.core.config.Migrations.global(root);
            assertEquals(pair[1], client.get("theme"), pair[0]);
            assertEquals(2, dev.mw19.core.config.Migrations.schemaOf(root));
            assertEquals(pair[1], dev.mw19.core.gui.Theme.preset(pair[1]).name);
        }
        for (String name : dev.mw19.core.gui.Theme.PRESETS) assertEquals(name, dev.mw19.core.gui.Theme.preset(name).name);
    }
}
