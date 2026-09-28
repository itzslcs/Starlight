package dev.starlight.core.modules;

import dev.starlight.api.game.Game;
import dev.starlight.api.hud.Anchor;
import dev.starlight.api.hud.HudStyle;
import dev.starlight.api.module.HudModule;
import dev.starlight.api.module.Rule;
import dev.starlight.api.render.Renderer;
import dev.starlight.api.setting.BoolSetting;
import dev.starlight.api.setting.ChoiceSetting;
import dev.starlight.api.setting.NumberSetting;
import dev.starlight.core.Starlight;

import java.util.Locale;

/** XYZ (same as F3). Hidden whenever the server enables reduced debug info. */
public final class CoordsModule extends HudModule {
    private final ChoiceSetting layout = add(new ChoiceSetting("layout", "Layout", "One line or stacked", "Stacked", "Stacked", "One line"));
    private final NumberSetting decimals = add(new NumberSetting("decimals", "Decimals", "Digits after the point", 1, 0, 3, 1));
    private final BoolSetting facing = add(new BoolSetting("facing", "Facing", "Show the direction you face (N/E/S/W and axis)", true));
    private final BoolSetting nether = add(new BoolSetting("nether", "Other-dimension XZ", "Show the matching Nether/Overworld X Z (÷8 / ×8)", false));

    private final String[] lines = new String[5];
    private int count;
    private long lastKey = Long.MIN_VALUE;

    public CoordsModule() {
        super("coords", "Coordinates", "Position and facing (hidden when the server reduces debug info)", Rule.ALLOWED, false, Anchor.TOP_LEFT, 4, 40);
    }

    @Override
    public boolean visible(boolean preview) {
        Game g = Starlight.get().platform;
        return preview || (g.inWorld() && !Starlight.get().platform.reducedDebugInfo());
    }

    private void refresh() {
        Game g = Starlight.get().platform;
        int d = decimals.intValue();
        double scale = Math.pow(10, d);
        long qx = Math.round(g.x() * scale), qy = Math.round(g.y() * scale), qz = Math.round(g.z() * scale);
        int yawBucket = facing.on() ? Math.floorMod(Math.round(g.yaw() / 45f), 8) : -1;
        long k = qx * 31 + qy * 961 + qz * 29791 + yawBucket * 7 + (layout.is("Stacked") ? 1 : 0) + (nether.on() ? 3 : 0) + d * 11;
        if (k == lastKey) return;
        lastKey = k;
        String fmt = "%." + d + "f";
        String x = String.format(Locale.ROOT, fmt, g.x()), y = String.format(Locale.ROOT, fmt, g.y()), z = String.format(Locale.ROOT, fmt, g.z());
        count = 0;
        if (layout.is("Stacked")) {
            lines[count++] = "X " + x;
            lines[count++] = "Y " + y;
            lines[count++] = "Z " + z;
        } else {
            lines[count++] = x + ", " + y + ", " + z;
        }
        if (yawBucket >= 0) lines[count++] = FACING[yawBucket];
        if (nether.on()) lines[count++] = String.format(Locale.ROOT, "⇄ %d, %d", Math.round(g.x() / 8), Math.round(g.z() / 8));
    }

    private static final String[] FACING = {"S (+Z)", "SW", "W (-X)", "NW", "N (-Z)", "NE", "E (+X)", "SE"};

    @Override
    public float width(Renderer r) {
        refresh();
        float w = 0;
        for (int i = 0; i < count; i++) w = Math.max(w, r.textWidth(lines[i]));
        return w;
    }

    @Override
    public float height(Renderer r) {
        return count * 10 - 1;
    }

    @Override
    public void render(Renderer r, HudStyle style, boolean preview) {
        for (int i = 0; i < count; i++) r.text(lines[i], 0, i * 10, style.textColor(), style.textShadow());
    }
}
