package dev.starlight.core.modules;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Quick Commands sends only single-line slash commands. */
class QuickCommandsTest {
    @Test
    void onlySingleLineSlashCommands() {
        assertEquals("hub", MoreHud.QuickCommands.command("/hub"));
        assertEquals("party warp", MoreHud.QuickCommands.command("  /party warp "));
        assertNull(MoreHud.QuickCommands.command("hello everyone"));
        assertNull(MoreHud.QuickCommands.command("/"));
        assertNull(MoreHud.QuickCommands.command("/a\nb"));
        assertNull(MoreHud.QuickCommands.command(""));
        assertNull(MoreHud.QuickCommands.command(null));
    }
}
