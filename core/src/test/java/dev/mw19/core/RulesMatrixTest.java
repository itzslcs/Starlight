package dev.mw19.core;

import dev.mw19.api.module.Module;
import dev.mw19.api.module.Rule;
import dev.mw19.core.modules.BuiltinModules;
import dev.mw19.core.modules.InputRates;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** docs/RULES_MATRIX.md is the human source of truth; code must agree with it. */
class RulesMatrixTest {
    @Test
    void everyBuiltinModuleMatchesTheMatrix() throws Exception {
        Path doc = Paths.get("..", "docs", "RULES_MATRIX.md");
        if (!Files.exists(doc)) doc = Paths.get("docs", "RULES_MATRIX.md");
        Map<String, String[]> rows = new HashMap<String, String[]>();
        for (String line : new String(Files.readAllBytes(doc), StandardCharsets.UTF_8).split("\n")) {
            String[] c = line.split("\\|");
            if (c.length < 8) continue;
            rows.put(c[1].trim(), new String[]{c[5].trim(), c[6].trim()});
        }
        for (Module m : BuiltinModules.create(new InputRates())) {
            String[] row = rows.get(m.id());
            assertNotNull(row, m.id() + " missing from RULES_MATRIX.md");
            String verdict = m.rule() == Rule.ALLOWED ? "ALLOWED" : m.rule() == Rule.GRAY ? "GRAY" : "DISALLOWED@hypixel";
            assertEquals(row[0], verdict, m.id() + " verdict");
            assertEquals(row[1], m.defaultEnabled() ? "on" : "off", m.id() + " default");
        }
    }
}
