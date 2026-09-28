package dev.starlight.core.modules;

import dev.starlight.api.Subscription;
import dev.starlight.api.hud.Anchor;
import dev.starlight.api.module.Rule;
import dev.starlight.api.setting.NumberSetting;
import dev.starlight.core.Starlight;
import dev.starlight.core.event.AttackEvent;

import java.util.function.Consumer;

/**
 * Consecutive hits you land without taking damage. GRAY in RULES_MATRIX: the number is derived from your own hits but
 * vanilla never shows it.
 */
public final class ComboModule extends TextHud {
    private final NumberSetting timeout = add(new NumberSetting("timeout", "Reset after", "Seconds without a hit before the combo resets", 2, 1, 6, 0.5, "s"));
    private int combo;
    private long lastHit;
    private Subscription sub;

    public ComboModule() {
        super("combo", "Combo Counter", "Hits in a row without being hit", Rule.GRAY, false, Anchor.CENTER, 0, 24);
    }

    @Override
    public void onEnable() {
        combo = 0;
        sub = Starlight.get().events.on(AttackEvent.class, new Consumer<AttackEvent>() {
            @Override
            public void accept(AttackEvent e) {
                combo++;
                lastHit = System.currentTimeMillis();
            }
        });
    }

    @Override
    public void onDisable() {
        if (sub != null) sub.cancel();
        sub = null;
    }

    @Override
    public void onTick() {
        if (combo == 0) return;
        if (Starlight.get().platform.hurtTime() > 0 || System.currentTimeMillis() - lastHit > timeout.get() * 1000) combo = 0;
    }

    @Override
    protected boolean hasContent() {
        return combo > 1;
    }

    @Override
    protected long key() {
        return combo;
    }

    @Override
    protected String build() {
        return combo + " Combo";
    }

    @Override
    protected String previewText() {
        return "3 Combo";
    }
}
