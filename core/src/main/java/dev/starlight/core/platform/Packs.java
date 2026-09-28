package dev.starlight.core.platform;

import java.nio.file.Path;
import java.util.List;

/** The game's resource packs (game thread). */
public interface Packs {
    Path folder();

    /** File names (in {@link #folder()}) of the enabled resource packs, top of the list first. */
    List<String> enabled();

    /** Enables {@code fileName} from {@link #folder()} on top of the others and reloads resources. False if unknown. */
    boolean enable(String fileName);
}
