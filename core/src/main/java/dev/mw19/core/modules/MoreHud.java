package dev.mw19.core.modules;

import dev.mw19.api.Subscription;
import dev.mw19.api.event.KeyPressEvent;
import dev.mw19.api.hud.Anchor;
import dev.mw19.api.module.Category;
import dev.mw19.api.module.Module;
import dev.mw19.api.module.Rule;
import dev.mw19.api.setting.KeySetting;
import dev.mw19.api.setting.NumberSetting;
import dev.mw19.api.util.Colors;
import dev.mw19.core.Mw19;
import dev.mw19.core.module.Overlay;
import dev.mw19.core.platform.Platform;
import dev.mw19.core.render.Gfx;

import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/** Small HUD elements added in 0.3.0 (each is its own module; grouped here like SimpleVisuals). */
public final class MoreHud {
    private MoreHud() {}

    static String duration(long ms, boolean tenths) {
        long s = ms / 1000;
        String t = s >= 3600 ? String.format(Locale.ROOT, "%d:%02d:%02d", s / 3600, s / 60 % 60, s % 60)
                : String.format(Locale.ROOT, "%d:%02d", s / 60, s % 60);
        return tenths ? t + "." + (ms / 100 % 10) : t;
    }

    /** Food saturation, the hidden reserve that is used before the hunger bar drops. GRAY: vanilla never shows it. */
    public static final class Saturation extends TextHud {
        private int food, tenths;

        public Saturation() {
            super("saturation", "Saturation", "Your hunger and hidden saturation reserve", Rule.GRAY, false, Anchor.TOP_RIGHT, 4, 64);
        }

        @Override
        protected boolean hasContent() {
            return Mw19.get().platform.inWorld();
        }

        @Override
        protected long key() {
            Platform p = Mw19.get().platform;
            food = p.food();
            tenths = Math.round(p.saturation() * 10);
            return (long) food << 32 | tenths;
        }

        @Override
        protected String build() {
            return String.format(Locale.ROOT, "Food %d · Saturation %.1f", food, tenths / 10.0);
        }
    }

    /** Time since you joined the current world or server. */
    public static final class SessionTime extends TextHud {
        private long since;

        public SessionTime() {
            super("session_time", "Session Time", "How long you have been in this world or server", Rule.ALLOWED, false, Anchor.TOP_RIGHT, 4, 40);
        }

        @Override
        public void onTick() {
            boolean in = Mw19.get().platform.inWorld();
            if (in && since == 0) since = System.currentTimeMillis();
            if (!in) since = 0;
        }

        @Override
        protected boolean hasContent() {
            return since > 0;
        }

        @Override
        protected long key() {
            return since == 0 ? -1 : (System.currentTimeMillis() - since) / 1000;
        }

        @Override
        protected String build() {
            return "Session " + (since == 0 ? "0:00" : duration(System.currentTimeMillis() - since, false));
        }
    }

    /** The resource pack on top of your list. */
    public static final class PackDisplay extends TextHud {
        private String top = "";
        private long checked;

        public PackDisplay() {
            super("pack_display", "Pack Display", "The resource pack you are using", Rule.ALLOWED, false, Anchor.BOTTOM_LEFT, 4, 4);
        }

        @Override
        protected boolean hasContent() {
            return !top.isEmpty();
        }

        @Override
        protected long key() {
            long now = System.currentTimeMillis();
            if (now - checked > 2000) {
                checked = now;
                List<String> packs = Mw19.get().platform.packs().enabled();
                String t = packs.isEmpty() ? "" : packs.get(0);
                top = t.toLowerCase(Locale.ROOT).endsWith(".zip") ? t.substring(0, t.length() - 4) : t;
            }
            return top.hashCode();
        }

        @Override
        protected String build() {
            return top;
        }

        @Override
        protected String previewText() {
            return "My Pack";
        }
    }

    /** A stopwatch on a key: press to start or pause, the reset key sets it back to zero. */
    public static final class Stopwatch extends TextHud {
        private final KeySetting toggle = add(new KeySetting("toggle_key", "Start/pause key", "Starts or pauses the stopwatch", KeySetting.NONE));
        private final KeySetting reset = add(new KeySetting("reset_key", "Reset key", "Sets it back to zero", KeySetting.NONE));
        private long startedAt, stored;
        private boolean running;
        private Subscription sub;

        public Stopwatch() {
            super("stopwatch", "Stopwatch", "A timer you start and stop with a key", Rule.ALLOWED, false, Anchor.TOP, 0, 80);
        }

        @Override
        public void onEnable() {
            sub = Mw19.get().events.on(KeyPressEvent.class, new Consumer<KeyPressEvent>() {
                @Override
                public void accept(KeyPressEvent e) {
                    long now = System.currentTimeMillis();
                    if (toggle.bound() && e.key == toggle.key()) {
                        if (running) stored += now - startedAt;
                        else startedAt = now;
                        running = !running;
                    } else if (reset.bound() && e.key == reset.key()) {
                        running = false;
                        stored = 0;
                    }
                }
            });
        }

        @Override
        public void onDisable() {
            if (sub != null) sub.cancel();
        }

        private long elapsed() {
            return stored + (running ? System.currentTimeMillis() - startedAt : 0);
        }

        @Override
        protected long key() {
            return elapsed() / 100;
        }

        @Override
        protected String build() {
            return duration(elapsed(), true);
        }
    }

    /** The world's day number, as F3 shows it. */
    public static final class DayCounter extends TextHud {
        private long day = -1;

        public DayCounter() {
            super("day_counter", "Day Counter", "Which in-game day it is (same as F3)", Rule.ALLOWED, false, Anchor.TOP_RIGHT, 4, 52);
        }

        @Override
        protected boolean hasContent() {
            return Mw19.get().platform.dayTime() >= 0;
        }

        @Override
        protected long key() {
            long t = Mw19.get().platform.dayTime();
            day = t < 0 ? -1 : t / 24000;
            return day;
        }

        @Override
        protected String build() {
            return day < 0 ? "Day -" : "Day " + day;
        }

        @Override
        protected String previewText() {
            return "Day 12";
        }
    }

    /** A pulsing red edge while your health is low. Your own health, already shown by the hearts. */
    public static final class LowHealth extends Module implements Overlay {
        private final NumberSetting below = add(new NumberSetting("below", "Below", "Health share that starts the warning (percent)", 30, 5, 80, 5));

        public LowHealth() {
            super("low_health", "Low Health Warning", "Red screen edges while your health is low", Category.VISUAL, Rule.ALLOWED, false);
        }

        @Override
        public void renderOverlay(Gfx g, float w, float h) {
            Platform p = Mw19.get().platform;
            float max = p.maxHealth();
            if (!p.inWorld() || max <= 0) return;
            float share = p.health() / max, limit = below.floatValue() / 100f;
            if (share >= limit || p.health() <= 0) return;
            float strength = 1f - share / limit;
            float pulse = 0.55f + 0.45f * (float) Math.sin(g.millis() / 1000.0 * Math.PI * (1.2 + strength));
            int red = Colors.fade(0xFFD01010, Math.min(0.75f, 0.25f + 0.5f * strength) * pulse);
            float e = Math.min(w, h) * 0.12f;
            g.gradient(0, 0, w, e, red, 0x00D01010);
            g.gradient(0, h - e, w, h, 0x00D01010, red);
            g.gradientH(0, 0, e, h, red, 0x00D01010);
            g.gradientH(w - e, 0, w, h, 0x00D01010, red);
        }
    }
}
