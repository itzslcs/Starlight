package dev.starlight.core;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * The client's folder in the game directory, {@code Starlight/}. A folder from before a rename ({@code MW19/} from
 * 0.1–0.7, and before that {@code Kestrel/}) is moved over once, by whichever part of the client asks first: the
 * renderer switch runs before the client starts (DECISIONS D-033).
 */
public final class ConfigFolder {
    public static final String NAME = "Starlight";
    private static final String[] OLD = {"MW19", "Kestrel"};

    private ConfigFolder() {}

    public static Path of(Path gameDir) {
        Path dir = gameDir.resolve(NAME);
        if (Files.exists(dir)) return dir;
        for (String name : OLD) {
            Path old = gameDir.resolve(name);
            if (!Files.isDirectory(old)) continue;
            try {
                Files.move(old, dir);
            } catch (IOException e) {
                System.err.println("[Starlight] could not move " + old + " to " + dir + ": " + e);
            }
            break;
        }
        return dir;
    }
}
