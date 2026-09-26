package dev.mw19.core.modules;

import dev.mw19.api.Subscription;
import dev.mw19.api.event.KeyPressEvent;
import dev.mw19.api.module.Category;
import dev.mw19.api.module.Module;
import dev.mw19.api.module.Rule;
import dev.mw19.api.setting.BoolSetting;
import dev.mw19.api.setting.ChoiceSetting;
import dev.mw19.api.setting.KeySetting;
import dev.mw19.api.setting.NumberSetting;
import dev.mw19.core.Hooks;
import dev.mw19.core.Mw19;
import dev.mw19.core.event.ScrollEvent;
import dev.mw19.core.gui.Anim;
import dev.mw19.core.platform.ScreenHost;

import java.util.function.Consumer;

/** OptiFine-style zoom (OptiFine is on Hypixel's allowed list): lower FOV while the key is held. */
public final class ZoomModule extends Module implements Hooks.FovSource {
    private final KeySetting key = add(new KeySetting("key", "Zoom key", "Hold (or toggle) to zoom", 'C'));
    private final ChoiceSetting mode = add(new ChoiceSetting("mode", "Mode", "Hold the key or press to toggle", "Hold", "Hold", "Toggle"));
    private final NumberSetting factor = add(new NumberSetting("factor", "Zoom", "How far to zoom", 4, 1.5, 30, 0.5, "x"));
    private final BoolSetting smooth = add(new BoolSetting("smooth", "Smooth", "Animate in and out", true));
    private final BoolSetting scroll = add(new BoolSetting("scroll", "Scroll to adjust", "Mouse wheel changes the zoom while zooming", true));
    private final BoolSetting cinematic = add(new BoolSetting("cinematic", "Cinematic camera", "Smooth mouse while zooming (like OptiFine)", true));

    private final Anim anim = new Anim(1f);
    private boolean zooming, toggled, smoothWasSet;
    private float current;
    private Subscription keySub, scrollSub;

    public ZoomModule() {
        super("zoom", "Zoom", "Hold a key to zoom in", Category.UTILITY, Rule.ALLOWED, true);
    }

    @Override
    public void onEnable() {
        Hooks.zoom = this;
        final Mw19 k = Mw19.get();
        keySub = k.events.on(KeyPressEvent.class, new Consumer<KeyPressEvent>() {
            @Override
            public void accept(KeyPressEvent e) {
                if (mode.is("Toggle") && e.key == key.key()) toggled = !toggled;
            }
        });
        scrollSub = k.events.on(ScrollEvent.class, new Consumer<ScrollEvent>() {
            @Override
            public void accept(ScrollEvent e) {
                if (!zooming || !scroll.on()) return;
                current = Math.max(1.5f, Math.min(60f, current * (e.amount > 0 ? 1.25f : 0.8f)));
                e.consumed = true;
            }
        });
    }

    @Override
    public void onDisable() {
        if (Hooks.zoom == this) Hooks.zoom = null;
        if (keySub != null) keySub.cancel();
        if (scrollSub != null) scrollSub.cancel();
        setZooming(false);
        toggled = false;
        anim.snap(1f);
    }

    @Override
    public void onTick() {
        Mw19 k = Mw19.get();
        boolean inGame = k.platform.inWorld() && k.platform.screens().current() == ScreenHost.Kind.NONE;
        boolean want = inGame && (mode.is("Toggle") ? toggled : key.bound() && k.platform.isKeyDown(key.key()));
        if (!inGame && mode.is("Toggle")) toggled = false;
        setZooming(want);
    }

    private void setZooming(boolean on) {
        if (on == zooming) return;
        zooming = on;
        if (on) current = factor.floatValue();
        long now = System.currentTimeMillis();
        anim.to(on ? 1f / current : 1f, smooth.on() ? 180 : 0, now);
        if (cinematic.on() || smoothWasSet) {
            Mw19.get().platform.setSmoothCamera(on && cinematic.on());
            smoothWasSet = on && cinematic.on();
        }
    }

    @Override
    public float multiplier(long nowMs) {
        if (zooming && anim.target() != 1f / current) anim.to(1f / current, smooth.on() ? 120 : 0, nowMs);
        return anim.get(nowMs);
    }
}
