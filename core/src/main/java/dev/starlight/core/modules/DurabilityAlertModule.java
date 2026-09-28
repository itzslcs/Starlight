package dev.starlight.core.modules;

import dev.starlight.api.module.Category;
import dev.starlight.api.module.Module;
import dev.starlight.api.module.Rule;
import dev.starlight.api.render.ItemRef;
import dev.starlight.api.setting.BoolSetting;
import dev.starlight.api.setting.NumberSetting;
import dev.starlight.core.Starlight;
import dev.starlight.core.platform.Platform;

/** A notification (and a ping) once when a worn armor piece or the held item drops below the threshold. */
public final class DurabilityAlertModule extends Module {
    private static final String[] SLOTS = {"Boots", "Leggings", "Chestplate", "Helmet"};
    private final NumberSetting threshold = add(new NumberSetting("threshold", "Warn below", "Remaining durability", 10, 1, 50, 1, "%"));
    private final BoolSetting sound = add(new BoolSetting("sound", "Sound", "Play a ping with the warning", true));
    /** Last warned item per slot (0-3 armor, 4 main hand): id and damage, so each item warns once until it changes. */
    private final String[] warnedId = new String[5];
    private int ticks;

    public DurabilityAlertModule() {
        super("durability_alert", "Durability Alert", "Warns when your armor or held tool is about to break", Category.UTILITY, Rule.ALLOWED, false);
    }

    @Override
    public void onTick() {
        if (++ticks % 20 != 0) return;
        Platform p = Starlight.get().platform;
        if (!p.inWorld()) return;
        for (int slot = 0; slot < 5; slot++) {
            ItemRef it = slot < 4 ? p.armor(slot) : p.mainHand();
            check(slot, it, slot < 4 ? SLOTS[slot] : null);
        }
    }

    private void check(int slot, ItemRef it, String label) {
        if (it.isEmpty() || it.maxDamage() <= 0) {
            warnedId[slot] = null;
            return;
        }
        int left = it.maxDamage() - it.damage();
        boolean low = left * 100 < threshold.intValue() * it.maxDamage();
        if (!low) {
            if (it.id().equals(warnedId[slot])) warnedId[slot] = null; // repaired
            return;
        }
        if (it.id().equals(warnedId[slot])) return;
        warnedId[slot] = it.id();
        Starlight k = Starlight.get();
        k.toast("Durability", (label != null ? label : it.displayName()) + " is about to break (" + left + "/" + it.maxDamage() + ")", k.theme.warn);
        if (sound.on()) k.platform.playPing();
    }
}
