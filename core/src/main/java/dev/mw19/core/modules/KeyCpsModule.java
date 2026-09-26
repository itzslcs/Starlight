package dev.mw19.core.modules;

import dev.mw19.api.game.Game.Binding;
import dev.mw19.api.hud.Anchor;
import dev.mw19.api.hud.HudStyle;
import dev.mw19.api.module.HudModule;
import dev.mw19.api.module.Rule;
import dev.mw19.api.render.Renderer;
import dev.mw19.api.setting.BoolSetting;
import dev.mw19.api.setting.ColorSetting;
import dev.mw19.api.setting.NumberSetting;
import dev.mw19.api.util.Colors;
import dev.mw19.core.Keys;
import dev.mw19.core.Mw19;
import dev.mw19.core.platform.Platform;

import java.util.Locale;

/**
 * KeyCPS: the owner's own keystrokes + CPS mod (KeyCPS 1.6.1), ported into MW19 with the author's permission
 * (DECISIONS D-018). Same layout, animation and counting as the standalone mod. Keys follow the player's bindings,
 * and clicks and OS key-repeat are counted from input events ({@link InputRates}). KeyCPS's own settings screen and
 * move screen are replaced by MW19's module settings and HUD editor. Position, scale, text colour and shadow are
 * set per element in the HUD editor.
 */
public final class KeyCpsModule extends HudModule {
    private static final int K = 22, G = 2, SMALL_ROW = 14;
    private static final float SMALL_TEXT = 0.75f;
    private static final int WARN = 0xFFFF5555;

    private final BoolSetting movement = add(new BoolSetting("movement", "Movement keys", "W, A, S and D", true));
    private final BoolSetting jump = add(new BoolSetting("jump", "Jump key", "Drawn as a space bar when jump is on Space", true));
    private final BoolSetting mouse = add(new BoolSetting("mouse", "Mouse buttons", "Attack and use keys", true));
    private final BoolSetting cps = add(new BoolSetting("cps", "CPS", "Clicks per second inside the mouse buttons", true));
    private final BoolSetting sneakSprint = add(new BoolSetting("sneak_sprint", "Sneak / sprint", "An extra row for sneak and sprint", false));
    private final BoolSetting keyRates = add(new BoolSetting("key_rates", "Rate on every key",
            "How often each key fires per second, key repeat included", false));
    private final BoolSetting mouseLabels = add(new BoolSetting("mouse_labels", "Always LMB / RMB",
            "Label attack and use as LMB/RMB even when they are bound to keyboard keys", false));
    private final NumberSetting cpsWarn = add(new NumberSetting("cps_warn", "CPS warning", "The counter turns red at this CPS (0 = off)",
            0, 0, 30, 1));
    private final ColorSetting background = add(new ColorSetting("background", "Background", "Key background (alpha 0 hides it)", 0x6E000000));
    private final ColorSetting pressed = add(new ColorSetting("pressed", "Pressed colour", "Key background while held", 0xE6FFFFFF));
    private final BoolSetting rounded = add(new BoolSetting("rounded", "Rounded keys", "Cut off the corner pixels", true));
    private final BoolSetting rainbow = add(new BoolSetting("rainbow", "Rainbow text", "Cycle the label colours", false));
    private final BoolSetting fade = add(new BoolSetting("fade", "Fade", "Smooth press and release", true));

    private final InputRates rates;
    private final Key forward = new Key(Binding.FORWARD), left = new Key(Binding.LEFT), back = new Key(Binding.BACK),
            right = new Key(Binding.RIGHT), space = new Key(Binding.JUMP), attack = new Key(Binding.ATTACK),
            use = new Key(Binding.USE), sneak = new Key(Binding.SNEAK), sprint = new Key(Binding.SPRINT);
    private final Key[] keys = {forward, left, back, right, space, attack, use, sneak, sprint};
    private long lastFrame, labelsAt;

    public KeyCpsModule(InputRates rates) {
        super("keycps", "KeyCPS", "Keystrokes and CPS that follow your key bindings", Rule.ALLOWED, true, Anchor.LEFT, 4, 0);
        this.rates = rates;
        cps.visibleWhen(mouse::on);
    }

    @Override
    public boolean wantsBackground() {
        return false; // every key draws its own box
    }

    @Override
    public float width(Renderer r) {
        refreshLabels(r.millis());
        int w = 3 * K + 2 * G;
        if (mouse.on()) w = Math.max(w, 2 * (Math.max(textW(r, label(attack)), textW(r, label(use))) + 8) + G);
        if (sneakSprint.on()) w = Math.max(w, 2 * (Math.max(textW(r, sneak.label), textW(r, sprint.label)) + 8) + G);
        return w;
    }

    @Override
    public float height(Renderer r) {
        int h = 0;
        if (movement.on()) h += 2 * (K + G);
        if (jump.on()) h += SMALL_ROW - 2 + G;
        if (mouse.on()) h += (cps.on() ? K + 4 : K) + G;
        if (sneakSprint.on()) h += SMALL_ROW + G;
        return Math.max(0, h - G);
    }

    @Override
    public void render(Renderer r, HudStyle style, boolean preview) {
        long now = r.millis();
        float step = lastFrame == 0 ? 1 : Math.min(1, (now - lastFrame) / 1000f * 14);
        lastFrame = now;
        int w = (int) width(r);
        int y = 0;
        if (movement.on()) {
            key(r, style, forward, (w - K) / 2, y, K, K, step, false, now);
            y += K + G;
            int ax = (w - (3 * K + 2 * G)) / 2;
            key(r, style, left, ax, y, K, K, step, false, now);
            key(r, style, back, ax + K + G, y, K, K, step, false, now);
            key(r, style, right, ax + 2 * (K + G), y, K, K, step, false, now);
            y += K + G;
        }
        if (jump.on()) {
            key(r, style, space, 0, y, w, SMALL_ROW - 2, step, false, now);
            y += SMALL_ROW - 2 + G;
        }
        if (mouse.on()) {
            int mw = (w - G) / 2, mh = cps.on() ? K + 4 : K;
            key(r, style, attack, 0, y, mw, mh, step, cps.on(), now);
            key(r, style, use, mw + G, y, w - mw - G, mh, step, cps.on(), now);
            y += mh + G;
        }
        if (sneakSprint.on()) {
            int mw = (w - G) / 2;
            key(r, style, sneak, 0, y, mw, SMALL_ROW, step, false, now);
            key(r, style, sprint, mw + G, y, w - mw - G, SMALL_ROW, step, false, now);
        }
    }

    private void key(Renderer r, HudStyle style, Key k, int x, int y, int w, int h, float step, boolean cpsLine, long now) {
        Platform p = Mw19.get().platform;
        float target = p.inWorld() && p.bindingDown(k.binding) ? 1 : 0;
        k.glow = fade.on() ? k.glow + (target - k.glow) * step : target;

        int onBg = pressed.argb(now);
        box(r, x, y, x + w, y + h, Colors.lerp(background.argb(now), onBg, k.glow));

        int offText = rainbow.on() ? rainbow(now, x + y) : style.textColor();
        int text = Colors.lerp(offText, contrast(onBg), k.glow);
        boolean shadow = style.textShadow() && k.glow < 0.5f;
        int rate = rates.rate(k.binding, now);

        String secondary = null;
        int secondaryColor = text;
        if (cpsLine) {
            secondary = k.rateText(rate, true);
            if (cpsWarn.intValue() > 0 && rate >= cpsWarn.intValue()) secondaryColor = WARN;
        } else if (keyRates.on() && rate > 0 && h >= K) {
            secondary = k.rateText(rate, false);
        }

        if (k == space && k.code == Keys.SPACE) { // the space bar is drawn as a line, like on a keyboard
            int lw = w / 3, ly = y + h / 2;
            if (shadow) r.rect(x + (w - lw) / 2 + 1, ly + 1, x + (w + lw) / 2 + 1, ly + 2, (text & 0xFCFCFC) >> 2 | 0xFF000000);
            r.rect(x + (w - lw) / 2, ly, x + (w + lw) / 2, ly + 1, text);
            return;
        }
        int cx = x + w / 2;
        if (secondary == null) {
            centered(r, label(k), cx, y + (h - 7) / 2, text, shadow);
        } else {
            centered(r, label(k), cx, y + (h - 14) / 2, text, shadow);
            r.push();
            r.translate(cx, y + (h - 14) / 2 + 9);
            r.scale(SMALL_TEXT);
            centered(r, secondary, 0, 0, secondaryColor, shadow);
            r.pop();
        }
    }

    private String label(Key k) {
        if (mouseLabels.on() && k == attack) return "LMB";
        if (mouseLabels.on() && k == use) return "RMB";
        return k.label;
    }

    /** Bindings can change at any time (Controls screen); re-read them twice a second rather than every frame. */
    private void refreshLabels(long now) {
        if (now - labelsAt < 500 && labelsAt != 0) return;
        labelsAt = now;
        Platform p = Mw19.get().platform;
        for (Key k : keys) {
            k.code = p.bindingKey(k.binding);
            k.label = keyLabel(p, k.binding, k.code);
        }
    }

    static String keyLabel(Platform p, Binding b, int code) {
        if (code == Keys.NONE) return "-";
        if (Keys.isMouse(code)) {
            int button = code - Keys.MOUSE_BASE;
            return button == 0 ? "LMB" : button == 1 ? "RMB" : button == 2 ? "MMB" : "M" + (button + 1);
        }
        switch (code) {
            case Keys.SPACE: return "Space";
            case Keys.ENTER: return "Enter";
            case Keys.TAB: return "Tab";
            case Keys.CAPS_LOCK: return "Caps";
            case Keys.LEFT_SHIFT: case Keys.RIGHT_SHIFT: return "Shift";
            case Keys.LEFT_CONTROL: case Keys.RIGHT_CONTROL: return "Ctrl";
            case Keys.LEFT_ALT: case Keys.RIGHT_ALT: return "Alt";
            default:
                String name = p.bindingName(b); // layout-aware (AZERTY shows Z Q S D)
                return name.length() > 6 ? name.substring(0, 6) : name.toUpperCase(Locale.ROOT);
        }
    }

    private void box(Renderer r, int x1, int y1, int x2, int y2, int argb) {
        if ((argb >>> 24) == 0) return;
        if (!rounded.on()) {
            r.rect(x1, y1, x2, y2, argb);
            return;
        }
        r.rect(x1 + 1, y1, x2 - 1, y2, argb);
        r.rect(x1, y1 + 1, x1 + 1, y2 - 1, argb);
        r.rect(x2 - 1, y1 + 1, x2, y2 - 1, argb);
    }

    private static int textW(Renderer r, String s) {
        return (int) r.textWidth(s);
    }

    private static void centered(Renderer r, String s, int cx, int y, int argb, boolean shadow) {
        r.text(s, cx - textW(r, s) / 2, y, argb, shadow);
    }

    /** Black or white, whichever reads better on the given colour. */
    static int contrast(int argb) {
        double lum = 0.299 * (argb >> 16 & 0xFF) + 0.587 * (argb >> 8 & 0xFF) + 0.114 * (argb & 0xFF);
        return lum > 140 ? 0xFF000000 : 0xFFFFFFFF;
    }

    private static int rainbow(long now, int offset) {
        float hue = (now % 4000L) / 4000f + offset / 200f;
        return 0xFF000000 | Colors.hsb(hue, 1f, 1f);
    }

    private static final class Key {
        final Binding binding;
        int code = Keys.NONE;
        String label = "";
        float glow;
        private int shownRate = -1;
        private boolean shownCps;
        private String rateText = "";

        Key(Binding binding) {
            this.binding = binding;
        }

        /** "7 CPS" / "7", rebuilt only when the number changes (no per-frame allocation). */
        String rateText(int rate, boolean cps) {
            if (rate != shownRate || cps != shownCps) {
                shownRate = rate;
                shownCps = cps;
                rateText = cps ? rate + " CPS" : String.valueOf(rate);
            }
            return rateText;
        }
    }
}
