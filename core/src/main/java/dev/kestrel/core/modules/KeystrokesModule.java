package dev.kestrel.core.modules;

import dev.kestrel.api.game.Game;
import dev.kestrel.api.hud.Anchor;
import dev.kestrel.api.hud.HudStyle;
import dev.kestrel.api.module.HudModule;
import dev.kestrel.api.module.Rule;
import dev.kestrel.api.render.Renderer;
import dev.kestrel.api.setting.BoolSetting;
import dev.kestrel.api.setting.ColorSetting;
import dev.kestrel.api.util.Colors;
import dev.kestrel.core.Kestrel;
import dev.kestrel.core.gui.Anim;

/** Shows which movement/mouse keys are held (the player's own input; display only). */
public final class KeystrokesModule extends HudModule {
    private static final float K = 18, GAP = 2;
    private static final Game.Binding[] KEYS = {Game.Binding.FORWARD, Game.Binding.LEFT, Game.Binding.BACK, Game.Binding.RIGHT};

    private final BoolSetting mouse = add(new BoolSetting("mouse", "Mouse buttons", "Show LMB/RMB with CPS", true));
    private final BoolSetting space = add(new BoolSetting("space", "Space bar", "Show the jump key", true));
    private final ColorSetting pressed = add(new ColorSetting("pressed", "Pressed colour", "Key background while held", 0xE0FFFFFF));
    private final ColorSetting idle = add(new ColorSetting("idle", "Idle colour", "Key background", 0x70000000));

    private final Anim[] anim = new Anim[7];
    private final String[] labels = new String[4];
    private int labelAge;
    private final ClickTracker clicks;
    private int lastL = -1, lastR = -1;
    private String lText = "LMB", rText = "RMB";

    public KeystrokesModule(ClickTracker clicks) {
        super("keystrokes", "Keystrokes", "Movement keys and mouse buttons you are holding", Rule.ALLOWED, true, Anchor.BOTTOM_LEFT, 4, 4);
        this.clicks = clicks;
        for (int i = 0; i < anim.length; i++) anim[i] = new Anim(0);
    }

    @Override
    public boolean wantsBackground() {
        return false;
    }

    @Override
    public float width(Renderer r) {
        return K * 3 + GAP * 2;
    }

    @Override
    public float height(Renderer r) {
        float h = K * 2 + GAP;
        if (mouse.on()) h += K + GAP;
        if (space.on()) h += 10 + GAP;
        return h;
    }

    @Override
    public void render(Renderer r, HudStyle style, boolean preview) {
        Game game = Kestrel.get().platform;
        long now = r.millis();
        if (labelAge-- <= 0) {
            labelAge = 40;
            for (int i = 0; i < 4; i++) labels[i] = game.bindingName(KEYS[i]);
        }
        key(r, style, K + GAP, 0, K, K, labels[0], down(game, Game.Binding.FORWARD), 0, now);
        key(r, style, 0, K + GAP, K, K, labels[1], down(game, Game.Binding.LEFT), 1, now);
        key(r, style, K + GAP, K + GAP, K, K, labels[2], down(game, Game.Binding.BACK), 2, now);
        key(r, style, (K + GAP) * 2, K + GAP, K, K, labels[3], down(game, Game.Binding.RIGHT), 3, now);
        float y = (K + GAP) * 2;
        if (mouse.on()) {
            float w = (K * 3 + GAP) / 2f;
            int lc = clicks.cps(0, now), rc = clicks.cps(1, now);
            if (lc != lastL) {
                lastL = lc;
                lText = lc > 0 ? lc + " CPS" : "LMB";
            }
            if (rc != lastR) {
                lastR = rc;
                rText = rc > 0 ? rc + " CPS" : "RMB";
            }
            key(r, style, 0, y, w, K, lText, down(game, Game.Binding.ATTACK), 4, now);
            key(r, style, w + GAP, y, w, K, rText, down(game, Game.Binding.USE), 5, now);
            y += K + GAP;
        }
        if (space.on()) {
            float w = K * 3 + GAP * 2;
            float t = anim[6].get(now);
            anim[6].to(down(game, Game.Binding.JUMP) ? 1 : 0, 80, now);
            r.roundRect(0, y, w, 10, 2, Colors.lerp(idle.argb(now), pressed.argb(now), t));
            r.roundRect(w / 2 - 12, y + 4, 24, 2, 1, Colors.lerp(style.textColor(), 0xFF000000, t));
        }
    }

    private static boolean down(Game g, Game.Binding b) {
        return g.inWorld() && g.bindingDown(b);
    }

    private void key(Renderer r, HudStyle style, float x, float y, float w, float h, String label, boolean isDown, int i, long now) {
        anim[i].to(isDown ? 1 : 0, 80, now);
        float t = anim[i].get(now);
        r.roundRect(x, y, w, h, 2, Colors.lerp(idle.argb(now), pressed.argb(now), t));
        int col = Colors.lerp(style.textColor(), 0xFF000000, t);
        float tw = r.textWidth(label);
        r.text(label, x + (w - tw) / 2f, y + (h - 8) / 2f, col, style.textShadow() && t < 0.5f);
    }
}
