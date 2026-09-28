package dev.starlight.core.gui.widget;

import dev.starlight.api.setting.KeySetting;
import dev.starlight.api.util.Colors;
import dev.starlight.core.Keys;
import dev.starlight.core.gui.Ui;
import dev.starlight.core.gui.Widget;

/** Click, then press a key or mouse button. Esc cancels, Backspace/Delete unbinds. Shows conflicts. */
public class KeybindButton extends Widget {
    private final KeySetting setting;
    private boolean listening;

    public KeybindButton(KeySetting setting) {
        this.setting = setting;
    }

    public boolean listening() {
        return listening;
    }

    @Override
    public void render(Ui ui) {
        if (listening && !ui.root.isFocused(this)) listening = false;
        String conflict = setting.bound() ? ui.root.keyConflicts(setting) : null;
        boolean hv = hovered(ui);
        int bg = listening ? ui.t.accentSoft(0.3f) : hv ? Colors.lerp(ui.t.surface2, ui.t.text, 0.08f) : ui.t.surface2;
        ui.g.roundRect(x, y, w, h, 3, bg);
        if (conflict != null) ui.g.roundOutline(x, y, w, h, 3, 1, ui.t.warn);
        String label = listening ? "> press a key <" : Keys.name(setting.key());
        int col = listening ? ui.t.accent : conflict != null ? ui.t.warn : setting.bound() ? ui.t.text : ui.t.textDim;
        ui.g.textCentered(label, x + w / 2f, y + (h - 8) / 2f, col, false);
        focusRing(ui, 3);
        if (hv) ui.tooltip = conflict != null ? "Also bound to: " + conflict : "Click, then press a key. Backspace clears, Esc cancels.";
    }

    @Override
    public boolean mouseClicked(Ui ui, int button) {
        if (listening) {
            setting.set(Keys.mouse(button));
            listening = false;
            return true;
        }
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
        ui.root.focus(this);
        listening = true;
    }

    /** While listening this captures every key, including Esc and Tab. */
    @Override
    public boolean keyPressed(Ui ui, int key, int mods) {
        if (!listening) return false;
        if (key == Keys.ESCAPE) {
            listening = false;
        } else if (key == Keys.BACKSPACE || key == Keys.DELETE) {
            setting.set(Keys.NONE);
            listening = false;
        } else {
            setting.set(key);
            listening = false;
        }
        return true;
    }
}
