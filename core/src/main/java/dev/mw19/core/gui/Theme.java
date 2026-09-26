package dev.mw19.core.gui;

import dev.mw19.api.util.Colors;

/** Colour tokens. Presets are original palettes; the accent can be overridden by the user. */
public final class Theme {
    public final String name;
    public final int dim, panel, sidebar, surface, surface2, border, text, textDim, onAccent, good, warn, bad;
    public int accent;

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

    private static Theme dark(String name, int accent) {
        return new Theme(name, 0x70000000, 0xF0111318, 0xF00C0E12, 0xFF1A1D24, 0xFF232833, 0x22FFFFFF,
                0xFFECEFF4, 0xFF8C95A5, accent, 0xFF141007, 0xFF3DD68C, 0xFFFFC53D, 0xFFFF5C5C);
    }

    public static final String[] PRESETS = {"MW19", "Glacier", "Violet", "Forest", "Rose", "Daylight"};

    public static Theme preset(String name) {
        if ("Glacier".equals(name)) return dark(name, 0xFF4CC9F0);
        if ("Violet".equals(name)) return dark(name, 0xFF9B7BFF);
        if ("Forest".equals(name)) return dark(name, 0xFF45D08A);
        if ("Rose".equals(name)) return dark(name, 0xFFFF6B9A);
        if ("Daylight".equals(name)) {
            return new Theme(name, 0x50FFFFFF, 0xF2F4F5F8, 0xF2E6E9EF, 0xFFFFFFFF, 0xFFE9EDF3, 0x22000000,
                    0xFF1B1F27, 0xFF5E6675, 0xFFE8702A, 0xFFFFFFFF, 0xFF1F9D5C, 0xFFB7860B, 0xFFD63B3B);
        }
        return dark("MW19", 0xFFFF8A3D);
    }

    /** Accent at reduced opacity, for fills behind accent-coloured content. */
    public int accentSoft(float a) {
        return Colors.fade(accent, a);
    }
}
