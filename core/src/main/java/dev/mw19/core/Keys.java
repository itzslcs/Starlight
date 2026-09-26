package dev.mw19.core;

import dev.mw19.api.setting.KeySetting;

/** GLFW key constants (canonical codes on every target) and display names. */
public final class Keys {
    public static final int NONE = KeySetting.NONE;
    public static final int SPACE = 32, APOSTROPHE = 39, COMMA = 44, MINUS = 45, PERIOD = 46, SLASH = 47;
    public static final int N0 = 48, N9 = 57, SEMICOLON = 59, EQUAL = 61, A = 65, Z = 90;
    public static final int LEFT_BRACKET = 91, BACKSLASH = 92, RIGHT_BRACKET = 93, GRAVE = 96;
    public static final int ESCAPE = 256, ENTER = 257, TAB = 258, BACKSPACE = 259, INSERT = 260, DELETE = 261;
    public static final int RIGHT = 262, LEFT = 263, DOWN = 264, UP = 265, PAGE_UP = 266, PAGE_DOWN = 267;
    public static final int HOME = 268, END = 269, CAPS_LOCK = 280, SCROLL_LOCK = 281, NUM_LOCK = 282, PRINT = 283, PAUSE = 284;
    public static final int F1 = 290, F25 = 314, KP_0 = 320, KP_9 = 329, KP_DECIMAL = 330, KP_DIVIDE = 331;
    public static final int KP_MULTIPLY = 332, KP_SUBTRACT = 333, KP_ADD = 334, KP_ENTER = 335, KP_EQUAL = 336;
    public static final int LEFT_SHIFT = 340, LEFT_CONTROL = 341, LEFT_ALT = 342, LEFT_SUPER = 343;
    public static final int RIGHT_SHIFT = 344, RIGHT_CONTROL = 345, RIGHT_ALT = 346, RIGHT_SUPER = 347, MENU = 348;

    public static final int MOD_SHIFT = 1, MOD_CONTROL = 2, MOD_ALT = 4, MOD_SUPER = 8;
    public static final int ACTION_RELEASE = 0, ACTION_PRESS = 1, ACTION_REPEAT = 2;

    public static final int MOUSE_BASE = KeySetting.MOUSE_BASE;

    private Keys() {}

    public static int mouse(int button) {
        return MOUSE_BASE + button;
    }

    public static boolean isMouse(int code) {
        return code >= MOUSE_BASE && code < MOUSE_BASE + 16;
    }

    public static String name(int code) {
        if (code == NONE) return "None";
        if (isMouse(code)) {
            int b = code - MOUSE_BASE;
            return b == 0 ? "LMB" : b == 1 ? "RMB" : b == 2 ? "MMB" : "Mouse " + (b + 1);
        }
        if (code >= A && code <= Z) return String.valueOf((char) code);
        if (code >= N0 && code <= N9) return String.valueOf((char) code);
        if (code >= F1 && code <= F25) return "F" + (code - F1 + 1);
        if (code >= KP_0 && code <= KP_9) return "Num " + (code - KP_0);
        switch (code) {
            case SPACE: return "Space";
            case APOSTROPHE: return "'";
            case COMMA: return ",";
            case MINUS: return "-";
            case PERIOD: return ".";
            case SLASH: return "/";
            case SEMICOLON: return ";";
            case EQUAL: return "=";
            case LEFT_BRACKET: return "[";
            case BACKSLASH: return "\\";
            case RIGHT_BRACKET: return "]";
            case GRAVE: return "`";
            case ESCAPE: return "Esc";
            case ENTER: return "Enter";
            case TAB: return "Tab";
            case BACKSPACE: return "Backspace";
            case INSERT: return "Insert";
            case DELETE: return "Delete";
            case RIGHT: return "Right";
            case LEFT: return "Left";
            case DOWN: return "Down";
            case UP: return "Up";
            case PAGE_UP: return "PgUp";
            case PAGE_DOWN: return "PgDn";
            case HOME: return "Home";
            case END: return "End";
            case CAPS_LOCK: return "Caps";
            case LEFT_SHIFT: return "LShift";
            case RIGHT_SHIFT: return "RShift";
            case LEFT_CONTROL: return "LCtrl";
            case RIGHT_CONTROL: return "RCtrl";
            case LEFT_ALT: return "LAlt";
            case RIGHT_ALT: return "RAlt";
            case KP_ENTER: return "Num Enter";
            case KP_ADD: return "Num +";
            case KP_SUBTRACT: return "Num -";
            case KP_MULTIPLY: return "Num *";
            case KP_DIVIDE: return "Num /";
            case KP_DECIMAL: return "Num .";
            default: return "Key " + code;
        }
    }
}
