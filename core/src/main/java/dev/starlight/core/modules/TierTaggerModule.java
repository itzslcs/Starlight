package dev.starlight.core.modules;

import dev.starlight.api.module.Category;
import dev.starlight.api.module.Module;
import dev.starlight.api.module.Rule;
import dev.starlight.api.net.Http;
import dev.starlight.api.setting.BoolSetting;
import dev.starlight.api.setting.ChoiceSetting;
import dev.starlight.api.setting.TextSetting;
import dev.starlight.api.util.Json;
import dev.starlight.core.Starlight;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * PvP tier-list ranks next to player names, on nametags and in the tab list (MCTiers by default, SubTiers, or any list
 * that serves the MCTiers v2 API). Built in again after the plugin system went (it was the Tier Tags addon, D-026).
 *
 * <p>Network use, only while the module is on: one HTTPS GET of {@code <list>/profile/<uuid>} per visible player with a
 * real (premium) account, each answer (ranked or not) kept for 4 hours. Nothing is sent except the player's UUID. Off by
 * default; extra information about other players is outside Hypixel's policy, so serverrules.json turns it off there
 * (DISALLOWED@hypixel). Game thread only (HTTP answers arrive there).
 */
public final class TierTaggerModule extends Module {
    static final String MCTIERS = "https://mctiers.com/api/v2", SUBTIERS = "https://subtiers.net/api/v2";
    private static final long TTL = 4 * 3600_000L, RETRY = 60_000L;

    private final ChoiceSetting list = add(new ChoiceSetting("list", "Tier list", "Which tier list to show", "MCTiers", "MCTiers", "SubTiers", "Custom"));
    private final TextSetting custom = add(new TextSetting("custom_url", "Custom API", "Base address of a list using the MCTiers v2 API (https://…/api/v2)", "", 200))
            .visibleWhen(() -> list.is("Custom"));
    private final ChoiceSetting mode = add(new ChoiceSetting("mode", "Gamemode", "Tier to show; Best = the highest tier in any mode", "Best",
            "Best", "sword", "vanilla", "pot", "nethop", "smp", "uhc", "axe", "mace", "bed", "bow", "creeper", "debuff",
            "dia_crystal", "dia_smp", "elytra", "manhunt", "minecart", "og_vanilla", "speed", "trident"));
    private final BoolSetting nametag = add(new BoolSetting("nametag", "Nametags", "Show the tier above players", true));
    private final BoolSetting tab = add(new BoolSetting("tab", "Tab list", "Show the tier in the player list", true));
    private final BoolSetting peak = add(new BoolSetting("peak", "Peak for retired", "Retired players show their peak tier (R prefix)", true));
    private final BoolSetting showMode = add(new BoolSetting("show_mode", "Show gamemode", "Add the gamemode's name to the tag", false));

    /** One player's lookup. The tag (with its leading space) is formatted again only when the settings change. */
    private static final class Result {
        long expires;
        boolean loading;
        Map<String, Object> rankings;
        String tag, tagKey;
    }

    private final Map<UUID, Result> cache = new HashMap<UUID, Result>();
    private UUID pinned, pinnedAs;
    private boolean active;
    private int lookups, found, errors;
    private String lastError = "";

    public TierTaggerModule() {
        super("tiertagger", "Tier Tagger", "PvP tier-list ranks next to names (MCTiers, SubTiers or your own list). Looks players up online",
                Category.UTILITY, Rule.DISALLOWED_ON_SOME_SERVERS, false);
        Runnable reset = new Runnable() {
            @Override
            public void run() {
                cache.clear();
            }
        };
        list.addListener(reset);
        custom.addListener(reset);
    }

    @Override
    public void onEnable() {
        active = true;
    }

    @Override
    public void onDisable() {
        active = false;
    }

    private String base() {
        if (list.is("SubTiers")) return SUBTIERS;
        if (list.is("Custom")) {
            String u = custom.get().trim();
            while (u.endsWith("/")) u = u.substring(0, u.length() - 1);
            return u.startsWith("https://") ? u : null;
        }
        return MCTIERS;
    }

    /** The tag to put after a player's name (with a leading space), or null. Asked by the nametag and tab list hooks. */
    public String suffix(UUID uuid, boolean tabList) {
        if (uuid != null && uuid.equals(pinned)) uuid = pinnedAs;
        if (!active || uuid == null || uuid.version() != 4) return null; // version 4 = a real (premium) account
        if (tabList ? !tab.on() : !nametag.on()) return null;
        Result r = cache.get(uuid);
        if (r == null || (!r.loading && r.expires < System.currentTimeMillis())) {
            r = request(uuid, r);
            if (r == null) return null;
        }
        if (r.rankings == null) return null;
        String key = mode.get() + peak.on() + showMode.on();
        if (!key.equals(r.tagKey)) {
            r.tagKey = key;
            String t = TierFormat.format(r.rankings, mode.get(), peak.on(), showMode.on());
            r.tag = t == null ? null : " " + t;
        }
        return r.tag;
    }

    private Result request(UUID uuid, Result existing) {
        String b = base();
        Starlight k = Starlight.get();
        if (b == null || k == null) return existing;
        if (existing == null && cache.size() > 4096) cache.clear(); // a very long session on a big server: start over
        final Result r = existing != null ? existing : new Result();
        r.loading = true;
        cache.put(uuid, r);
        lookups++;
        k.http.getJson(b + "/profile/" + uuid, TTL, new Http.Callback() {
            @Override
            public void done(int status, Object json, String error) {
                r.loading = false;
                if (status == 200) {
                    r.rankings = Json.obj(Json.obj(json).get("rankings"));
                    r.expires = System.currentTimeMillis() + TTL;
                    r.tagKey = null;
                    found++;
                } else if (status == 404) {
                    r.rankings = null;
                    r.expires = System.currentTimeMillis() + TTL; // the HTTP cache keeps a 404 as long
                } else {
                    errors++;
                    lastError = error == null ? "HTTP " + status : error;
                    r.expires = System.currentTimeMillis() + RETRY; // offline or rate limited: try again later
                }
            }
        });
        return r;
    }

    /** Smoke only: {@code player} shows {@code as}'s tier (the test player is offline, so it has none); nulls end it. */
    public void pin(UUID player, UUID as) {
        pinned = player;
        pinnedAs = as;
    }

    /** One line on what the module has done (smoke log, settings). */
    public String status() {
        return list.get() + (base() == null ? " (set an https:// address)" : "") + ": looked up " + lookups + ", ranked " + found
                + (errors > 0 ? ", " + errors + " errors (" + lastError + ")" : "");
    }
}
