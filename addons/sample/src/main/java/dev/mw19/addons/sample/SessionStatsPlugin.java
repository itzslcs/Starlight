package dev.mw19.addons.sample;

import dev.mw19.api.ConfigStore;
import dev.mw19.api.Gui;
import dev.mw19.api.Plugin;
import dev.mw19.api.PluginContext;
import dev.mw19.api.event.ChatReceivedEvent;
import dev.mw19.api.event.ServerEvent;
import dev.mw19.api.hud.Anchor;
import dev.mw19.api.hud.HudStyle;
import dev.mw19.api.module.HudModule;
import dev.mw19.api.module.Rule;
import dev.mw19.api.render.Renderer;
import dev.mw19.api.setting.BoolSetting;

/**
 * Reference plugin for docs/PLUGIN_API.md. Shows every API area in ~100 lines: a HUD module with settings,
 * events, a repeating scheduler task, persistent config, a GUI panel and a toast.
 */
public final class SessionStatsPlugin implements Plugin {
    private PluginContext ctx;
    private long sessionStart;
    private int servers, chatLines;

    @Override
    public void onEnable(PluginContext ctx) {
        this.ctx = ctx;
        this.sessionStart = System.currentTimeMillis();
        final ConfigStore cfg = ctx.config();

        ctx.registerModule(new Timer());
        ctx.events().on(ServerEvent.class, e -> {
            if (e.joined) servers++;
        });
        ctx.events().on(ChatReceivedEvent.class, e -> chatLines++);
        // Persist lifetime play time once a minute (1200 ticks).
        ctx.scheduler().every(1200, () -> cfg.setNumber("lifetime_minutes", cfg.getNumber("lifetime_minutes", 0) + 1));
        ctx.gui().registerPanel("Session", new Gui.Panel() {
            @Override
            public void render(Renderer r, float w, float h, float mx, float my) {
                r.text("This session: " + minutes() + " min, " + servers + " server(s), " + chatLines + " chat line(s)", 0, 0, 0xFFE0E0E0, false);
                r.text("Lifetime: " + (long) cfg.getNumber("lifetime_minutes", 0) + " min", 0, 12, 0xFFB0B0B0, false);
                boolean hover = mx >= 0 && my >= 26 && mx < 80 && my < 40;
                r.roundRect(0, 26, 80, 14, 3, hover ? 0xFF3A3F4B : 0xFF2A2E37);
                r.text("Reset lifetime", 6, 29, 0xFFFFFFFF, false);
            }

            @Override
            public boolean mouseClicked(float mx, float my, int button) {
                if (mx >= 0 && my >= 26 && mx < 80 && my < 40) {
                    cfg.setNumber("lifetime_minutes", 0);
                    SessionStatsPlugin.this.ctx.gui().toast("Session Stats", "Lifetime total reset");
                    return true;
                }
                return false;
            }
        });
        ctx.logger().info("Session Stats sample plugin enabled");
    }

    private long minutes() {
        return (System.currentTimeMillis() - sessionStart) / 60000;
    }

    /** HUD element: "Session 1:23:45". Text only changes once a second, so it is cached. */
    private final class Timer extends HudModule {
        private final BoolSetting seconds = add(new BoolSetting("seconds", "Seconds", "Show seconds", true));
        private long shown = -1;
        private String text = "";

        Timer() {
            super("session_stats", "Session Timer", "How long you have been playing this session (sample plugin)", Rule.ALLOWED, false,
                    Anchor.BOTTOM_RIGHT, 4, 52);
        }

        private String text() {
            long s = (System.currentTimeMillis() - sessionStart) / 1000;
            long key = seconds.on() ? s : s / 60;
            if (key != shown) {
                shown = key;
                long h = s / 3600, m = s / 60 % 60, sec = s % 60;
                text = "Session " + h + ":" + (m < 10 ? "0" : "") + m + (seconds.on() ? ":" + (sec < 10 ? "0" : "") + sec : "");
            }
            return text;
        }

        @Override
        public float width(Renderer r) {
            return r.textWidth(text());
        }

        @Override
        public float height(Renderer r) {
            return r.lineHeight() - 1;
        }

        @Override
        public void render(Renderer r, HudStyle style, boolean preview) {
            r.text(text(), 0, 0, style.textColor(), style.textShadow());
        }
    }
}
