package dev.starlight.core;

import dev.starlight.api.hud.Anchor;
import dev.starlight.api.hud.HudStyle;
import dev.starlight.api.module.Category;
import dev.starlight.api.module.HudModule;
import dev.starlight.api.module.Module;
import dev.starlight.api.module.Rule;
import dev.starlight.api.render.Renderer;
import dev.starlight.api.setting.BoolSetting;
import dev.starlight.api.setting.NumberSetting;

final class TestModules {
    private TestModules() {}

    static final class Plain extends Module {
        final BoolSetting flag = add(new BoolSetting("flag", "Flag", "", false));
        final NumberSetting num = add(new NumberSetting("num", "Num", "", 5, 0, 10, 1));
        int enables, disables, ticks;
        boolean throwOnTick;

        Plain(String id, Rule rule, boolean def) {
            super(id, id, "test", Category.UTILITY, rule, def);
        }

        @Override
        public void onEnable() {
            enables++;
        }

        @Override
        public void onDisable() {
            disables++;
        }

        @Override
        public void onTick() {
            ticks++;
            if (throwOnTick) throw new IllegalStateException("boom");
        }
    }

    static final class Hud extends HudModule {
        Hud(String id) {
            super(id, id, "test hud", Rule.ALLOWED, true, Anchor.TOP_RIGHT, 2, 3);
        }

        @Override
        public float width(Renderer r) {
            return 10;
        }

        @Override
        public float height(Renderer r) {
            return 5;
        }

        @Override
        public void render(Renderer r, HudStyle style, boolean preview) {}
    }
}
