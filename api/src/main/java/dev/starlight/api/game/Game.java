package dev.starlight.api.game;

import dev.starlight.api.render.ItemRef;

import java.util.UUID;

/** Read-only view of the running game. Call from the game thread only. */
public interface Game {
    String minecraftVersion();

    /** "fabric" or "forge". */
    String loader();

    boolean inWorld();

    String playerName();

    UUID playerUuid();

    double x();

    double y();

    double z();

    float yaw();

    float pitch();

    int fps();

    /** Own latency in ms, or -1 when unknown. */
    int ping();

    /** Server address as typed by the user, or null in singleplayer / menus. */
    String serverAddress();

    boolean singleplayer();

    /** Armor by slot: 0 feet, 1 legs, 2 chest, 3 head. Never null (empty ItemRef instead). */
    ItemRef armor(int slot);

    ItemRef mainHand();

    ItemRef offHand();

    /** Total count of items with this id in the player's inventory (hotbar + main + offhand). */
    int countItem(String itemId);

    boolean isKeyDown(int key);

    /** Vanilla key bindings the HUD cares about (respects the player's rebinds). */
    enum Binding { FORWARD, LEFT, BACK, RIGHT, JUMP, SNEAK, SPRINT, ATTACK, USE, SCREENSHOT }

    boolean bindingDown(Binding b);

    /** Short display name of the key bound to {@code b}, e.g. "W" or "LMB". */
    String bindingName(Binding b);

    String keyName(int key);

    int screenWidth();

    int screenHeight();

    /** Target capability flags, e.g. "offhand", "shield". */
    boolean supports(String feature);
}
