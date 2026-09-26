package dev.mw19.api.name;

import java.util.UUID;

/**
 * Adds text next to player names. Called for every visible name, so return cached data
 * (fetch in the background, return null until ready). Text may use '§' codes.
 */
public interface NameDecorator {
    String suffix(UUID uuid, String name, Placement placement);

    enum Placement { NAMETAG, TAB_LIST }
}
