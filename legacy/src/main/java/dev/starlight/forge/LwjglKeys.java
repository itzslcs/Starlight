package dev.starlight.forge;

import dev.starlight.core.Keys;

/** LWJGL2 Keyboard codes <-> GLFW codes (core's canonical codes). Table from lwjgl-2.9.4 constants. */
final class LwjglKeys {
    private static final int[] TO_GLFW = new int[256];
    private static final int[] TO_LWJGL = new int[400];

    private static void map(int lwjgl, int glfw) {
        TO_GLFW[lwjgl] = glfw;
        if (glfw >= 0 && glfw < TO_LWJGL.length) TO_LWJGL[glfw] = lwjgl;
    }

    static {
        java.util.Arrays.fill(TO_GLFW, Keys.NONE);
        map(1, Keys.ESCAPE);
        int[] digits = {11, 2, 3, 4, 5, 6, 7, 8, 9, 10}; // KEY_0..KEY_9
        for (int d = 0; d <= 9; d++) map(digits[d], '0' + d);
        String letters = "QWERTYUIOPASDFGHJKLZXCVBNM";
        int[] letterCodes = {16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 30, 31, 32, 33, 34, 35, 36, 37, 38, 44, 45, 46, 47, 48, 49, 50};
        for (int i = 0; i < letters.length(); i++) map(letterCodes[i], letters.charAt(i));
        map(12, Keys.MINUS);
        map(13, Keys.EQUAL);
        map(14, Keys.BACKSPACE);
        map(15, Keys.TAB);
        map(26, Keys.LEFT_BRACKET);
        map(27, Keys.RIGHT_BRACKET);
        map(28, Keys.ENTER);
        map(29, Keys.LEFT_CONTROL);
        map(39, Keys.SEMICOLON);
        map(40, Keys.APOSTROPHE);
        map(41, Keys.GRAVE);
        map(42, Keys.LEFT_SHIFT);
        map(43, Keys.BACKSLASH);
        map(51, Keys.COMMA);
        map(52, Keys.PERIOD);
        map(53, Keys.SLASH);
        map(54, Keys.RIGHT_SHIFT);
        map(55, Keys.KP_MULTIPLY);
        map(56, Keys.LEFT_ALT);
        map(57, Keys.SPACE);
        map(58, Keys.CAPS_LOCK);
        for (int f = 0; f < 10; f++) map(59 + f, Keys.F1 + f); // F1..F10
        map(87, Keys.F1 + 10);
        map(88, Keys.F1 + 11);
        for (int f = 0; f < 6; f++) map(100 + f, Keys.F1 + 12 + f); // F13..F18
        map(113, Keys.F1 + 18);
        map(69, Keys.NUM_LOCK);
        map(70, Keys.SCROLL_LOCK);
        int[] pad = {82, 79, 80, 81, 75, 76, 77, 71, 72, 73}; // NUMPAD0..9
        for (int d = 0; d <= 9; d++) map(pad[d], Keys.KP_0 + d);
        map(74, Keys.KP_SUBTRACT);
        map(78, Keys.KP_ADD);
        map(83, Keys.KP_DECIMAL);
        map(141, Keys.KP_EQUAL);
        map(156, Keys.KP_ENTER);
        map(157, Keys.RIGHT_CONTROL);
        map(181, Keys.KP_DIVIDE);
        map(183, Keys.PRINT);
        map(184, Keys.RIGHT_ALT);
        map(197, Keys.PAUSE);
        map(199, Keys.HOME);
        map(200, Keys.UP);
        map(201, Keys.PAGE_UP);
        map(203, Keys.LEFT);
        map(205, Keys.RIGHT);
        map(207, Keys.END);
        map(208, Keys.DOWN);
        map(209, Keys.PAGE_DOWN);
        map(210, Keys.INSERT);
        map(211, Keys.DELETE);
        map(219, Keys.LEFT_SUPER);
        map(220, Keys.RIGHT_SUPER);
        map(221, Keys.MENU);
    }

    private LwjglKeys() {}

    static int toGlfw(int lwjgl) {
        return lwjgl > 0 && lwjgl < TO_GLFW.length ? TO_GLFW[lwjgl] : Keys.NONE;
    }

    static int toLwjgl(int glfw) {
        return glfw > 0 && glfw < TO_LWJGL.length ? TO_LWJGL[glfw] : 0;
    }

    /** Vanilla 1.8.9 KeyBinding code (mouse = button - 100) -> canonical code. */
    static int fromBinding(int code) {
        return code < 0 ? Keys.mouse(code + 100) : toGlfw(code);
    }
}
