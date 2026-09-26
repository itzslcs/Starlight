package dev.mw19.core.gui.widget;

import dev.mw19.api.setting.ColorSetting;
import dev.mw19.api.util.Colors;
import dev.mw19.core.Keys;
import dev.mw19.core.gui.Ui;
import dev.mw19.core.gui.Widget;

import java.util.Locale;

/** Colour preview that opens an HSB + alpha + chroma picker. */
public class ColorSwatch extends Widget {
    private final ColorSetting s;

    public ColorSwatch(ColorSetting s) {
        this.s = s;
    }

    @Override
    public void render(Ui ui) {
        float sw = 26, sx = x + w - sw;
        checker(ui, sx, y + 2, sw, h - 4);
        ui.g.roundRect(sx, y + 2, sw, h - 4, 3, s.argb(ui.now));
        ui.g.roundOutline(sx, y + 2, sw, h - 4, 3, 1, hovered(ui) ? ui.t.text : ui.t.border);
        String hex = s.chroma() ? "Chroma" : hex(s.get());
        ui.g.textRight(hex, sx - 4, y + (h - 8) / 2f, ui.t.textDim, false);
        focusRing(ui, 3);
        if (hovered(ui) && tooltip != null) ui.tooltip = tooltip;
    }

    static void checker(Ui ui, float x, float y, float w, float h) {
        ui.g.roundRect(x, y, w, h, 3, 0xFFFFFFFF);
        for (float cx = x; cx < x + w; cx += 4) {
            for (float cy = y; cy < y + h; cy += 4) {
                if ((((int) ((cx - x) / 4)) + ((int) ((cy - y) / 4))) % 2 == 0) {
                    ui.g.rect(cx, cy, Math.min(cx + 4, x + w), Math.min(cy + 4, y + h), 0xFFCCCCCC);
                }
            }
        }
    }

    static String hex(int argb) {
        return String.format(Locale.ROOT, "#%08X", argb);
    }

    @Override
    public boolean mouseClicked(Ui ui, int button) {
        if (button != 0 || !contains(ui.mx, ui.my)) return false;
        activate(ui);
        return true;
    }

    @Override
    public boolean focusable() {
        return true;
    }

    @Override
    public void activate(Ui ui) {
        if (ui.root.popupOwner() == this) {
            ui.root.closePopup();
            return;
        }
        float pw = 156, ph = 146;
        float px = Math.min(x + w - pw, ui.root.width() - pw - 4);
        float py = y + h + 2 + ph > ui.root.height() ? y - ph - 2 : y + h + 2;
        ui.root.openPopup(this, new Picker().bounds(Math.max(4, px), Math.max(4, py), pw, ph));
    }

    private final class Picker extends Widget {
        private float hue, sat, val;
        private int drag; // 0 none, 1 sv, 2 hue, 3 alpha
        private final TextField hexField;

        Picker() {
            float[] hsb = Colors.toHsb(s.get());
            hue = hsb[0];
            sat = hsb[1];
            val = hsb[2];
            hexField = new TextField(hex(s.get()));
            hexField.maxLength = 9;
            hexField.onSubmit = new Runnable() {
                @Override
                public void run() {
                    applyHex();
                }
            };
        }

        private void applyHex() {
            String t = hexField.text.trim().replace("#", "");
            try {
                long v = Long.parseLong(t, 16);
                int argb = t.length() <= 6 ? (int) (0xFF000000L | v) : (int) v;
                s.set(argb);
                float[] hsb = Colors.toHsb(argb);
                hue = hsb[0];
                sat = hsb[1];
                val = hsb[2];
            } catch (NumberFormatException ignored) {
                hexField.setText(hex(s.get()));
            }
        }

        private void push() {
            int rgb = Colors.hsb(hue, sat, val) & 0xFFFFFF;
            s.set((s.get() & 0xFF000000) | rgb);
            hexField.setText(hex(s.get()));
        }

        @Override
        public void render(Ui ui) {
            if (drag != 0) drag(ui);
            ui.g.roundRect(x, y, w, h, 5, ui.t.surface);
            ui.g.roundOutline(x, y, w, h, 5, 1, ui.t.border);
            float sx = x + 6, sy = y + 6, ss = 96;
            // saturation/value square: hue base, white→transparent horizontally, transparent→black vertically
            ui.g.rect(sx, sy, sx + ss, sy + ss, Colors.hsb(hue, 1, 1));
            ui.g.gradientH(sx, sy, sx + ss, sy + ss, 0xFFFFFFFF, 0x00FFFFFF);
            ui.g.gradient(sx, sy, sx + ss, sy + ss, 0x00000000, 0xFF000000);
            float kx = sx + sat * ss, ky = sy + (1 - val) * ss;
            ui.g.roundOutline(kx - 3, ky - 3, 6, 6, 3, 1, 0xFFFFFFFF);
            // hue bar
            float hx = sx + ss + 6;
            for (int i = 0; i < 6; i++) {
                ui.g.gradient(hx, sy + i * ss / 6f, hx + 10, sy + (i + 1) * ss / 6f, Colors.hsb(i / 6f, 1, 1), Colors.hsb((i + 1) / 6f, 1, 1));
            }
            ui.g.rect(hx - 1, sy + hue * ss - 1, hx + 11, sy + hue * ss + 1, 0xFFFFFFFF);
            // alpha bar
            float ax = hx + 16;
            checker(ui, ax, sy, 10, ss);
            ui.g.gradient(ax, sy, ax + 10, sy + ss, s.get() | 0xFF000000, s.get() & 0x00FFFFFF);
            float a = (s.get() >>> 24) / 255f;
            ui.g.rect(ax - 1, sy + (1 - a) * ss - 1, ax + 11, sy + (1 - a) * ss + 1, 0xFFFFFFFF);
            // hex + chroma
            hexField.bounds(sx, sy + ss + 6, 70, 14);
            hexField.render(ui);
            float cx = sx + 76;
            ui.g.roundRect(cx, sy + ss + 6, 60, 14, 3, s.chroma() ? ui.t.accent : ui.t.surface2);
            ui.g.textCentered("Chroma", cx + 30, sy + ss + 9, s.chroma() ? ui.t.onAccent : ui.t.text, false);
            ui.g.text("Hue / alpha / hex; Enter applies hex", sx, y + h - 11, ui.t.textDim, false);
        }

        private void drag(Ui ui) {
            float sx = x + 6, sy = y + 6, ss = 96;
            if (drag == 1) {
                sat = clamp((ui.mx - sx) / ss);
                val = 1 - clamp((ui.my - sy) / ss);
                push();
            } else if (drag == 2) {
                hue = Math.min(0.999f, clamp((ui.my - sy) / ss));
                push();
            } else if (drag == 3) {
                int alpha = Math.round((1 - clamp((ui.my - sy) / ss)) * 255);
                s.set(Colors.withAlpha(s.get(), alpha));
                hexField.setText(hex(s.get()));
            }
        }

        private float clamp(float v) {
            return Math.max(0, Math.min(1, v));
        }

        @Override
        public boolean mouseClicked(Ui ui, int button) {
            if (!contains(ui.mx, ui.my)) return false;
            float sx = x + 6, sy = y + 6, ss = 96, hx = sx + ss + 6, ax = hx + 16;
            if (hexField.mouseClicked(ui, button)) return true;
            if (ui.my >= sy + ss + 6 && ui.my < sy + ss + 20 && ui.mx >= sx + 76 && ui.mx < sx + 136) {
                s.setChroma(!s.chroma());
                return true;
            }
            if (ui.my >= sy && ui.my < sy + ss) {
                if (ui.mx >= sx && ui.mx < sx + ss) drag = 1;
                else if (ui.mx >= hx && ui.mx < hx + 10) drag = 2;
                else if (ui.mx >= ax && ui.mx < ax + 10) drag = 3;
                if (drag != 0) {
                    ui.root.capture(this);
                    drag(ui);
                }
            }
            return true;
        }

        @Override
        public boolean mouseDragged(Ui ui, int button) {
            if (drag != 0) drag(ui);
            return drag != 0;
        }

        @Override
        public boolean mouseReleased(Ui ui, int button) {
            drag = 0;
            return true;
        }

        @Override
        public boolean keyPressed(Ui ui, int key, int mods) {
            if (ui.root.isFocused(hexField)) return hexField.keyPressed(ui, key, mods);
            return key == Keys.ENTER;
        }

        @Override
        public boolean charTyped(Ui ui, char c) {
            return ui.root.isFocused(hexField) && hexField.charTyped(ui, c);
        }
    }
}
