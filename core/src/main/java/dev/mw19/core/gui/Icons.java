package dev.mw19.core.gui;

import dev.mw19.core.render.Gfx;

/** Original pixel icons drawn from rects in a 10x10 box (no bundled assets; DECISIONS D-009). */
public final class Icons {
    private Icons() {}

    public static void draw(Gfx g, String name, float x, float y, int c) {
        if ("mods".equals(name)) {
            g.roundRect(x, y, 4, 4, 1, c);
            g.roundRect(x + 6, y, 4, 4, 1, c);
            g.roundRect(x, y + 6, 4, 4, 1, c);
            g.roundRect(x + 6, y + 6, 4, 4, 1, c);
        } else if ("hud".equals(name)) {
            g.roundOutline(x, y + 1, 10, 8, 1.5f, 1, c);
            g.rect(x + 2, y + 3, x + 5, y + 4, c);
            g.rect(x + 6, y + 6, x + 8, y + 7, c);
        } else if ("profiles".equals(name)) {
            g.roundRect(x + 3, y, 4, 4, 2, c);
            g.roundRect(x + 1, y + 5, 8, 5, 2, c);
        } else if ("keys".equals(name)) {
            g.roundOutline(x, y + 2, 10, 7, 1.5f, 1, c);
            g.rect(x + 2, y + 4, x + 3, y + 5, c);
            g.rect(x + 4.5f, y + 4, x + 5.5f, y + 5, c);
            g.rect(x + 7, y + 4, x + 8, y + 5, c);
            g.rect(x + 3, y + 6.5f, x + 7, y + 7.5f, c);
        } else if ("plugins".equals(name)) {
            g.roundRect(x, y + 2, 8, 8, 1.5f, c);
            g.roundRect(x + 3, y, 3, 3, 1.5f, c);
            g.roundRect(x + 7, y + 4, 3, 3, 1.5f, c);
        } else if ("rules".equals(name)) {
            g.roundRect(x + 1, y, 8, 6, 1, c);
            g.roundRect(x + 2, y + 5, 6, 3, 1.5f, c);
            g.roundRect(x + 3.5f, y + 7, 3, 3, 1.5f, c);
        } else if ("perf".equals(name)) {
            g.rect(x, y + 6, x + 2, y + 10, c);
            g.rect(x + 3, y + 3, x + 5, y + 10, c);
            g.rect(x + 6, y + 5, x + 8, y + 10, c);
            g.rect(x + 9, y, x + 10, y + 10, c);
        } else if ("themes".equals(name)) {
            g.roundOutline(x, y, 10, 10, 5, 1, c);
            g.roundRect(x + 2, y + 2, 3, 3, 1.5f, c);
            g.roundRect(x + 6, y + 3, 2, 2, 1, c);
            g.roundRect(x + 3, y + 6, 2, 2, 1, c);
        } else if ("about".equals(name)) {
            g.roundOutline(x, y, 10, 10, 5, 1, c);
            g.rect(x + 4.5f, y + 2, x + 5.5f, y + 3, c);
            g.rect(x + 4.5f, y + 4, x + 5.5f, y + 8, c);
        } else if ("gear".equals(name)) {
            g.roundOutline(x + 2, y + 2, 6, 6, 3, 1.5f, c);
            g.rect(x + 4, y, x + 6, y + 2, c);
            g.rect(x + 4, y + 8, x + 6, y + 10, c);
            g.rect(x, y + 4, x + 2, y + 6, c);
            g.rect(x + 8, y + 4, x + 10, y + 6, c);
            g.rect(x + 1, y + 1, x + 2.5f, y + 2.5f, c);
            g.rect(x + 7.5f, y + 1, x + 9, y + 2.5f, c);
            g.rect(x + 1, y + 7.5f, x + 2.5f, y + 9, c);
            g.rect(x + 7.5f, y + 7.5f, x + 9, y + 9, c);
        } else if ("star".equals(name) || "star_on".equals(name)) {
            // 10x10 five-point star, one span per row (same shape filled; colour tells on/off)
            float[][] rows = {{4, 6}, {4, 6}, {3, 7}, {0, 10}, {1, 9}, {2, 8}, {2, 8}, {1, 4}, {6, 9}, {1, 3}, {7, 9}, {0, 2}, {8, 10}};
            int[] ry = {0, 1, 2, 3, 4, 5, 6, 7, 7, 8, 8, 9, 9};
            for (int i = 0; i < rows.length; i++) g.rect(x + rows[i][0], y + ry[i], x + rows[i][1], y + ry[i] + 1, c);
        } else if ("back".equals(name)) {
            for (int i = 0; i < 5; i++) {
                g.rect(x + 5 - i, y + i, x + 6 - i, y + i + 1, c);
                g.rect(x + 5 - i, y + 9 - i, x + 6 - i, y + 10 - i, c);
            }
            g.rect(x + 1, y + 4.5f, x + 10, y + 5.5f, c);
        } else if ("close".equals(name)) {
            for (int i = 0; i < 9; i++) {
                g.rect(x + i, y + i, x + i + 1.5f, y + i + 1.5f, c);
                g.rect(x + 8.5f - i, y + i, x + 10 - i, y + i + 1.5f, c);
            }
        } else if ("search".equals(name)) {
            g.roundOutline(x, y, 7, 7, 3.5f, 1, c);
            for (int i = 0; i < 3; i++) g.rect(x + 6 + i, y + 6 + i, x + 7.5f + i, y + 7.5f + i, c);
        } else if ("check".equals(name)) {
            for (int i = 0; i < 3; i++) g.rect(x + 1 + i, y + 5 + i, x + 2.5f + i, y + 6.5f + i, c);
            for (int i = 0; i < 6; i++) g.rect(x + 4 + i, y + 7 - i, x + 5.5f + i, y + 8.5f - i, c);
        } else {
            g.roundRect(x + 3, y + 3, 4, 4, 2, c);
        }
    }
}
