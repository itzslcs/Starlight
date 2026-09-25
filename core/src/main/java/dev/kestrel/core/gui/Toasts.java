package dev.kestrel.core.gui;

import dev.kestrel.core.render.Gfx;

import java.util.ArrayList;
import java.util.List;

/** Corner notifications. Rendered on the HUD layer and on top of our screens. */
public final class Toasts {
    private static final float W = 170, PAD = 6;
    private static final long LIFE = 5000;

    private static final class Toast {
        final String title, message;
        final int color;
        final long start;
        List<String> lines;
        final Anim slide = new Anim(0);

        Toast(String title, String message, int color, long start) {
            this.title = title;
            this.message = message;
            this.color = color;
            this.start = start;
        }
    }

    private final List<Toast> toasts = new ArrayList<Toast>();
    public boolean enabled = true;

    public void show(String title, String message, int color) {
        synchronized (toasts) {
            toasts.add(new Toast(title, message == null ? "" : message, color, System.currentTimeMillis()));
            while (toasts.size() > 5) toasts.remove(0);
        }
    }

    public void render(Gfx g, Theme t, float sw, long now) {
        synchronized (toasts) {
            if (toasts.isEmpty()) return;
            float y = 6;
            for (int i = toasts.size() - 1; i >= 0; i--) {
                Toast to = toasts.get(i);
                long age = now - to.start;
                if (age > LIFE + 300) {
                    toasts.remove(i);
                    continue;
                }
                if (!enabled) continue;
                if (to.lines == null) to.lines = wrap(g, to.message, W - PAD * 2 - 4);
                to.slide.to(age > LIFE ? 0f : 1f, 220, now);
                float k = to.slide.get(now);
                float h = PAD * 2 + 10 + to.lines.size() * 10;
                float x = sw - (W + 6) * k;
                g.pushAlpha(k);
                g.roundRect(x, y, W, h, 5, t.panel);
                g.roundRect(x, y, 3, h, 1.5f, to.color);
                g.text(to.title, x + PAD + 3, y + PAD, t.text, false);
                for (int l = 0; l < to.lines.size(); l++) g.text(to.lines.get(l), x + PAD + 3, y + PAD + 11 + l * 10, t.textDim, false);
                g.popAlpha();
                y += (h + 4) * k;
            }
        }
    }

    /** Greedy word wrap (allocates; done once per toast). */
    public static List<String> wrap(Gfx g, String text, float maxW) {
        List<String> out = new ArrayList<String>();
        for (String para : text.split("\n")) {
            StringBuilder line = new StringBuilder();
            for (String word : para.split(" ")) {
                String cand = line.length() == 0 ? word : line + " " + word;
                if (g.textWidth(cand) > maxW && line.length() > 0) {
                    out.add(line.toString());
                    line.setLength(0);
                    line.append(word);
                } else {
                    line.setLength(0);
                    line.append(cand);
                }
            }
            if (line.length() > 0) out.add(line.toString());
        }
        return out;
    }
}
