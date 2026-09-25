package dev.kestrel.core.modules;

import dev.kestrel.api.game.Game;
import dev.kestrel.api.hud.Anchor;
import dev.kestrel.api.hud.HudStyle;
import dev.kestrel.api.module.HudModule;
import dev.kestrel.api.module.Rule;
import dev.kestrel.api.render.ItemRef;
import dev.kestrel.api.render.Renderer;
import dev.kestrel.api.setting.BoolSetting;
import dev.kestrel.api.setting.ChoiceSetting;
import dev.kestrel.core.Kestrel;

/** Armor pieces and durability (listed as allowed by Hypixel: "Armor Status"). */
public final class ArmorModule extends HudModule {
    private static final float ROW = 17;

    private final ChoiceSetting layout = add(new ChoiceSetting("layout", "Layout", "Stack direction", "Vertical", "Vertical", "Horizontal"));
    private final ChoiceSetting durability = add(new ChoiceSetting("durability", "Durability", "How durability is shown", "Value", "Value", "Percent", "None"));
    private final BoolSetting hand = add(new BoolSetting("hand", "Held item", "Also show the item in your main hand", true));
    private final BoolSetting colored = add(new BoolSetting("colored", "Colour by damage", "Green → yellow → red as durability drops", true));

    // cached per-row text so the frame path does not build strings
    private final String[] text = new String[5];
    private final int[] key = new int[5];
    private final int[] color = new int[5];
    private final ItemRef[] items = new ItemRef[5];
    private int rows;

    public ArmorModule() {
        super("armor", "Armor Status", "Your armor and held item with durability", Rule.ALLOWED, true, Anchor.RIGHT, 4, 0);
        for (int i = 0; i < 5; i++) key[i] = Integer.MIN_VALUE;
    }

    private void collect(boolean preview) {
        Game g = Kestrel.get().platform;
        rows = 0;
        if (!g.inWorld()) return;
        for (int slot = 3; slot >= 0; slot--) {
            ItemRef it = g.armor(slot);
            if (!it.isEmpty()) items[rows++] = it;
        }
        if (hand.on()) {
            ItemRef it = g.mainHand();
            if (!it.isEmpty()) items[rows++] = it;
        }
        for (int i = 0; i < rows; i++) refresh(i, items[i]);
    }

    private void refresh(int i, ItemRef it) {
        int max = it.maxDamage(), dmg = it.damage();
        int k = (max << 16) ^ dmg ^ (durability.get().hashCode() * 31) ^ (it.count() << 24);
        if (k == key[i] && text[i] != null) return;
        key[i] = k;
        if (max <= 0 || durability.is("None")) {
            text[i] = it.count() > 1 ? Integer.toString(it.count()) : "";
            color[i] = 0;
            return;
        }
        int left = max - dmg;
        float frac = left / (float) max;
        text[i] = durability.is("Percent") ? Math.round(frac * 100) + "%" : Integer.toString(left);
        color[i] = frac > 0.6f ? 0xFF55FF55 : frac > 0.3f ? 0xFFFFFF55 : frac > 0.15f ? 0xFFFFAA00 : 0xFFFF5555;
    }

    @Override
    public boolean visible(boolean preview) {
        collect(preview);
        return preview || rows > 0;
    }

    @Override
    public float width(Renderer r) {
        int n = Math.max(rows, 1);
        if (layout.is("Horizontal")) return (n - 1) * col(r) + Math.max(16, hasText() ? maxText(r) : 0);
        return 16 + (hasText() ? 3 + maxText(r) : 0);
    }

    @Override
    public float height(Renderer r) {
        int n = Math.max(rows, 1);
        if (layout.is("Horizontal")) return 16 + (hasText() ? 10 : 0);
        return n * ROW - 1;
    }

    private boolean hasText() {
        if (durability.is("None")) return false;
        for (int i = 0; i < rows; i++) if (text[i] != null && !text[i].isEmpty()) return true;
        return rows == 0;
    }

    private float col(Renderer r) {
        return Math.max(ROW + 1, (hasText() ? maxText(r) : 0) + 3);
    }

    private float maxText(Renderer r) {
        float m = rows == 0 ? r.textWidth("100%") : 0;
        for (int i = 0; i < rows; i++) m = Math.max(m, r.textWidth(text[i]));
        return m;
    }

    @Override
    public void render(Renderer r, HudStyle style, boolean preview) {
        if (rows == 0) {
            if (preview) {
                r.roundOutline(0, 0, 16, 16, 2, 1, 0x80FFFFFF);
                r.text("—", 5, 4, style.textColor(), style.textShadow());
            }
            return;
        }
        boolean h = layout.is("Horizontal");
        float col = h ? col(r) : 0;
        for (int i = 0; i < rows; i++) {
            float x = h ? i * col : 0, y = h ? 0 : i * ROW;
            r.item(items[i], x, y);
            String t = text[i];
            if (t == null || t.isEmpty()) continue;
            int tc = colored.on() && color[i] != 0 ? color[i] : style.textColor();
            if (h) r.text(t, x + (16 - r.textWidth(t)) / 2f, y + 17, tc, style.textShadow());
            else r.text(t, x + 19, y + 4, tc, style.textShadow());
        }
    }
}
