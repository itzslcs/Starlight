package dev.mw19.api.render;

/** Read-only view of an item stack; only valid during the call that returned it. */
public interface ItemRef {
    boolean isEmpty();

    /** Namespaced id, e.g. "minecraft:diamond_sword". */
    String id();

    int count();

    int damage();

    int maxDamage();

    String displayName();
}
