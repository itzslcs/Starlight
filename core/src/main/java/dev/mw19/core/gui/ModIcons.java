package dev.mw19.core.gui;

import java.util.HashMap;
import java.util.Map;

/**
 * The item shown on each module's tile in the Mods page. Ids exist from 1.8.9 to 26.x unless an alternative follows a
 * "|" (the first one the game knows is drawn); a module without an entry, or on a version without the item, shows its
 * initial instead.
 */
public final class ModIcons {
    private static final Map<String, String[]> ICONS = new HashMap<String, String[]>();

    static {
        put("fps", "minecraft:repeater");
        put("keycps", "minecraft:note_block|minecraft:noteblock");
        put("speed", "minecraft:sugar");
        put("ping", "minecraft:ender_pearl");
        put("coords", "minecraft:map");
        put("direction", "minecraft:compass");
        put("armor", "minecraft:iron_chestplate");
        put("effects", "minecraft:potion");
        put("item_counter", "minecraft:arrow");
        put("clock", "minecraft:clock");
        put("system", "minecraft:comparator");
        put("fps_graph", "minecraft:paper");
        put("server_address", "minecraft:name_tag");
        put("combo", "minecraft:diamond_sword");
        put("crosshair", "minecraft:bow");
        put("zoom", "minecraft:spyglass|minecraft:ender_eye");
        put("toggle_sprint", "minecraft:feather");
        put("toggle_sneak", "minecraft:leather_boots");
        put("fullbright", "minecraft:glowstone_dust");
        put("hit_color", "minecraft:red_dye|minecraft:redstone");
        put("damage_tilt", "minecraft:golden_apple");
        put("fire_overlay", "minecraft:blaze_powder");
        put("shield_overlay", "minecraft:shield");
        put("particles", "minecraft:nether_star");
        put("chat", "minecraft:writable_book");
        put("entity_culling", "minecraft:ender_eye");
        put("fast_chests", "minecraft:chest");
        put("clear_weather", "minecraft:water_bucket");
        put("exploit_protection", "minecraft:iron_door");
        put("saturation", "minecraft:cooked_beef");
        put("session_time", "minecraft:clock");
        put("pack_display", "minecraft:painting");
        put("stopwatch", "minecraft:clock");
        put("day_counter", "minecraft:red_bed|minecraft:bed");
        put("low_health", "minecraft:poppy|minecraft:red_flower");
        put("hitboxes", "minecraft:glass");
        put("tnt_timer", "minecraft:tnt");
        put("reach", "minecraft:blaze_rod");
        put("quick_commands", "minecraft:command_block");
        put("durability_alert", "minecraft:anvil");
        put("screenshot", "minecraft:item_frame");
        put("freelook", "minecraft:skeleton_skull|minecraft:skull");
        put("own_nametag", "minecraft:name_tag");
        put("old_animations", "minecraft:fishing_rod");
    }

    private ModIcons() {}

    private static void put(String module, String items) {
        ICONS.put(module, items.split("\\|"));
    }

    /** Candidate item ids for a module's icon, best first (empty when it has none). */
    public static String[] of(String moduleId) {
        String[] ids = ICONS.get(moduleId);
        return ids == null ? new String[0] : ids;
    }
}
