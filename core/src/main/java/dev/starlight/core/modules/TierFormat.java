package dev.starlight.core.modules;

import dev.starlight.api.util.Json;

import java.util.Map;

/** Picks and formats one ranking out of a tier-list profile (MCTiers API v2 schema), for the Tier Tagger. */
public final class TierFormat {
    private TierFormat() {}

    /** Colour per tier 1..5 (original palette). */
    private static final String[] COLOR = {"§c", "§6", "§e", "§a", "§7"};

    /**
     * @param rankings the profile's "rankings" object (mode -> {tier, pos, peak_tier, peak_pos, retired}); pos 0 = high
     * @param mode     a gamemode id or "Best" (the highest tier in any mode)
     * @return a tag such as "§8[§6HT2§8]", or null when the player is unranked there
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
        return "§8[" + COLOR[bestTier - 1] + tag + (showMode ? " §7" + bestMode : "") + "§8]";
    }
}
