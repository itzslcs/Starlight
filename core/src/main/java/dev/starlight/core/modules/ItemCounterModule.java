package dev.starlight.core.modules;

import dev.starlight.api.hud.Anchor;
import dev.starlight.api.hud.HudStyle;
import dev.starlight.api.module.HudModule;
import dev.starlight.api.module.Rule;
import dev.starlight.api.render.ItemRef;
import dev.starlight.api.render.Renderer;
import dev.starlight.api.setting.BoolSetting;
import dev.starlight.api.setting.ChoiceSetting;
import dev.starlight.api.setting.ListSetting;
import dev.starlight.core.Starlight;

import java.util.Arrays;
import java.util.List;

/** Counts of chosen items in your inventory (arrows, pearls…). Counts are refreshed each tick. */
public final class ItemCounterModule extends HudModule {
    private static final int MAX = 16;
    private final ListSetting items = add(new ListSetting("items", "Items", "Item ids to count, e.g. minecraft:arrow",
            Arrays.asList("minecraft:arrow", "minecraft:ender_pearl", "minecraft:golden_apple"), MAX));
    private final ChoiceSetting layout = add(new ChoiceSetting("layout", "Layout", "Stack direction", "Vertical", "Vertical", "Horizontal"));
    private final BoolSetting hideZero = add(new BoolSetting("hide_zero", "Hide zero", "Hide items you have none of", true));

    private final ItemRef[] icons = new ItemRef[MAX];
    private final int[] counts = new int[MAX];
    private final String[] labels = new String[MAX];
    private final int[] shown = new int[MAX];
    private int n;

    public ItemCounterModule() {
        super("item_counter", "Item Counter", "How many of chosen items you carry", Rule.ALLOWED, false, Anchor.RIGHT, 4, 60);
        Arrays.fill(shown, -1);
    }

    @Override
    public void onTick() {
        List<String> ids = items.get();
        n = 0;
        for (int i = 0; i < ids.size() && n < MAX; i++) {
            int c = Starlight.get().platform.countItem(ids.get(i));
            if (c == 0 && hideZero.on()) continue;
            icons[n] = Starlight.get().platform.itemIcon(ids.get(i));
            if (icons[n].isEmpty()) continue;
            if (shown[n] != c || labels[n] == null) {
                shown[n] = c;
                labels[n] = Integer.toString(c);
            }
            counts[n++] = c;
        }
    }

    @Override
    public boolean visible(boolean preview) {
        return preview || n > 0;
    }

    private boolean horizontal() {
        return layout.is("Horizontal");
    }

    @Override
    public float width(Renderer r) {
        int k = Math.max(1, n);
        return horizontal() ? k * 30 - 4 : 18 + r.textWidth("999");
    }

    @Override
    public float height(Renderer r) {
        int k = Math.max(1, n);
        return horizontal() ? 16 : k * 17 - 1;
    }

    @Override
    public void render(Renderer r, HudStyle style, boolean preview) {
        if (n == 0) {
            r.roundOutline(0, 0, 16, 16, 2, 1, 0x80FFFFFF);
            r.text("0", 19, 4, style.textColor(), style.textShadow());
            return;
        }
        for (int i = 0; i < n; i++) {
            float x = horizontal() ? i * 30 : 0, y = horizontal() ? 0 : i * 17;
            r.item(icons[i], x, y);
            r.text(labels[i], x + 18, y + 4, style.textColor(), style.textShadow());
        }
    }
}
