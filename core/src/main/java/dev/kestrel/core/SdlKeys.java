package dev.kestrel.core;

import java.util.Arrays;

/**
 * Minecraft 26.3 moved from GLFW to SDL3: key events carry SDL scancodes (USB HID usage ids), modifiers are SDL_Keymod
 * bits and mouse buttons are 1-based with right = 3. Kestrel keeps GLFW codes as its canonical key space (profiles
 * stay portable between versions), so the 26.3 adapter translates at the boundary with these tables.
 */
public final class SdlKeys {
    private static final int[] TO_GLFW = new int[256];
    private static final int[] TO_SDL = new int[Keys.MENU + 1];

    static {
        Arrays.fill(TO_GLFW, Keys.NONE);
        Arrays.fill(TO_SDL, -1);
        for (int i = 0; i < 26; i++) map(4 + i, Keys.A + i);             // a..z
        for (int i = 0; i < 9; i++) map(30 + i, Keys.N0 + 1 + i);        // 1..9
        map(39, Keys.N0);
        map(40, Keys.ENTER);
        map(41, Keys.ESCAPE);
        map(42, Keys.BACKSPACE);
        map(43, Keys.TAB);
        map(44, Keys.SPACE);
        map(45, Keys.MINUS);
        map(46, Keys.EQUAL);
        map(47, Keys.LEFT_BRACKET);
        map(48, Keys.RIGHT_BRACKET);
        map(49, Keys.BACKSLASH);
        TO_GLFW[50] = Keys.BACKSLASH;                                     // ISO "non-US #": same key position
        map(51, Keys.SEMICOLON);
        map(52, Keys.APOSTROPHE);
        map(53, Keys.GRAVE);
        map(54, Keys.COMMA);
        map(55, Keys.PERIOD);
        map(56, Keys.SLASH);
        map(57, Keys.CAPS_LOCK);
        for (int i = 0; i < 12; i++) map(58 + i, Keys.F1 + i);           // F1..F12
        map(70, Keys.PRINT);
        map(71, Keys.SCROLL_LOCK);
        map(72, Keys.PAUSE);
        map(73, Keys.INSERT);
        map(74, Keys.HOME);
        map(75, Keys.PAGE_UP);
        map(76, Keys.DELETE);
        map(77, Keys.END);
        map(78, Keys.PAGE_DOWN);
        map(79, Keys.RIGHT);
        map(80, Keys.LEFT);
        map(81, Keys.DOWN);
        map(82, Keys.UP);
        map(83, Keys.NUM_LOCK);
        map(84, Keys.KP_DIVIDE);
        map(85, Keys.KP_MULTIPLY);
        map(86, Keys.KP_SUBTRACT);
        map(87, Keys.KP_ADD);
        map(88, Keys.KP_ENTER);
        for (int i = 0; i < 9; i++) map(89 + i, Keys.KP_0 + 1 + i);      // keypad 1..9
        map(98, Keys.KP_0);
        map(99, Keys.KP_DECIMAL);
        TO_GLFW[220] = Keys.KP_DECIMAL;                                   // SDL's separate "keypad decimal"
        map(101, Keys.MENU);                                              // application / context-menu key
        TO_GLFW[118] = Keys.MENU;
        map(103, Keys.KP_EQUAL);
        for (int i = 0; i < 12; i++) map(104 + i, Keys.F1 + 12 + i);     // F13..F24
        map(224, Keys.LEFT_CONTROL);
        map(225, Keys.LEFT_SHIFT);
        map(226, Keys.LEFT_ALT);
        map(227, Keys.LEFT_SUPER);
        map(228, Keys.RIGHT_CONTROL);
        map(229, Keys.RIGHT_SHIFT);
        map(230, Keys.RIGHT_ALT);
        map(231, Keys.RIGHT_SUPER);
    }

    private SdlKeys() {}

    private static void map(int sdl, int glfw) {
        TO_GLFW[sdl] = glfw;
        TO_SDL[glfw] = sdl;
    }

    /** SDL scancode -> canonical (GLFW) key, or {@link Keys#NONE}. */
    public static int toCanonical(int scancode) {
        return scancode >= 0 && scancode < TO_GLFW.length ? TO_GLFW[scancode] : Keys.NONE;
    }

    /** Canonical (GLFW) key -> SDL scancode, or -1. */
    public static int toSdl(int key) {
        return key >= 0 && key < TO_SDL.length ? TO_SDL[key] : -1;
    }

    /** SDL_Keymod bits (left/right pairs) -> GLFW modifier bits. */
    public static int mods(int sdl) {
        int m = 0;
        if ((sdl & 0x0003) != 0) m |= Keys.MOD_SHIFT;
        if ((sdl & 0x00C0) != 0) m |= Keys.MOD_CONTROL;
        if ((sdl & 0x0300) != 0) m |= Keys.MOD_ALT;
        if ((sdl & 0x0C00) != 0) m |= Keys.MOD_SUPER;
        return m;
    }

    /** SDL mouse button (1 left, 2 middle, 3 right, 4.. extra) -> GLFW button (0 left, 1 right, 2 middle, 3.. extra). */
    public static int button(int sdl) {
        return sdl == 2 ? 2 : sdl == 3 ? 1 : sdl - 1;
    }

    /** GLFW button -> SDL button. */
    public static int sdlButton(int glfw) {
        return glfw == 1 ? 3 : glfw == 2 ? 2 : glfw + 1;
    }
}
