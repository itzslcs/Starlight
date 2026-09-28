package dev.starlight.core;

import dev.starlight.core.perf.HardwareTier;
import dev.starlight.core.perf.VideoPreset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/** Presets keep the player's original options (first snapshot wins), Undo puts them back, and auto never raises distances. */
class VideoPresetsTest {
    @Test
    void undoRestoresTheOriginalAfterSwitchingPresets(@TempDir Path dir) {
        Starlight k = TestPlatform.boot(dir);
        TestPlatform p = (TestPlatform) k.platform;
        p.options.put("clouds", "FAST");
        assertFalse(k.video.active());
        k.video.apply(VideoPreset.LOW, false);
        assertEquals("OFF", p.options.get("clouds"));
        assertEquals("8", p.options.get("renderDistance"));
        k.video.apply(VideoPreset.HIGH, false); // a second snapshot would hold LOW's values
        assertEquals(VideoPreset.HIGH, k.video.current());
        assertEquals("FANCY", p.options.get("clouds"));
        k.video.undo();
        assertEquals("FAST", p.options.get("clouds"));
        assertEquals("12", p.options.get("renderDistance"));
        assertEquals("true", p.options.get("vsync"));
        assertFalse(k.video.active());
        assertNull(k.video.current());
    }

    @Test
    void onlyLowerKeepsAShorterViewDistance(@TempDir Path dir) {
        Starlight k = TestPlatform.boot(dir);
        TestPlatform p = (TestPlatform) k.platform;
        p.options.put("renderDistance", "4");
        k.video.apply(VideoPreset.HIGH, true);
        assertEquals("4", p.options.get("renderDistance")); // High's 12 would have raised it
        assertEquals("10", p.options.get("simulationDistance")); // 12 -> 10 is a decrease
        k.video.apply(VideoPreset.POTATO, true);
        assertEquals("4", p.options.get("renderDistance"));
    }

    @Test
    void firstRunAppliesOnFreshInstallsAndOnlyHintsOnUpgrades(@TempDir Path dir) {
        Starlight k = TestPlatform.boot(dir.resolve("fresh"));
        TestPlatform p = (TestPlatform) k.platform;
        k.video.firstRun(true);
        assertEquals(VideoPreset.POTATO, k.video.current()); // Intel UHD 620 in TestPlatform
        assertEquals("MINIMAL", p.options.get("particles"));
        k.video.firstRun(true); // once only
        k.video.undo();
        k.video.firstRun(true);
        assertNull(k.video.current());

        Starlight up = TestPlatform.boot(dir.resolve("upgrade"));
        up.config.section("video").remove("fresh"); // as a profile saved by an Starlight from before presets
        up.video.firstRun(false);
        assertNull(up.video.current());
        assertNull(((TestPlatform) up.platform).options.get("particles"));
    }

    /** debug-log 2026-09-27: a first start that crashed before its first tick (Vulkan failing) saved a profile on the
     *  way out, so the next start was no longer "fresh" and skipped the preset. */
    @Test
    void aFirstStartThatCrashedStillGetsThePresetNextTime(@TempDir Path dir) {
        Starlight crashed = TestPlatform.boot(dir); // fresh install, then down before the first tick: firstRun never ran
        assertTrue(crashed.config.freshInstall);
        crashed.config.flush(); // the shutdown hook saves on the way out
        Starlight next = TestPlatform.boot(dir);
        assertFalse(next.config.freshInstall);
        next.video.firstRun(next.config.freshInstall);
        assertEquals(VideoPreset.POTATO, next.video.current());
    }

    @Test
    void everyPresetValueIsInsideVanillaRanges() {
        for (VideoPreset v : VideoPreset.values()) {
            int rd = Integer.parseInt(v.settings().get("renderDistance")), sim = Integer.parseInt(v.settings().get("simulationDistance"));
            assertTrue(rd >= 2 && rd <= 12, v + " renderDistance");
            assertTrue(sim >= 5 && sim <= 12, v + " simulationDistance");
            int weather = Integer.parseInt(v.settings().get("weatherRadius"));
            assertTrue(weather >= 3 && weather <= 10, v + " weatherRadius");
            int blur = Integer.parseInt(v.settings().get("menuBlur")), mip = Integer.parseInt(v.settings().get("mipmaps"));
            assertTrue(blur >= 0 && blur <= 10 && mip >= 0 && mip <= 4, v + " blur/mipmaps");
            int cloud = Integer.parseInt(v.settings().get("cloudRange"));
            assertTrue(cloud >= 2 && cloud <= 128, v + " cloudRange");
            assertEquals("false", v.settings().get("vsync"));
        }
    }

    @Test
    void hardwareTiers() {
        assertTier(VideoPreset.POTATO, "llvmpipe (LLVM 17.0.6, 256 bits)", "", 16, 4096);
        assertTier(VideoPreset.POTATO, "Mesa Intel(R) UHD Graphics 620 (KBL GT2)", "", 8, 4096);
        assertTier(VideoPreset.LOW, "Intel(R) Iris(R) Xe Graphics", "", 8, 4096);
        assertTier(VideoPreset.MEDIUM, "Intel(R) Arc(TM) A580 Graphics", "", 12, 4096);
        assertTier(VideoPreset.HIGH, "Intel(R) Arc(TM) B580 Graphics", "", 12, 4096);
        assertTier(VideoPreset.HIGH, "NVIDIA GeForce RTX 3060/PCIe/SSE2", "", 12, 4096);
        assertTier(VideoPreset.MEDIUM, "NVIDIA GeForce GTX 1060 6GB/PCIe/SSE2", "", 8, 4096);
        assertTier(VideoPreset.MEDIUM, "NVIDIA GeForce GTX 1650/PCIe/SSE2", "", 8, 4096);
        assertTier(VideoPreset.LOW, "NVIDIA GeForce GTX 970/PCIe/SSE2", "", 8, 4096);
        assertTier(VideoPreset.LOW, "NVIDIA GeForce MX350/PCIe/SSE2", "", 8, 4096);
        assertTier(VideoPreset.HIGH, "AMD Radeon RX 6700 XT (navi22, LLVM 17.0.6, DRM 3.57)", "", 16, 4096);
        assertTier(VideoPreset.MEDIUM, "Radeon RX 580 Series", "", 8, 4096);
        assertTier(VideoPreset.LOW, "AMD Radeon(TM) Graphics", "", 12, 4096);
        assertTier(VideoPreset.MEDIUM, "AMD Radeon 780M (radeonsi, gfx1103_r1)", "", 16, 4096);
        assertTier(VideoPreset.MEDIUM, "Apple M2", "", 8, 4096);
        assertTier(VideoPreset.POTATO, "Mali-G78", "", 8, 4096);
        assertTier(VideoPreset.MEDIUM, "Some New GPU 9000", "discrete", 8, 4096);
        assertTier(VideoPreset.LOW, "Some New GPU 9000", "integrated", 8, 4096);
        assertTier(VideoPreset.POTATO, "whatever", "cpu", 8, 4096);
        // weak CPU or little memory: one step down each
        assertTier(VideoPreset.MEDIUM, "NVIDIA GeForce RTX 3060/PCIe/SSE2", "", 4, 4096);
        assertTier(VideoPreset.LOW, "NVIDIA GeForce RTX 3060/PCIe/SSE2", "", 4, 1024);
        assertTier(VideoPreset.LOW, "", "", 8, 4096);
    }

    private static void assertTier(VideoPreset want, String gpu, String kind, int threads, long heapMb) {
        HardwareTier.Suggestion s = HardwareTier.suggest(gpu, kind, threads, heapMb);
        assertEquals(want, s.preset, gpu + " / " + kind + " / " + threads + " threads / " + heapMb + " MB: " + s.reason);
    }
}
