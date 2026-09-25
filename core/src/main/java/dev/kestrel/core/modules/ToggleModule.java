package dev.kestrel.core.modules;

import dev.kestrel.api.game.Game;
import dev.kestrel.api.hud.Anchor;
import dev.kestrel.api.module.Rule;
import dev.kestrel.api.setting.BoolSetting;
import dev.kestrel.core.Kestrel;

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
        Kestrel.get().platform.setToggle(binding, true);
    }

    @Override
    public void onDisable() {
        Kestrel.get().platform.setToggle(binding, false);
    }

    private int now() {
        Kestrel k = Kestrel.get();
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
