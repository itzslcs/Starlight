package dev.starlight.core;

import dev.starlight.core.config.ProfileCodec;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The client was renamed twice: Kestrel -> MW19 (2026-09-26), then MW19 -> Starlight (2026-09-27). Existing settings,
 * folders and shared profile codes keep working.
 */
class RenameCompatTest {
    @Test
    void oldProfileCodesStillImport() {
        Map<String, Object> m = new HashMap<String, Object>();
        m.put("name", "PvP");
        String code = ProfileCodec.encode(m);
        assertTrue(code.startsWith("STARLIGHT-P1:"));
        for (String old : new String[]{"MW19-P1:", "KESTREL-P1:"}) {
            assertEquals("PvP", ProfileCodec.decode(old + code.substring("STARLIGHT-P1:".length())).get("name"), old);
        }
    }

    @Test
    void anMw19FolderMovesOnce(@TempDir Path game) throws Exception {
        Files.createDirectories(game.resolve("MW19"));
        Files.write(game.resolve("MW19/config.json"), "{}".getBytes(StandardCharsets.UTF_8));
        Files.createDirectories(game.resolve("Kestrel")); // older still: MW19 wins, Kestrel is left alone
        Path dir = Starlight.configDir(game);
        assertEquals(game.resolve("Starlight"), dir);
        assertTrue(Files.exists(dir.resolve("config.json")));
        assertFalse(Files.exists(game.resolve("MW19")));
        Files.createDirectories(game.resolve("MW19")); // a later stray folder never overwrites Starlight/
        assertEquals(dir, ConfigFolder.of(game));
        assertTrue(Files.exists(dir.resolve("config.json")));
    }

    @Test
    void aKestrelFolderStillMoves(@TempDir Path game) throws Exception {
        Files.createDirectories(game.resolve("Kestrel"));
        Files.write(game.resolve("Kestrel/config.json"), "{}".getBytes(StandardCharsets.UTF_8));
        assertTrue(Files.exists(ConfigFolder.of(game).resolve("config.json")));
        assertFalse(Files.exists(game.resolve("Kestrel")));
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
            dev.starlight.core.config.Migrations.global(root);
            assertEquals(pair[1], client.get("theme"), pair[0]);
            assertEquals(2, dev.starlight.core.config.Migrations.schemaOf(root));
            assertEquals(pair[1], dev.starlight.core.gui.Theme.preset(pair[1]).name);
        }
        for (String name : dev.starlight.core.gui.Theme.PRESETS) assertEquals(name, dev.starlight.core.gui.Theme.preset(name).name);
    }
}
