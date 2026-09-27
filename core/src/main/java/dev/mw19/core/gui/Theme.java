package dev.mw19.core.gui;

import dev.mw19.api.util.Colors;

/**
 * A theme: colour tokens for the menu, and a scene (the animated backdrop, {@link Scenes}) with its sky and the keycap
 * colours of Minecraft's own menus. Presets are original palettes; the accent can be overridden by the user.
 */
public final class Theme {
    /** The animated backdrops. */
    public enum Scene { STARS, EMBERS, AURORA, NEBULA, SNOW, PETALS, CLOUDS }

    public final String name;
    public final int dim, panel, sidebar, surface, surface2, border, text, textDim, onAccent, good, warn, bad;
    public int accent;
    public Scene scene = Scene.STARS;
    /** The scene's sky, top and bottom. */
    public int skyTop = 0xFF070B1A, skyBottom = 0xFF1A2448;
    /** Keycaps of Minecraft's menus: dark in every theme, because vanilla draws their labels white. */
    public int keyTop = 0xFF3D404B, keyBottom = 0xFF2B2E36, keySide = 0xFF18191F;

    private Theme(String name, int dim, int panel, int sidebar, int surface, int surface2, int border, int text,
                  int textDim, int accent, int onAccent, int good, int warn, int bad) {
        this.name = name;
        this.dim = dim;
        this.panel = panel;
        this.sidebar = sidebar;
        this.surface = surface;
        this.surface2 = surface2;
        this.border = border;
        this.text = text;
        this.textDim = textDim;
        this.accent = accent;
        this.onAccent = onAccent;
        this.good = good;
        this.warn = warn;
        this.bad = bad;
    }

    private Theme look(Scene scene, int skyTop, int skyBottom, int keyTop, int keyBottom, int keySide) {
        this.scene = scene;
        this.skyTop = skyTop;
        this.skyBottom = skyBottom;
        this.keyTop = keyTop;
        this.keyBottom = keyBottom;
        this.keySide = keySide;
        return this;
    }

    private static Theme dark(String name, int accent) {
        return new Theme(name, 0x70000000, 0xF0111318, 0xF00C0E12, 0xFF1A1D24, 0xFF232833, 0x22FFFFFF,
                0xFFECEFF4, 0xFF8C95A5, accent, 0xFF141007, 0xFF3DD68C, 0xFFFFC53D, 0xFFFF5C5C);
    }

    /** Picker order. Starlight is the default. */
    public static final String[] PRESETS = {"Starlight", "Ember", "Aurora", "Nebula", "Glacier", "Sakura", "Crystal", "Daylight", "Black", "White"};

    /** Names used before 0.8.0 (config migration 1 -> 2, and anything still holding them). */
    public static String current(String name) {
        if ("MW19".equals(name)) return "Starlight"; // the brand theme follows the brand
        if ("Violet".equals(name)) return "Nebula";
        if ("Forest".equals(name)) return "Aurora";
        if ("Rose".equals(name)) return "Sakura";
        return name;
    }

    public static Theme preset(String name) {
        name = current(name);
        if ("Ember".equals(name)) {
            return dark(name, 0xFFFF8A3D).look(Scene.EMBERS, 0xFF15151B, 0xFF09090C, 0xFF3D404B, 0xFF2B2E36, 0xFF18191F);
        }
        if ("Aurora".equals(name)) {
            return dark(name, 0xFF45D08A).look(Scene.AURORA, 0xFF06121A, 0xFF0E2A2E, 0xFF2C4843, 0xFF203733, 0xFF0F1E1B);
        }
        if ("Nebula".equals(name)) {
            return dark(name, 0xFF9B7BFF).look(Scene.NEBULA, 0xFF0C0718, 0xFF1C1236, 0xFF3A3058, 0xFF2B2345, 0xFF161026);
        }
        if ("Glacier".equals(name)) {
            return dark(name, 0xFF4CC9F0).look(Scene.SNOW, 0xFF0B1626, 0xFF2A4868, 0xFF2E4556, 0xFF223543, 0xFF101C25);
        }
        if ("Sakura".equals(name)) {
            return dark(name, 0xFFFF6B9A).look(Scene.PETALS, 0xFF1E0F2A, 0xFF6B3452, 0xFF4A2F45, 0xFF3A2437, 0xFF1E121C);
        }
        if ("Black".equals(name)) { // OLED black, white accent
            return new Theme(name, 0x90000000, 0xF5000000, 0xF5050505, 0xFF0E0E0E, 0xFF1A1A1A, 0x26FFFFFF,
                    0xFFFFFFFF, 0xFF8A8A8A, 0xFFFFFFFF, 0xFF000000, 0xFF3DD68C, 0xFFFFC53D, 0xFFFF5C5C)
                    .look(Scene.STARS, 0xFF000000, 0xFF050507, 0xFF2A2A2C, 0xFF1C1C1E, 0xFF0B0B0C);
        }
        if ("White".equals(name)) { // paper white, black accent
            return new Theme(name, 0x50FFFFFF, 0xF7FFFFFF, 0xF7F2F2F2, 0xFFFFFFFF, 0xFFEDEDED, 0x1F000000,
                    0xFF111111, 0xFF6B6B6B, 0xFF111111, 0xFFFFFFFF, 0xFF1F9D5C, 0xFFB7860B, 0xFFD63B3B)
                    .look(Scene.CLOUDS, 0xFF7E9CBC, 0xFFD2DEEA, 0xFF3C4552, 0xFF2D3440, 0xFF161B22);
        }
        if ("Crystal".equals(name)) { // frosted ice: blue-tinted glass panels
            return new Theme(name, 0x600A1622, 0xE60E1A27, 0xE60A131E, 0xFF132236, 0xFF1B2F47, 0x4099D6FF,
                    0xFFEAF6FF, 0xFF8FB3CC, 0xFF9BE3FF, 0xFF06121E, 0xFF5BE3B0, 0xFFFFD66B, 0xFFFF7A8A)
                    .look(Scene.SNOW, 0xFF061424, 0xFF16406A, 0xFF2E4A66, 0xFF223A52, 0xFF10202E);
        }
        if ("Daylight".equals(name)) {
            return new Theme(name, 0x50FFFFFF, 0xF2F4F5F8, 0xF2E6E9EF, 0xFFFFFFFF, 0xFFE9EDF3, 0x22000000,
                    0xFF1B1F27, 0xFF5E6675, 0xFFE8702A, 0xFFFFFFFF, 0xFF1F9D5C, 0xFFB7860B, 0xFFD63B3B)
                    .look(Scene.CLOUDS, 0xFF3F86D8, 0xFFA9D2F5, 0xFF3C4552, 0xFF2D3440, 0xFF161B22);
        }
        // Starlight: a night-blue menu with a star-gold accent
        return new Theme("Starlight", 0x70000010, 0xF00A0F1E, 0xF0070B17, 0xFF131A2E, 0xFF1C2540, 0x2680A0FF,
                0xFFEAF0FF, 0xFF8C99BC, 0xFFFFC857, 0xFF1A1405, 0xFF3DD68C, 0xFFFFC53D, 0xFFFF5C5C)
                .look(Scene.STARS, 0xFF050818, 0xFF1B2450, 0xFF2F3A5C, 0xFF232C48, 0xFF11172A);
    }

    /** Accent at reduced opacity, for fills behind accent-coloured content. */
    public int accentSoft(float a) {
        return Colors.fade(accent, a);
    }
}
