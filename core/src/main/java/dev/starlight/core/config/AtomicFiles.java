package dev.starlight.core.config;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/** Crash-safe file writes and rolling backups. */
public final class AtomicFiles {
    private AtomicFiles() {}

    /** Writes via temp file + fsync + atomic rename, so a crash leaves either the old or the new file. */
    public static void write(Path file, String content) throws IOException {
        Files.createDirectories(file.getParent());
        Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
        FileChannel ch = FileChannel.open(tmp, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
        try {
            ByteBuffer buf = ByteBuffer.wrap(bytes);
            while (buf.hasRemaining()) ch.write(buf);
            ch.force(true);
        } finally {
            ch.close();
        }
        try {
            Files.move(tmp, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    public static String read(Path file) throws IOException {
        return new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
    }

    /** Copies {@code file} to {@code dir/<name>.<timestamp>.json} and keeps the newest {@code keep} copies of it. */
    public static Path backup(Path file, Path dir, int keep) throws IOException {
        if (!Files.exists(file)) return null;
        Files.createDirectories(dir);
        String base = stem(file);
        String ts = new SimpleDateFormat("yyyyMMdd-HHmmss-SSS").format(new Date());
        Path target = dir.resolve(base + "." + ts + ".json");
        Files.copy(file, target, StandardCopyOption.REPLACE_EXISTING);
        List<Path> all = backupsOf(file, dir);
        for (int i = 0; i < all.size() - keep; i++) Files.deleteIfExists(all.get(i));
        return target;
    }

    /** Backups of {@code file}, oldest first. */
    public static List<Path> backupsOf(Path file, Path dir) throws IOException {
        List<Path> out = new ArrayList<Path>();
        if (!Files.isDirectory(dir)) return out;
        String prefix = stem(file) + ".";
        DirectoryStream<Path> ds = Files.newDirectoryStream(dir);
        try {
            for (Path p : ds) {
                String n = p.getFileName().toString();
                if (n.startsWith(prefix) && n.endsWith(".json") && n.length() == prefix.length() + 24) out.add(p);
            }
        } finally {
            ds.close();
        }
        Collections.sort(out);
        return out;
    }

    /** Moves an unreadable file aside so it is never overwritten or lost. */
    public static Path quarantine(Path file) {
        try {
            Path target = file.resolveSibling(stem(file) + ".corrupt-" + System.currentTimeMillis() + ".json");
            Files.move(file, target, StandardCopyOption.REPLACE_EXISTING);
            return target;
        } catch (IOException e) {
            return null;
        }
    }

    static String stem(Path file) {
        String n = file.getFileName().toString();
        return n.endsWith(".json") ? n.substring(0, n.length() - 5) : n;
    }
}
