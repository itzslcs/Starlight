package dev.mw19.core.modules;

import dev.mw19.api.game.Game;
import dev.mw19.api.hud.Anchor;
import dev.mw19.api.module.Rule;
import dev.mw19.api.setting.BoolSetting;
import dev.mw19.core.Mw19;

/**
 * Toggle Sprint / Toggle Sneak with a status line. GRAY: Hypixel names "auto-sprint" as disallowed; a toggle is not
 * automatic, but the policy text does not make that distinction. Modern targets use vanilla's own toggle key mode.
 */
public final class ToggleModule extends TextHud {
    private final Game.Binding binding;
    private final String verb;
    private final BoolSetting status = add(new BoolSetting("status", "Show status", "Show the status line on the HUD", true));
    private int state = -1;

    public ToggleModule(String id, String name, Game.Binding binding, String verb, float y) {
        super(id, name, "Press once to keep " + verb.toLowerCase() + " (status line on the HUD)", Rule.GRAY, false, Anchor.BOTTOM_RIGHT, 4, y);
        this.binding = binding;
        this.verb = verb;
    }

    @Override
    public void onEnable() {
        Mw19.get().platform.setToggle(binding, true);
    }

    @Override
    public void onDisable() {
        Mw19.get().platform.setToggle(binding, false);
    }

    private int now() {
        Mw19 k = Mw19.get();
        if (!k.platform.inWorld()) return 0;
        if (k.platform.toggleLatched(binding)) return 2;
        boolean active = binding == Game.Binding.SPRINT ? k.platform.sprinting() : k.platform.sneaking();
        return active ? 1 : 0;
    }

    @Override
    protected boolean hasContent() {
        return status.on() && now() != 0;
    }

    @Override
    protected long key() {
        state = now();
        return state;
    }

    @Override
    protected String build() {
        return state == 2 ? "[" + verb + " (Toggled)]" : state == 1 ? "[" + verb + " (Key held)]" : "";
    }

    @Override
    protected String previewText() {
        return "[" + verb + " (Toggled)]";
    }
}
