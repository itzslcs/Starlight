package dev.kestrel.core.config;

import dev.kestrel.api.setting.BoolSetting;
import dev.kestrel.api.setting.ChoiceSetting;
import dev.kestrel.api.setting.ColorSetting;
import dev.kestrel.api.setting.KeySetting;
import dev.kestrel.api.setting.NumberSetting;
import dev.kestrel.api.setting.Setting;
import dev.kestrel.core.Keys;
import dev.kestrel.core.gui.Theme;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Client-wide options (not per profile). Stored in config.json under "client". */
public final class ClientSettings {
    private final List<Setting<?>> all = new ArrayList<Setting<?>>();

    public final KeySetting openGui = add(new KeySetting("open_gui", "Open menu", "Key that opens the Kestrel menu", Keys.RIGHT_SHIFT));
    public final ChoiceSetting theme = add(new ChoiceSetting("theme", "Theme", "Colour preset", "Kestrel", Theme.PRESETS));
    public final BoolSetting customAccent = add(new BoolSetting("custom_accent", "Custom accent", "Override the preset's accent colour", false));
    public final ColorSetting accent = add(new ColorSetting("accent", "Accent colour", "Used for highlights and toggles", 0xFFFF8A3D));
    public final BoolSetting blur = add(new BoolSetting("blur", "Background blur", "Blur the game behind menus where the game supports it (dim fallback)", true));
    public final NumberSetting uiScale = add(new NumberSetting("ui_scale", "Menu scale", "Size of the Kestrel menu relative to the game's GUI scale", 1.0, 0.6, 1.6, 0.05, "x"));
    public final NumberSetting animSpeed = add(new NumberSetting("anim_speed", "Animation speed", "0 disables animations", 1.0, 0.0, 2.0, 0.1, "x"));
    public final BoolSetting menuButtons = add(new BoolSetting("menu_buttons", "Title/pause button", "Show a Kestrel button on the title and pause screens", true));
    public final BoolSetting competitiveSafe = add(new BoolSetting("competitive_safe", "Competitive-safe", "Disable every GRAY and server-restricted module everywhere", false));
    public final BoolSetting toasts = add(new BoolSetting("toasts", "Notifications", "Show toasts (module errors, server rules, profiles)", true));
    public final BoolSetting hudGrid = add(new BoolSetting("hud_grid", "Editor grid", "Show and snap to a grid in the HUD editor", false));
    public final BoolSetting hudSnap = add(new BoolSetting("hud_snap", "Editor snapping", "Snap to screen and element edges in the HUD editor", true));
    public final NumberSetting gridSize = add(new NumberSetting("grid_size", "Grid size", "HUD editor grid cell", 8, 2, 32, 1));

    private <S extends Setting<?>> S add(S s) {
        all.add(s);
        return s;
    }

    public List<Setting<?>> all() {
        return Collections.unmodifiableList(all);
    }

    public void onAnyChange(Runnable r) {
        for (Setting<?> s : all) s.addListener(r);
    }

    public Map<String, Object> toJson() {
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        for (Setting<?> s : all) m.put(s.id(), s.toJson());
        return m;
    }

    public void fromJson(Map<String, Object> m) {
        for (Setting<?> s : all) {
            if (m.containsKey(s.id())) s.fromJson(m.get(s.id()));
        }
    }
}
