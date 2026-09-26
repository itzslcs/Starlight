package dev.mw19.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SdlKeysTest {
    /**
     * {GLFW, SDL} for every KEY_* constant that Minecraft's InputConstants defines in both 26.2 (GLFW values) and
     * 26.3 (SDL scancodes), paired by constant name (javap -constants of both client jars).
     */
    private static final int[][] MINECRAFT_PAIRS = {
            {32, 44}, {39, 52}, {44, 54}, {45, 45}, {46, 55}, {47, 56}, {48, 39}, {49, 30}, {50, 31}, {51, 32},
            {52, 33}, {53, 34}, {54, 35}, {55, 36}, {56, 37}, {57, 38}, {59, 51}, {61, 46}, {65, 4}, {66, 5}, {67, 6},
            {68, 7}, {69, 8}, {70, 9}, {71, 10}, {72, 11}, {73, 12}, {74, 13}, {75, 14}, {76, 15}, {77, 16}, {78, 17},
            {79, 18}, {80, 19}, {81, 20}, {82, 21}, {83, 22}, {84, 23}, {85, 24}, {86, 25}, {87, 26}, {88, 27},
            {89, 28}, {90, 29}, {91, 47}, {92, 49}, {93, 48}, {96, 53}, {256, 41}, {257, 40}, {258, 43}, {259, 42},
            {260, 73}, {261, 76}, {262, 79}, {263, 80}, {264, 81}, {265, 82}, {266, 75}, {267, 78}, {268, 74},
            {269, 77}, {280, 57}, {281, 71}, {282, 83}, {283, 70}, {284, 72}, {290, 58}, {291, 59}, {292, 60},
            {293, 61}, {294, 62}, {295, 63}, {296, 64}, {297, 65}, {298, 66}, {299, 67}, {300, 68}, {301, 69},
            {302, 104}, {303, 105}, {304, 106}, {305, 107}, {306, 108}, {307, 109}, {308, 110}, {309, 111}, {310, 112},
            {311, 113}, {312, 114}, {313, 115}, {320, 98}, {321, 89}, {322, 90}, {323, 91}, {324, 92}, {325, 93},
            {326, 94}, {327, 95}, {328, 96}, {329, 97}, {330, 220}, {332, 85}, {334, 87}, {335, 88}, {336, 103},
            {340, 225}, {341, 224}, {342, 226}, {344, 229}, {345, 228}, {346, 230}
    };

    @Test
    void agreesWithMinecraftsOwnKeyConstants() {
        for (int[] p : MINECRAFT_PAIRS) {
            assertEquals(p[0], SdlKeys.toCanonical(p[1]), "SDL " + p[1]);
            // Minecraft names SDL's rare "keypad decimal" (220); the numpad '.' key itself reports 99.
            if (p[0] != Keys.KP_DECIMAL) assertEquals(p[1], SdlKeys.toSdl(p[0]), "GLFW " + p[0]);
        }
        assertEquals(99, SdlKeys.toSdl(Keys.KP_DECIMAL));
    }

    @Test
    void unknownCodesMapToNothing() {
        assertEquals(Keys.NONE, SdlKeys.toCanonical(0));
        assertEquals(Keys.NONE, SdlKeys.toCanonical(9999));
        assertEquals(-1, SdlKeys.toSdl(Keys.NONE));
        assertEquals(-1, SdlKeys.toSdl(Keys.F25));
    }

    @Test
    void modifiersAndButtons() {
        assertEquals(Keys.MOD_SHIFT | Keys.MOD_CONTROL, SdlKeys.mods(0x0002 | 0x0040)); // RSHIFT | LCTRL
        assertEquals(Keys.MOD_ALT | Keys.MOD_SUPER, SdlKeys.mods(0x0200 | 0x0400));     // RALT | LGUI
        assertEquals(0, SdlKeys.mods(0x1000 | 0x2000));                                  // num/caps lock only
        int[][] buttons = {{1, 0}, {3, 1}, {2, 2}, {4, 3}, {5, 4}};             // SDL -> GLFW
        for (int[] b : buttons) {
            assertEquals(b[1], SdlKeys.button(b[0]));
            assertEquals(b[0], SdlKeys.sdlButton(b[1]));
        }
    }
}
