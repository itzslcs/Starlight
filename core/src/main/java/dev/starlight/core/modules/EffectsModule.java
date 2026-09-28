package dev.starlight.core.modules;

import dev.starlight.api.hud.Anchor;
import dev.starlight.api.hud.HudStyle;
import dev.starlight.api.module.HudModule;
import dev.starlight.api.module.Rule;
import dev.starlight.api.render.Renderer;
import dev.starlight.api.setting.BoolSetting;
import dev.starlight.api.setting.ChoiceSetting;
import dev.starlight.core.Starlight;
import dev.starlight.core.platform.Platform;

/** Active potion effects outside the inventory (listed as allowed by Hypixel: "Effect Status"). */
public final class EffectsModule extends HudModule {
    private static final int MAX = 32;
    private static final String[] ROMAN = {"", " II", " III", " IV", " V", " VI", " VII", " VIII", " IX", " X"};

    private final ChoiceSetting sort = add(new ChoiceSetting("sort", "Sort", "Order of effects", "Duration", "Duration", "Name", "None"));
    private final BoolSetting colorBar = add(new BoolSetting("color_bar", "Colour bar", "Effect colour next to the name", true));
    private final BoolSetting blink = add(new BoolSetting("blink", "Blink when ending", "Blink during the last 10 seconds", true));
    private final BoolSetting hideInfinite = add(new BoolSetting("hide_infinite", "Hide infinite", "Hide effects without a timer (e.g. beacons)", false));

    // pooled entries (no per-frame allocation)
    private final String[] name = new String[MAX], text = new String[MAX];
    private final int[] amp = new int[MAX], dur = new int[MAX], rgb = new int[MAX], shownSec = new int[MAX];
    private int n;
    private final Platform.EffectSink sink = new Platform.EffectSink() {
        @Override
        public void accept(String nm, int amplifier, int durationTicks, int color, boolean beneficial) {
            if (n >= MAX || (hideInfinite.on() && durationTicks < 0)) return;
            name[n] = nm;
            amp[n] = amplifier;
            dur[n] = durationTicks;
            rgb[n] = color;
            n++;
        }
    };

    public EffectsModule() {
        super("effects", "Potion Effects", "Your active effects and timers", Rule.ALLOWED, true, Anchor.TOP_RIGHT, 4, 4);
    }

    /** Collected per tick (names come from translated components, which allocate). */
    @Override
    public void onTick() {
        collect();
    }

    private void collect() {
        n = 0;
        if (Starlight.get().platform.inWorld()) Starlight.get().platform.effects(sink);
        // insertion sort (n is tiny) keeps it allocation-free
        for (int i = 1; i < n; i++) {
            for (int j = i; j > 0 && before(j, j - 1); j--) swap(j, j - 1);
        }
        for (int i = 0; i < n; i++) {
            int sec = dur[i] < 0 ? -1 : dur[i] / 20;
            int key = sec * 16 + amp[i];
            if (text[i] == null || shownSec[i] != key || !text[i].startsWith(name[i])) {
                shownSec[i] = key;
                String a = amp[i] >= 0 && amp[i] < ROMAN.length ? ROMAN[amp[i]] : " " + (amp[i] + 1);
                text[i] = name[i] + a + (sec < 0 ? "" : "  " + (sec / 60) + ":" + (sec % 60 < 10 ? "0" : "") + (sec % 60));
            }
        }
    }

    private boolean before(int a, int b) {
        if (sort.is("Duration")) {
            int da = dur[a] < 0 ? Integer.MAX_VALUE : dur[a], db = dur[b] < 0 ? Integer.MAX_VALUE : dur[b];
            return da < db;
        }
        return sort.is("Name") && name[a].compareTo(name[b]) < 0;
    }

    private void swap(int a, int b) {
        String s = name[a];
        name[a] = name[b];
        name[b] = s;
        s = text[a];
        text[a] = text[b];
        text[b] = s;
        int t = amp[a];
        amp[a] = amp[b];
        amp[b] = t;
        t = dur[a];
        dur[a] = dur[b];
        dur[b] = t;
        t = rgb[a];
        rgb[a] = rgb[b];
        rgb[b] = t;
        t = shownSec[a];
        shownSec[a] = shownSec[b];
        shownSec[b] = t;
    }

    @Override
    public boolean visible(boolean preview) {
        return preview || n > 0;
    }

    @Override
    public float width(Renderer r) {
        if (n == 0) return r.textWidth("Speed II  1:23") + 5;
        float w = 0;
        for (int i = 0; i < n; i++) w = Math.max(w, r.textWidth(text[i]));
        return w + (colorBar.on() ? 5 : 0);
    }

    @Override
    public float height(Renderer r) {
        return Math.max(1, n) * 11 - 2;
    }

    @Override
    public void render(Renderer r, HudStyle style, boolean preview) {
        if (n == 0) {
            if (colorBar.on()) r.roundRect(0, 0, 2, 8, 1, 0xFF7CAFC6);
            r.text("Speed II  1:23", colorBar.on() ? 5 : 0, 0, style.textColor(), style.textShadow());
            return;
        }
        long now = r.millis();
        for (int i = 0; i < n; i++) {
            float y = i * 11;
            boolean ending = blink.on() && dur[i] >= 0 && dur[i] < 200;
            if (ending && (now / 250) % 2 == 0) continue;
            float x = 0;
            if (colorBar.on()) {
                r.roundRect(0, y, 2, 8, 1, 0xFF000000 | rgb[i]);
                x = 5;
            }
            r.text(text[i], x, y, style.textColor(), style.textShadow());
        }
    }
}
