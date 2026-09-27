package dev.mw19.core;

import dev.mw19.api.hud.Anchor;
import dev.mw19.api.module.Rule;
import dev.mw19.api.util.Json;
import dev.mw19.core.config.AtomicFiles;
import dev.mw19.core.config.ClientSettings;
import dev.mw19.core.config.ConfigManager;
import dev.mw19.core.event.SchedulerImpl;
import dev.mw19.core.hud.HudElement;
import dev.mw19.core.hud.HudManager;
import dev.mw19.core.module.ModuleManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ConfigManagerTest {
    @TempDir
    Path dir;

    /** A fresh "client": modules + config manager over the same directory. */
    private static final class Rig {
        final ModuleManager mm = new ModuleManager();
        final HudManager hud = new HudManager(mm);
        final ClientSettings client = new ClientSettings();
        final ConfigManager cfg;
        final TestModules.Plain plain = new TestModules.Plain("plain", Rule.ALLOWED, false);
        final TestModules.Hud hudMod = new TestModules.Hud("hudmod");
        final ModuleManager.State ps, hs;

        Rig(Path dir) {
            cfg = new ConfigManager(dir, mm, hud, client, new SchedulerImpl());
            ps = mm.register(plain, "core");
            hs = mm.register(hudMod, "core");
            cfg.load();
        }
    }

    @Test
    void roundTripsModulesSettingsHudAndClient() {
        Rig a = new Rig(dir);
        assertFalse(a.ps.enabled(), "default off");
        assertTrue(a.hs.enabled(), "HUD test module defaults on");
        a.mm.setEnabled(a.ps, true);
        a.mm.setFavorite(a.ps, true);
        a.plain.num.set(8.0);
        a.plain.flag.set(true);
        HudElement e = a.hud.element(a.hudMod);
        e.anchor = Anchor.BOTTOM_LEFT;
        e.x = 11;
        e.scale = 1.75f;
        a.client.theme.set("Nebula");
        a.cfg.flush();

        Rig b = new Rig(dir);
        assertTrue(b.ps.enabled());
        assertTrue(b.ps.favorite());
        assertEquals(8.0, b.plain.num.get(), 0);
        assertTrue(b.plain.flag.on());
        HudElement e2 = b.hud.element(b.hudMod);
        assertEquals(Anchor.BOTTOM_LEFT, e2.anchor);
        assertEquals(11, e2.x, 1e-6);
        assertEquals(1.75f, e2.scale, 1e-6);
        assertEquals("Nebula", b.client.theme.get());
    }

    @Test
    void recoversFromCorruptProfileUsingBackup() throws Exception {
        Rig a = new Rig(dir);
        a.mm.setEnabled(a.ps, true);
        a.cfg.flush();
        new Rig(dir).cfg.flush(); // second session start -> backup of the good file
        Path profile = a.cfg.profileFile(ConfigManager.DEFAULT_PROFILE);
        Files.write(profile, "{ this is not json".getBytes(StandardCharsets.UTF_8));

        Rig b = new Rig(dir);
        assertTrue(b.ps.enabled(), "restored from backup");
        assertFalse(b.cfg.notices.isEmpty());
        boolean quarantined = false;
        for (Path p : Files.newDirectoryStream(dir.resolve("profiles"))) quarantined |= p.getFileName().toString().contains(".corrupt-");
        assertTrue(quarantined, "damaged file kept aside");
    }

    @Test
    void keepsEntriesOfModulesNotRegisteredNowAndAppliesThemLater() throws Exception {
        Path profile = dir.resolve("profiles").resolve("Default.json");
        Files.createDirectories(profile.getParent());
        AtomicFiles.write(profile, "{\"schema\":1,\"name\":\"Default\",\"modules\":{\"late_mod\":{\"enabled\":true,"
                + "\"settings\":{\"num\":2}}}}");
        Rig a = new Rig(dir);
        TestModules.Plain late = new TestModules.Plain("late_mod", Rule.ALLOWED, false);
        ModuleManager.State ls = a.mm.register(late, "extra");
        a.cfg.applyTo(ls);
        assertTrue(ls.enabled());
        assertEquals(2.0, late.num.get(), 0);

        a.mm.unregisterOwner("extra");
        a.cfg.flush();
        Map<String, Object> saved = Json.obj(Json.parse(AtomicFiles.read(profile)));
        assertTrue(Json.obj(saved.get("modules")).containsKey("late_mod"), "unknown module data preserved");
    }

    @Test
    void migratesV0ProfileOnDisk() throws Exception {
        Path profile = dir.resolve("profiles").resolve("Default.json");
        Files.createDirectories(profile.getParent());
        AtomicFiles.write(profile, "{\"enabled\":[\"plain\"],\"settings\":{\"plain.num\":3}}");
        Rig a = new Rig(dir);
        assertTrue(a.ps.enabled());
        assertEquals(3.0, a.plain.num.get(), 0);
    }

    @Test
    void profileLifecycleAndSharing() throws Exception {
        Rig a = new Rig(dir);
        a.mm.setEnabled(a.ps, true);
        a.cfg.create("PvP", true);
        a.cfg.switchTo("PvP");
        assertEquals("PvP", a.cfg.active());
        assertTrue(a.ps.enabled(), "duplicated from current");
        a.mm.setEnabled(a.ps, false);
        a.cfg.switchTo(ConfigManager.DEFAULT_PROFILE);
        assertTrue(a.ps.enabled(), "switching back restores the other profile");

        String shared = a.cfg.export("PvP");
        String name = a.cfg.importProfile(shared);
        assertEquals("PvP 2", name);
        a.cfg.switchTo(name);
        assertFalse(a.ps.enabled());

        a.cfg.rename("PvP 2", "Ranked");
        assertEquals("Ranked", a.cfg.active());
        assertThrows(java.io.IOException.class, () -> a.cfg.delete("Ranked"), "cannot delete active");
        a.cfg.switchTo("PvP");
        a.cfg.delete("Ranked");
        assertFalse(a.cfg.profiles().contains("Ranked"));
        assertThrows(java.io.IOException.class, () -> a.cfg.create("../evil", false));
        assertThrows(java.io.IOException.class, () -> a.cfg.importProfile("junk"));
    }

    @Test
    void autoProfilesMatchServers() throws Exception {
        Rig a = new Rig(dir);
        a.cfg.create("Hyp", false);
        a.cfg.setAutoProfiles(Collections.singletonList(new String[]{"*.hypixel.net", "Hyp"}));
        assertEquals("Hyp", a.cfg.profileFor("mc.hypixel.net"));
        assertNull(a.cfg.profileFor("example.org"));
        assertNull(a.cfg.profileFor(null));
    }
}
