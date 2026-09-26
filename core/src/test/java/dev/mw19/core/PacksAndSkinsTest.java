package dev.mw19.core;

import dev.mw19.api.util.Json;
import dev.mw19.core.packs.Modrinth;
import dev.mw19.core.skin.SkinLibrary;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class PacksAndSkinsTest {
    /** Just the PNG signature and an IHDR header with the given size (enough for the header check). */
    public static byte[] png(int w, int h) {
        byte[] b = new byte[33];
        int[] sig = {0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A};
        for (int i = 0; i < 8; i++) b[i] = (byte) sig[i];
        b[11] = 13;
        b[12] = 'I';
        b[13] = 'H';
        b[14] = 'D';
        b[15] = 'R';
        b[18] = (byte) (w >> 8);
        b[19] = (byte) w;
        b[22] = (byte) (h >> 8);
        b[23] = (byte) h;
        return b;
    }

    @Test
    void searchUrlEncodesQueryAndFacets() {
        String u = Modrinth.searchUrl("faithful 32x", "1.21.4", Modrinth.Sort.DOWNLOADS, 20, 20);
        assertTrue(u.startsWith("https://api.modrinth.com/v2/search?query=faithful%2032x&facets="), u);
        assertTrue(u.contains("%5B%5B%22project_type%3Aresourcepack%22%5D%2C%5B%22versions%3A1.21.4%22%5D%5D"), u);
        assertTrue(u.endsWith("&index=downloads&offset=20&limit=20"), u);
        assertFalse(Modrinth.searchUrl("", null, Modrinth.Sort.RELEVANCE, 0, 20).contains("versions"));
    }

    @Test
    void parsesHitsAndPicksNewestReleasePrimaryFile() {
        Object search = Json.parse("{\"total_hits\":2,\"hits\":[{\"project_id\":\"AAA\",\"slug\":\"a\",\"title\":\"Pack A\",\"author\":\"x\","
                + "\"downloads\":1234,\"icon_url\":\"https://cdn.modrinth.com/a.png\",\"description\":\"d\"},{\"slug\":\"no-id\"}]}");
        List<Modrinth.Hit> hits = Modrinth.hits(search);
        assertEquals(1, hits.size());
        assertEquals("Pack A", hits.get(0).title);
        assertEquals(1234, hits.get(0).downloads);
        assertEquals(2, Modrinth.totalHits(search));

        Object versions = Json.parse("[{\"version_type\":\"beta\",\"version_number\":\"2\",\"files\":[{\"url\":\"https://cdn.modrinth.com/b.zip\",\"filename\":\"b.zip\",\"primary\":true,\"size\":5,\"hashes\":{\"sha512\":\"bb\"}}]},"
                + "{\"version_type\":\"release\",\"version_number\":\"1\",\"files\":[{\"url\":\"https://cdn.modrinth.com/extra.zip\",\"filename\":\"extra.zip\",\"primary\":false},"
                + "{\"url\":\"https://cdn.modrinth.com/r.zip\",\"filename\":\"Pack R.zip\",\"primary\":true,\"size\":7,\"hashes\":{\"sha512\":\"rr\"}}]}]");
        Modrinth.PackFile f = Modrinth.newestFile(versions);
        assertNotNull(f);
        assertEquals("Pack R.zip", f.fileName);
        assertEquals("rr", f.sha512);
        assertEquals("1", f.versionName);
        assertNull(Modrinth.newestFile(Json.parse("[]")));
        assertNull(Modrinth.newestFile(Json.parse("[{\"files\":[{\"url\":\"http://insecure/x.zip\",\"filename\":\"x.zip\"}]}]")));
    }

    @Test
    void packFileNamesCannotLeaveTheFolder() {
        assertEquals("_evil.zip", Modrinth.safeFileName("../evil.zip"));
        assertEquals("a_b.zip", Modrinth.safeFileName("a\\b.zip"));
        assertEquals("C__x.zip", Modrinth.safeFileName("C:/x.zip"));
        assertNull(Modrinth.safeFileName("pack.exe"));
        assertNull(Modrinth.safeFileName(".zip"));
        assertNull(Modrinth.safeFileName(null));
        assertEquals("My Pack (1.21) [32x].zip", Modrinth.safeFileName("My Pack (1.21) [32x].zip"));
    }

    @Test
    void skinsMustBe64x64Png() {
        assertTrue(SkinLibrary.isSkin(png(64, 64)));
        assertFalse(SkinLibrary.isSkin(png(64, 32)));
        assertFalse(SkinLibrary.isSkin(png(128, 128)));
        byte[] notPng = png(64, 64);
        notPng[1] = 'X';
        assertFalse(SkinLibrary.isSkin(notPng));
        assertFalse(SkinLibrary.isSkin(new byte[3]));
    }

    @Test
    void smokeTestSkinIsARealPng() throws Exception {
        byte[] png = Smoke.testSkin();
        assertTrue(SkinLibrary.isSkin(png));
        java.awt.image.BufferedImage img = javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(png));
        assertEquals(64, img.getWidth());
        assertEquals(0xFF3A6FD8, img.getRGB(10, 10)); // face
        assertEquals(0, img.getRGB(40, 8) >>> 24);    // transparent hat layer
        assertEquals(0xFFC03030, img.getRGB(20, 20)); // body
    }

    @Test
    void libraryImportsNamesAndRemembersModels(@TempDir Path dir) throws Exception {
        SkinLibrary lib = new SkinLibrary(dir.resolve("skins"));
        Path src = dir.resolve("Steve Fan.png");
        Files.write(src, png(64, 64));
        assertNull(lib.importFile(src));
        Path bad = dir.resolve("bad.png");
        Files.write(bad, png(64, 32));
        assertNotNull(lib.importFile(bad));
        lib.add("Steve Fan", png(64, 64), true); // name taken: gets a suffix
        lib.add("../../escape", png(64, 64), false);
        lib.refresh();
        assertEquals(3, lib.entries().size());
        assertTrue(Files.exists(dir.resolve("skins").resolve("Steve Fan (2).png")));
        assertFalse(Files.exists(dir.resolve("escape.png")));
        SkinLibrary.Entry second = null;
        for (SkinLibrary.Entry e : lib.entries()) if (e.file.equals("Steve Fan (2).png")) second = e;
        assertNotNull(second);
        assertTrue(second.slim);
        SkinLibrary again = new SkinLibrary(dir.resolve("skins"));
        again.refresh();
        for (SkinLibrary.Entry e : again.entries()) assertEquals(e.file.equals("Steve Fan (2).png"), e.slim, e.file);
    }
}
