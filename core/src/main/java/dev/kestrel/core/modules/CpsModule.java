package dev.kestrel.core.modules;

import dev.kestrel.api.hud.Anchor;
import dev.kestrel.api.module.Rule;
import dev.kestrel.api.setting.BoolSetting;

/** Clicks per second from real mouse presses (display only; never generates input). */
public final class CpsModule extends TextHud {
    private final ClickTracker clicks;
    private final BoolSetting right = add(new BoolSetting("right", "Right button", "Also show right-click CPS", true));

    public CpsModule(ClickTracker clicks) {
        super("cps", "CPS", "Your clicks per second (display only)", Rule.ALLOWED, true, Anchor.TOP_LEFT, 4, 16);
        this.clicks = clicks;
    }

    private int l, r;

    @Override
    protected long key() {
        long now = System.currentTimeMillis();
        l = clicks.cps(0, now);
        r = clicks.cps(1, now);
        return ((long) l << 20) | ((long) r << 1) | (right.on() ? 1 : 0);
    }

    @Override
    protected String build() {
        return right.on() ? l + " | " + r + " CPS" : l + " CPS";
    }
}
