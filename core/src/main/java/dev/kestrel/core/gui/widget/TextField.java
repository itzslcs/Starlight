package dev.kestrel.core.gui.widget;

import dev.kestrel.api.util.Colors;
import dev.kestrel.core.Keys;
import dev.kestrel.core.gui.Ui;
import dev.kestrel.core.gui.Widget;

/** Single-line text input with cursor, select-all, clipboard and horizontal scrolling. */
public class TextField extends Widget {
    public interface Listener {
        void changed(String text);
    }

    public String text;
    public String placeholder = "";
    public int maxLength = 256;
    public Listener onChange;
    public Runnable onSubmit;
    private int cursor;
    private boolean allSelected;
    private float offset;

    public TextField(String text) {
        this.text = text == null ? "" : text;
        this.cursor = this.text.length();
    }

    @Override
    public void render(Ui ui) {
        boolean focused = ui.root.isFocused(this);
        ui.root.registerFocusable(this);
        ui.g.roundRect(x, y, w, h, 3, ui.t.surface2);
        ui.g.roundOutline(x, y, w, h, 3, 1, focused ? ui.t.accent : hovered(ui) ? Colors.fade(ui.t.text, 0.2f) : ui.t.border);
        float pad = 5, inner = w - pad * 2;
        float cx = ui.g.textWidth(text.substring(0, Math.min(cursor, text.length())));
        if (cx - offset > inner) offset = cx - inner;
        if (cx - offset < 0) offset = cx;
        ui.g.pushClip(x + pad - 1, y, inner + 2, h);
        float ty = y + (h - 8) / 2f;
        if (text.isEmpty() && !focused) {
            ui.g.text(placeholder, x + pad, ty, ui.t.textDim, false);
        } else {
            if (focused && allSelected && !text.isEmpty()) {
                ui.g.rect(x + pad - offset, ty - 1, x + pad - offset + ui.g.textWidth(text), ty + 9, ui.t.accentSoft(0.35f));
            }
            ui.g.text(text, x + pad - offset, ty, ui.t.text, false);
        }
        if (focused && (ui.now / 500) % 2 == 0) {
            float px = x + pad + cx - offset;
            ui.g.rect(px, ty - 1, px + 1, ty + 9, ui.t.accent);
        }
        ui.g.popClip();
        if (hovered(ui) && tooltip != null) ui.tooltip = tooltip;
    }

    @Override
    public boolean mouseClicked(Ui ui, int button) {
        if (!contains(ui.mx, ui.my)) return false;
        ui.root.focus(this);
        allSelected = false;
        float rel = ui.mx - x - 5 + offset;
        int best = text.length();
        for (int i = 0; i <= text.length(); i++) {
            if (ui.g.textWidth(text.substring(0, i)) >= rel) {
                best = i;
                break;
            }
        }
        cursor = best;
        if (button == 1) setText("");
        return true;
    }

    @Override
    public boolean focusable() {
        return true;
    }

    public void setText(String t) {
        String n = t == null ? "" : (t.length() > maxLength ? t.substring(0, maxLength) : t);
        if (n.equals(text)) return;
        text = n;
        cursor = Math.min(cursor, text.length());
        if (onChange != null) onChange.changed(text);
    }

    private void insert(String s) {
        if (allSelected) {
            text = "";
            cursor = 0;
            allSelected = false;
        }
        String clean = s.replaceAll("[\\p{Cntrl}]", "");
        String n = text.substring(0, cursor) + clean + text.substring(cursor);
        if (n.length() > maxLength) return;
        cursor += clean.length();
        text = n;
        if (onChange != null) onChange.changed(text);
    }

    @Override
    public boolean charTyped(Ui ui, char c) {
        if (c < 32 || c == 127 || c == '§') return false;
        insert(String.valueOf(c));
        return true;
    }

    @Override
    public boolean keyPressed(Ui ui, int key, int mods) {
        boolean ctrl = (mods & (Keys.MOD_CONTROL | Keys.MOD_SUPER)) != 0;
        if (ctrl && key == Keys.A) {
            allSelected = true;
            cursor = text.length();
            return true;
        }
        if (ctrl && key == 'C') {
            ui.k.platform.setClipboard(text);
            return true;
        }
        if (ctrl && key == 'X') {
            ui.k.platform.setClipboard(text);
            cursor = 0;
            setText("");
            return true;
        }
        if (ctrl && key == 'V') {
            String clip = ui.k.platform.clipboard();
            if (clip != null) insert(clip.replace('\n', ' '));
            return true;
        }
        switch (key) {
            case Keys.BACKSPACE:
                if (allSelected) {
                    allSelected = false;
                    cursor = 0;
                    setText("");
                } else if (cursor > 0) {
                    int from = ctrl ? wordStart() : cursor - 1;
                    String n = text.substring(0, from) + text.substring(cursor);
                    cursor = from;
                    setText(n);
                }
                return true;
            case Keys.DELETE:
                if (allSelected) {
                    allSelected = false;
                    cursor = 0;
                    setText("");
                } else if (cursor < text.length()) {
                    setText(text.substring(0, cursor) + text.substring(cursor + 1));
                }
                return true;
            case Keys.LEFT:
                allSelected = false;
                cursor = Math.max(0, ctrl ? wordStart() : cursor - 1);
                return true;
            case Keys.RIGHT:
                allSelected = false;
                cursor = Math.min(text.length(), cursor + 1);
                return true;
            case Keys.HOME:
                allSelected = false;
                cursor = 0;
                return true;
            case Keys.END:
                allSelected = false;
                cursor = text.length();
                return true;
            case Keys.ENTER:
            case Keys.KP_ENTER:
                if (onSubmit != null) onSubmit.run();
                return true;
            default:
                return false;
        }
    }

    private int wordStart() {
        int i = cursor - 1;
        while (i > 0 && text.charAt(i - 1) != ' ') i--;
        return Math.max(0, i);
    }
}
