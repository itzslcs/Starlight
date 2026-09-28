package dev.starlight.core.platform;

import java.util.List;

public interface ModList {
    boolean isLoaded(String modId);

    /** "id version" strings of every loaded mod, for the About page and crash reports. */
    List<String> describe();
}
