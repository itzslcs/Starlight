package dev.kestrel.core.modules;

import dev.kestrel.api.hud.Anchor;
import dev.kestrel.api.module.Rule;
import dev.kestrel.api.setting.BoolSetting;
import dev.kestrel.api.setting.ChoiceSetting;
import dev.kestrel.core.Kestrel;

import java.util.TimeZone;

/** Real-world or in-game time. */
public final class ClockModule extends TextHud {
    private final ChoiceSetting source = add(new ChoiceSetting("source", "Time", "Which clock", "Real", "Real", "In-game"));
    private final ChoiceSetting format = add(new ChoiceSetting("format", "Format", "12 or 24 hour", "24h", "24h", "12h"));
    private final BoolSetting seconds = add(new BoolSetting("seconds", "Seconds", "Show seconds (real time only)", false));
    private int h, m, s;

    public ClockModule() {
        super("clock", "Clock", "Real or in-game time", Rule.ALLOWED, false, Anchor.BOTTOM_RIGHT, 4, 4);
    }

    @Override
    protected long key() {
        if (source.is("In-game")) {
            long t = Kestrel.get().platform.dayTime();
            if (t < 0) {
                h = m = s = -1;
            } else {
                long day = (t + 6000) % 24000; // tick 0 = 06:00
                h = (int) (day / 1000);
                m = (int) ((day % 1000) * 60 / 1000);
                s = 0;
            }
        } else {
            long now = System.currentTimeMillis();
            long local = now + TimeZone.getDefault().getOffset(now);
            long sec = local / 1000;
            s = (int) (sec % 60);
            m = (int) (sec / 60 % 60);
            h = (int) (sec / 3600 % 24);
        }
        return ((long) h << 16) | (m << 8) | (seconds.on() && source.is("Real") ? s : 0)
                | (format.is("12h") ? 1L << 40 : 0) | (source.is("Real") ? 1L << 41 : 0);
    }

    @Override
    protected String build() {
        if (h < 0) return "--:--";
        int hh = h;
        String suffix = "";
        if (format.is("12h")) {
            suffix = hh < 12 ? " AM" : " PM";
            hh = hh % 12 == 0 ? 12 : hh % 12;
        }
        String t = (hh < 10 && format.is("24h") ? "0" : "") + hh + ":" + (m < 10 ? "0" : "") + m;
        if (seconds.on() && source.is("Real")) t += ":" + (s < 10 ? "0" : "") + s;
        return t + suffix;
    }
}
