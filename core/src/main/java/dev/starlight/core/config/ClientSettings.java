package dev.starlight.core.config;

import dev.starlight.api.setting.BoolSetting;
import dev.starlight.api.setting.ChoiceSetting;
import dev.starlight.api.setting.ColorSetting;
import dev.starlight.api.setting.KeySetting;
import dev.starlight.api.setting.NumberSetting;
import dev.starlight.api.setting.Setting;
import dev.starlight.core.Keys;
import dev.starlight.core.gui.Theme;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Client-wide options (not per profile). Stored in config.json under "client". */
public final class ClientSettings {
    private final List<Setting<?>> all = new ArrayList<Setting<?>>();

    public final KeySetting openGui = add(new KeySetting("open_gui", "Open menu", "Key that opens the Starlight menu", Keys.RIGHT_SHIFT));
    public final ChoiceSetting theme = add(new ChoiceSetting("theme", "Theme", "Colours and the animated scene behind the menus", "Starlight", Theme.PRESETS));
    public final BoolSetting customAccent = add(new BoolSetting("custom_accent", "Custom accent", "Override the preset's accent colour", false));
    public final ColorSetting accent = add(new ColorSetting("accent", "Accent colour", "Used for highlights and toggles", 0xFFFFC857));
    public final BoolSetting blur = add(new BoolSetting("blur", "Background blur", "Blur the game behind menus (costs frames on weaker graphics; a dim is used when off)", false));
    public final NumberSetting uiScale = add(new NumberSetting("ui_scale", "Menu scale", "Size of the Starlight menu relative to the game's GUI scale", 1.0, 0.6, 1.6, 0.05, "x"));
    public final NumberSetting animSpeed = add(new NumberSetting("anim_speed", "Animation speed", "0 disables animations", 1.0, 0.0, 2.0, 0.1, "x"));
    public final BoolSetting customTitle = add(new BoolSetting("custom_title", "Custom home screen",
            "Replace Minecraft's title screen with the Starlight home screen", true));
    public final BoolSetting menuButtons = add(new BoolSetting("menu_buttons", "Title/pause buttons", "Starlight Menu and Packs on the pause screen, a Starlight button on the vanilla title screen", true));
    public final BoolSetting styleMenus = add(new BoolSetting("style_menus", "Starlight game menus", "Starlight's keycap buttons and ember backdrop for Minecraft's own menus (pause menu, server list, options) and the home screen", true));
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
