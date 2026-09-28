package dev.starlight.core.modules;

import dev.starlight.api.util.Json;

import java.util.Map;

/** Picks and formats one ranking out of a tier-list profile (MCTiers API v2 schema), for the Tier Tagger. */
public final class TierFormat {
    private TierFormat() {}

    /**
     * A glyph per gamemode, instead of its name: every one is in the font Minecraft ships (unifont), so it draws on
     * every version and needs no texture of ours. A mode we have no glyph for (a custom list's own) keeps its name.
     */
    private static String icon(String mode) {
        if ("sword".equals(mode)) return "\u2694";        // crossed swords
        if ("vanilla".equals(mode)) return "\u2726";      // star
        if ("og_vanilla".equals(mode)) return "\u2606";   // hollow star
        if ("pot".equals(mode)) return "\u2697";          // alembic
        if ("nethop".equals(mode)) return "\u2668";       // hot springs (lava)
        if ("smp".equals(mode)) return "\u2302";          // house
        if ("dia_smp".equals(mode)) return "\u25C8";      // diamond in a diamond
        if ("dia_crystal".equals(mode)) return "\u2756";  // cut diamond
        if ("uhc".equals(mode)) return "\u2665";          // heart (no regeneration)
        if ("axe".equals(mode)) return "\u2692";          // hammer and pick
        if ("mace".equals(mode)) return "\u2604";         // comet (the smash)
        if ("bed".equals(mode)) return "\u26FA";          // tent
        if ("bow".equals(mode)) return "\u27B3";          // arrow
        if ("creeper".equals(mode)) return "\u2620";      // skull
        if ("debuff".equals(mode)) return "\u2623";       // biohazard
        if ("elytra".equals(mode)) return "\u2708";       // flight
        if ("manhunt".equals(mode)) return "\u2316";      // target
        if ("minecart".equals(mode)) return "\u26DF";     // cart
        if ("speed".equals(mode)) return "\u26A1";        // lightning
        if ("trident".equals(mode)) return "\u2646";      // trident (Neptune)
        return mode;
    }

    /** Colour per tier 1..5 (original palette). */
    private static final String[] COLOR = {"§c", "§6", "§e", "§a", "§7"};

    /**
     * @param rankings the profile's "rankings" object (mode -> {tier, pos, peak_tier, peak_pos, retired}); pos 0 = high
     * @param mode     a gamemode id or "Best" (the highest tier in any mode)
     * @return a tag such as "§8[§6HT2§8]", or null when the player is unranked there. With {@code showMode} the
     *         gamemode follows as a small glyph: "§8[§6HT2 §7⚔§8]"
     */
    public static String format(Map<String, Object> rankings, String mode, boolean usePeak, boolean showMode) {
        String bestMode = null;
        int bestScore = Integer.MAX_VALUE, bestTier = 0, bestPos = 0;
        boolean bestRetired = false;
        for (Map.Entry<String, Object> e : rankings.entrySet()) {
            if (!"Best".equals(mode) && !e.getKey().equals(mode)) continue;
            Map<String, Object> r = Json.obj(e.getValue());
            boolean retired = Json.bool(r, "retired", false);
            int tier = (int) Json.num(r, "tier", 0), pos = (int) Json.num(r, "pos", 1);
            if (retired && usePeak && r.get("peak_tier") instanceof Number) {
                tier = (int) Json.num(r, "peak_tier", tier);
                pos = (int) Json.num(r, "peak_pos", pos);
            }
            if (tier < 1 || tier > 5) continue;
            int score = tier * 2 + (pos == 0 ? 0 : 1); // HT1 < LT1 < HT2 ...
            if (score < bestScore) {
                bestScore = score;
                bestTier = tier;
                bestPos = pos;
                bestRetired = retired;
                bestMode = e.getKey();
            }
        }
        if (bestMode == null) return null;
        String tag = (bestRetired ? "R" : "") + (bestPos == 0 ? "HT" : "LT") + bestTier;
        return "§8[" + COLOR[bestTier - 1] + tag + (showMode ? " §7" + icon(bestMode) : "") + "§8]";
    }
}
