package dev.starlight.core.modules;

import dev.starlight.api.Subscription;
import dev.starlight.api.event.KeyPressEvent;
import dev.starlight.api.module.Category;
import dev.starlight.api.module.Module;
import dev.starlight.api.module.Rule;
import dev.starlight.api.setting.BoolSetting;
import dev.starlight.api.setting.ChoiceSetting;
import dev.starlight.api.setting.KeySetting;
import dev.starlight.core.Hooks;
import dev.starlight.core.Starlight;
import dev.starlight.core.Keys;
import dev.starlight.core.platform.ScreenHost;

import java.util.function.Consumer;

/**
 * Look around without turning your player. Changes perspective, so it is DISALLOWED on Hypixel
 * (serverrules.json blocks it there) and off by default everywhere.
 */
public final class FreelookModule extends Module {
    private final KeySetting key = add(new KeySetting("key", "Freelook key", "Hold (or toggle) to look around", Keys.LEFT_ALT));
    private final ChoiceSetting mode = add(new ChoiceSetting("mode", "Mode", "Hold the key or press to toggle", "Hold", "Hold", "Toggle"));
    private final BoolSetting thirdPerson = add(new BoolSetting("third_person", "Switch to third person", "Look at yourself while free-looking", true));
    private final BoolSetting invert = add(new BoolSetting("invert", "Invert pitch", "Invert up/down while free-looking", false));

    private boolean active, toggled;
    private int savedPerspective;
    private Subscription sub;

    public FreelookModule() {
        super("freelook", "Freelook", "Look around without turning (blocked on servers that forbid it)", Category.UTILITY,
                Rule.DISALLOWED_ON_SOME_SERVERS, false);
    }

    @Override
    public void onEnable() {
        sub = Starlight.get().events.on(KeyPressEvent.class, new Consumer<KeyPressEvent>() {
            @Override
            public void accept(KeyPressEvent e) {
                if (mode.is("Toggle") && e.key == key.key()) toggled = !toggled;
            }
        });
    }

    @Override
    public void onDisable() {
        if (sub != null) sub.cancel();
        toggled = false;
        set(false);
    }

    @Override
    public void onTick() {
        Starlight k = Starlight.get();
        boolean inGame = k.platform.inWorld() && k.platform.screens().current() == ScreenHost.Kind.NONE;
        boolean want = inGame && (mode.is("Toggle") ? toggled : key.bound() && k.platform.isKeyDown(key.key()));
        set(want);
    }

    private void set(boolean on) {
        if (on == active) return;
        active = on;
        Starlight k = Starlight.get();
        if (on) {
            Hooks.freelookYaw = k.platform.yaw();
            Hooks.freelookPitch = k.platform.pitch();
            Hooks.freelookInvert = invert.on();
            savedPerspective = k.platform.perspective();
            if (thirdPerson.on() && savedPerspective == 0) k.platform.setPerspective(1);
            Hooks.freelook = true;
        } else {
            Hooks.freelook = false;
            if (thirdPerson.on() && k.platform.inWorld()) k.platform.setPerspective(savedPerspective);
        }
    }
}
