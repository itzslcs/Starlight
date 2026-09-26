package dev.mw19.core.skin;

import dev.mw19.core.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

/**
 * The skin folder ({@code <game>/MW19/skins}): PNG skins the player can preview and apply. Which model (classic or
 * slim) each file uses is kept next to them in {@code models.properties}. Game thread only; files are a few KB.
 */
public final class SkinLibrary {
    public static final int MAX_BYTES = 256 * 1024, MAX_FILES = 300;

    public static final class Entry {
        public final String file;
        public final byte[] png;
        /** Texture height: 64, or 32 for the old single-layer layout. */
        public final int texH;
        public boolean slim;
        /** Texture handle while loaded (owned by the page that draws it). */
        public int handle;

        Entry(String file, byte[] png, boolean slim) {
            this.file = file;
            this.png = png;
            this.texH = be32(png, 20);
            this.slim = slim;
        }

        public String label() {
            return file.substring(0, file.length() - 4);
        }
    }

    private final Path dir;
    private final List<Entry> entries = new ArrayList<Entry>();
    private final Properties models = new Properties();

    public SkinLibrary(Path dir) {
        this.dir = dir;
    }

    public Path folder() {
        return dir;
    }

    public List<Entry> entries() {
        return entries;
    }

    /** Re-reads the folder. Invalid files are skipped (and logged once per refresh). */
    public void refresh() {
        entries.clear();
        models.clear();
        try {
            Files.createDirectories(dir);
            Path props = dir.resolve("models.properties");
            if (Files.isRegularFile(props)) {
                InputStream in = Files.newInputStream(props);
                try {
                    models.load(in);
                } finally {
                    in.close();
                }
            }
            DirectoryStream<Path> ds = Files.newDirectoryStream(dir, "*.png");
            try {
                for (Path p : ds) {
                    if (entries.size() >= MAX_FILES) break;
                    if (!Files.isRegularFile(p) || Files.size(p) > MAX_BYTES) continue;
                    byte[] png = Files.readAllBytes(p);
                    String name = p.getFileName().toString();
                    if (!isSkin(png)) {
                        Log.warn("skins: " + name + " is not a 64x64 PNG, skipped");
                        continue;
                    }
                    entries.add(new Entry(name, png, "slim".equals(models.getProperty(name))));
                }
            } finally {
                ds.close();
            }
        } catch (IOException e) {
            Log.warn("skins: could not read " + dir + ": " + e);
        }
        Collections.sort(entries, new Comparator<Entry>() {
            @Override
            public int compare(Entry a, Entry b) {
                return a.file.compareToIgnoreCase(b.file);
            }
        });
    }

    public void setSlim(Entry e, boolean slim) {
        e.slim = slim;
        models.setProperty(e.file, slim ? "slim" : "classic");
        saveModels();
    }

    /** Copies a dropped/chosen file into the folder. Returns null on success, else why it was refused. */
    public String importFile(Path src) {
        try {
            if (!Files.isRegularFile(src) || Files.size(src) > MAX_BYTES) return "not a skin file";
            byte[] png = Files.readAllBytes(src);
            if (!isSkin(png)) return "not a 64×64 PNG skin";
            String base = src.getFileName().toString();
            add(base.toLowerCase(Locale.ROOT).endsWith(".png") ? base.substring(0, base.length() - 4) : base, png, false);
            return null;
        } catch (IOException e) {
            return e.toString();
        }
    }

    /** Writes a new skin file (a free name based on {@code base}) and returns its entry. */
    public Entry add(String base, byte[] png, boolean slim) throws IOException {
        if (!isSkin(png)) throw new IOException("not a skin PNG");
        Files.createDirectories(dir);
        String clean = base.replaceAll("[^A-Za-z0-9_ .()-]", "_").trim();
        if (clean.isEmpty() || clean.startsWith(".")) clean = "skin";
        if (clean.length() > 48) clean = clean.substring(0, 48);
        String name = clean + ".png";
        for (int i = 2; Files.exists(dir.resolve(name)); i++) name = clean + " (" + i + ").png";
        OutputStream out = Files.newOutputStream(dir.resolve(name));
        try {
            out.write(png);
        } finally {
            out.close();
        }
        Entry e = new Entry(name, png, slim);
        entries.add(e);
        if (slim) setSlim(e, true);
        return e;
    }

    private void saveModels() {
        try {
            OutputStream out = Files.newOutputStream(dir.resolve("models.properties"));
            try {
                models.store(out, "MW19 skin models (classic or slim)");
            } finally {
                out.close();
            }
        } catch (IOException e) {
            Log.warn("skins: could not save models: " + e);
        }
    }

    /** A PNG whose header says 64×64 pixels (the skin layout since 1.8; old 64×32 skins are not previewed or applied). */
    public static boolean isSkin(byte[] b) {
        if (b == null || b.length < 24) return false;
        int[] sig = {0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A};
        for (int i = 0; i < 8; i++) if ((b[i] & 0xFF) != sig[i]) return false;
        if (b[12] != 'I' || b[13] != 'H' || b[14] != 'D' || b[15] != 'R') return false;
        int w = be32(b, 16), h = be32(b, 20);
        return w == 64 && h == 64;
    }

    private static int be32(byte[] b, int o) {
        return (b[o] & 0xFF) << 24 | (b[o + 1] & 0xFF) << 16 | (b[o + 2] & 0xFF) << 8 | (b[o + 3] & 0xFF);
    }
}
