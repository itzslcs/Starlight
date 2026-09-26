package dev.mw19.addons.tiertags;

import dev.mw19.api.Gui;
import dev.mw19.api.PluginContext;
import dev.mw19.api.module.Category;
import dev.mw19.api.module.Module;
import dev.mw19.api.module.Rule;
import dev.mw19.api.name.NameDecorator;
import dev.mw19.api.net.Http;
import dev.mw19.api.render.Renderer;
import dev.mw19.api.setting.BoolSetting;
import dev.mw19.api.setting.ChoiceSetting;
import dev.mw19.api.setting.TextSetting;
import dev.mw19.api.util.Json;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Tier-list tags next to names. Network use: one HTTPS GET per visible premium player per 4 hours (cached, spaced
 * per host by the host client), only while this module is on. GRAY: extra info about other players, blocked on
 * Hypixel by serverrules.json.
 */
final class TierTagsModule extends Module implements NameDecorator {
    private static final String MCTIERS = "https://mctiers.com/api/v2", SUBTIERS = "https://subtiers.net/api/v2";
    private static final long HIT_TTL = 4 * 3600_000L, MISS_TTL = 3600_000L;

    private final PluginContext ctx;
    private final ChoiceSetting list = add(new ChoiceSetting("list", "Tier list", "Which tier list to show", "MCTiers", "MCTiers", "SubTiers", "Custom"));
    private final TextSetting custom = add(new TextSetting("custom_url", "Custom API", "Base URL of a list using the MCTiers v2 API (…/api/v2)", "", 200))
            .visibleWhen(() -> list.is("Custom"));
    private final ChoiceSetting mode = add(new ChoiceSetting("mode", "Gamemode", "Tier to show; Best = highest tier in any mode", "Best",
            "Best", "sword", "vanilla", "pot", "nethop", "smp", "uhc", "axe", "mace", "bed", "bow", "creeper", "debuff",
            "dia_crystal", "dia_smp", "elytra", "manhunt", "minecart", "og_vanilla", "speed", "trident"));
    private final BoolSetting nametag = add(new BoolSetting("nametag", "Nametags", "Show above players", true));
    private final BoolSetting tab = add(new BoolSetting("tab", "Tab list", "Show in the player list", true));
    private final BoolSetting peak = add(new BoolSetting("peak", "Peak for retired", "Retired players show their peak tier (R prefix)", true));
    private final BoolSetting showMode = add(new BoolSetting("show_mode", "Show gamemode", "Add the gamemode name to the tag", false));

    private static final class Result {
        long expires;
        boolean loading;
        Map<String, Object> rankings;
        String tag;
        String tagKey;
    }

    private final Map<UUID, Result> cache = new HashMap<UUID, Result>();
    private boolean active;
    private int lookups, found, errors;
    private String lastError = "";

    TierTagsModule(PluginContext ctx) {
        super("tiertags", "Tier Tags", "PvP tier-list ranks next to names (MCTiers, SubTiers…)", Category.ADDON, Rule.GRAY, false);
        this.ctx = ctx;
        Runnable reset = () -> cache.clear();
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

    @Override
    public String suffix(UUID uuid, String name, Placement placement) {
        if (!active || uuid == null || uuid.version() != 4) return null; // v4 = real (premium) accounts only
        if (placement == Placement.NAMETAG ? !nametag.on() : !tab.on()) return null;
        long now = System.currentTimeMillis();
        Result r = cache.get(uuid);
        if (r == null || (!r.loading && r.expires < now)) {
            request(uuid);
            return r == null ? null : r.tag;
        }
        if (r.rankings == null) return null;
        String key = mode.get() + peak.on() + showMode.on();
        if (!key.equals(r.tagKey)) {
            r.tagKey = key;
            r.tag = Tier.format(r.rankings, mode.get(), peak.on(), showMode.on());
        }
        return r.tag;
    }

    private void request(final UUID uuid) {
        String b = base();
        if (b == null) return;
        final Result r = cache.containsKey(uuid) ? cache.get(uuid) : new Result();
        r.loading = true;
        cache.put(uuid, r);
        lookups++;
        ctx.http().getJson(b + "/profile/" + uuid, HIT_TTL, new Http.Callback() {
            @Override
            public void done(int status, Object json, String error) {
                r.loading = false;
                if (status == 200) {
                    r.rankings = Json.obj(Json.obj(json).get("rankings"));
                    r.expires = System.currentTimeMillis() + HIT_TTL;
                    r.tagKey = null;
                    found++;
                } else if (status == 404) {
                    r.rankings = null;
                    r.tag = null;
                    r.expires = System.currentTimeMillis() + MISS_TTL;
                } else {
                    errors++;
                    lastError = error == null ? "HTTP " + status : error;
                    r.expires = System.currentTimeMillis() + 60_000; // offline / rate limited: try again later
                }
            }
        });
    }

    Gui.Panel panel() {
        return new Gui.Panel() {
            @Override
            public void render(Renderer r, float width, float height, float mouseX, float mouseY) {
                int y = 0;
                r.text("Source: " + list.get() + (base() == null ? " (set a https:// URL)" : "  " + base()), 0, y, 0xFFE0E0E0, false);
                y += 12;
                r.text("Looked up " + lookups + " players, " + found + " ranked, " + errors + " errors", 0, y, 0xFFB0B0B0, false);
                y += 12;
                if (!lastError.isEmpty()) r.text("Last error: " + lastError, 0, y, 0xFFFF7070, false);
                y += 16;
                r.text("Tags come from the public tier-list API; nothing is sent except the player's UUID.", 0, y, 0xFF909090, false);
            }
        };
    }
}
