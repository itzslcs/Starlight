package dev.starlight.core.modules;

import dev.starlight.api.module.Category;
import dev.starlight.api.module.Module;
import dev.starlight.api.module.Rule;
import dev.starlight.api.setting.BoolSetting;
import dev.starlight.api.setting.ColorSetting;
import dev.starlight.api.setting.NumberSetting;
import dev.starlight.core.Hooks;

/** Small visual tweaks whose whole effect is one Hooks value read by the platform mixins. */
public final class SimpleVisuals {
    private SimpleVisuals() {}

    public static final class Fullbright extends Module {
        private final NumberSetting level = add(new NumberSetting("level", "Brightness", "Gamma to apply (vanilla max is 100%)", 1000, 100, 1500, 50, "%"));

        public Fullbright() {
            super("fullbright", "Brightness", "See in the dark (Hypixel's \"Brightness & Gamma\" category)", Category.VISUAL, Rule.ALLOWED, false);
            level.addListener(new Runnable() {
                @Override
                public void run() {
                    if (!Double.isNaN(Hooks.gamma)) Hooks.gamma = level.get() / 100.0;
                }
            });
        }

        @Override
        public void onEnable() {
            Hooks.gamma = level.get() / 100.0;
        }

        @Override
        public void onDisable() {
            Hooks.gamma = Double.NaN;
        }
    }

    public static final class HitColor extends Module {
        private final ColorSetting color = add(new ColorSetting("color", "Hit colour", "Tint of entities that were just hurt", 0x6600AAFF));

        public HitColor() {
            super("hit_color", "Hit Color", "Change the red flash on hurt entities", Category.VISUAL, Rule.ALLOWED, false);
            color.addListener(new Runnable() {
                @Override
                public void run() {
                    if (Hooks.hitColor != 0) Hooks.hitColor = nonZero(color.get());
                }
            });
        }

        private static int nonZero(int c) {
            return c == 0 ? 0x01000000 : c;
        }

        @Override
        public void onEnable() {
            Hooks.hitColor = nonZero(color.get());
        }

        @Override
        public void onDisable() {
            Hooks.hitColor = 0;
        }
    }

    public static final class DamageTilt extends Module {
        private final NumberSetting strength = add(new NumberSetting("strength", "Tilt strength", "Camera shake when you take damage", 50, 0, 100, 5, "%"));

        public DamageTilt() {
            super("damage_tilt", "Damage Tilt", "Reduce or remove the camera shake on damage", Category.VISUAL, Rule.ALLOWED, false);
            strength.addListener(new Runnable() {
                @Override
                public void run() {
                    apply();
                }
            });
        }

        private boolean on;

        private void apply() {
            Hooks.damageTilt = on ? strength.floatValue() / 100f : 1f;
        }

        @Override
        public void onEnable() {
            on = true;
            apply();
        }

        @Override
        public void onDisable() {
            on = false;
            apply();
        }
    }

    public static final class LowFire extends Module {
        private final NumberSetting offset = add(new NumberSetting("offset", "Lower by", "How far the fire overlay moves down", 0.3, 0, 0.6, 0.05));
        private final NumberSetting opacity = add(new NumberSetting("opacity", "Opacity", "Fire overlay opacity", 60, 10, 100, 5, "%"));
        private boolean on;

        public LowFire() {
            super("fire_overlay", "Low Fire", "Lower and fade the fire overlay while burning", Category.VISUAL, Rule.ALLOWED, false);
            Runnable r = new Runnable() {
                @Override
                public void run() {
                    apply();
                }
            };
            offset.addListener(r);
            opacity.addListener(r);
        }

        private void apply() {
            Hooks.fireOffset = on ? offset.floatValue() : 0f;
            Hooks.fireOpacity = on ? opacity.floatValue() / 100f : 1f;
        }

        @Override
        public void onEnable() {
            on = true;
            apply();
        }

        @Override
        public void onDisable() {
            on = false;
            apply();
        }
    }

    public static final class ShieldOverlay extends Module {
        private final NumberSetting offset = add(new NumberSetting("offset", "Lower by", "How far a raised shield moves down", 0.25, 0, 0.6, 0.05));
        private boolean on;

        public ShieldOverlay() {
            super("shield_overlay", "Shield Overlay", "Lower a raised shield so it covers less of the screen", Category.VISUAL, Rule.ALLOWED, false);
            offset.addListener(new Runnable() {
                @Override
                public void run() {
                    Hooks.shieldOffset = on ? offset.floatValue() : 0f;
                }
            });
        }

        @Override
        public boolean available() {
            return dev.starlight.core.Starlight.get() == null || dev.starlight.core.Starlight.get().platform.supports("shield");
        }

        @Override
        public void onEnable() {
            on = true;
            Hooks.shieldOffset = offset.floatValue();
        }

        @Override
        public void onDisable() {
            on = false;
            Hooks.shieldOffset = 0f;
        }
    }

    public static final class Particles extends Module {
        private final NumberSetting multiplier = add(new NumberSetting("multiplier", "Multiplier", "Copies of crit / sharpness particles per hit", 3, 1, 10, 1, "x"));
        private boolean on;

        public Particles() {
            super("particles", "Particle Multiplier", "More crit and sharpness particles on hits (client-side)", Category.VISUAL, Rule.ALLOWED, false);
            multiplier.addListener(new Runnable() {
                @Override
                public void run() {
                    Hooks.extraHitParticles = on ? multiplier.intValue() - 1 : 0;
                }
            });
        }

        @Override
        public void onEnable() {
            on = true;
            Hooks.extraHitParticles = multiplier.intValue() - 1;
        }

        @Override
        public void onDisable() {
            on = false;
            Hooks.extraHitParticles = 0;
        }
    }

    public static final class OwnNametag extends Module {
        public OwnNametag() {
            super("own_nametag", "Show Own Nametag", "Show your own name above your head in third person", Category.VISUAL, Rule.ALLOWED, false);
        }

        @Override
        public void onEnable() {
            Hooks.ownNametag = true;
        }

        @Override
        public void onDisable() {
            Hooks.ownNametag = false;
        }
    }

    /** 1.21+ only: keeps the held item steady after attacking, like 1.8 (animation only; attacks are unchanged). */
    public static final class OldVisuals extends Module {
        private final BoolSetting noDip = add(new BoolSetting("no_dip", "No cooldown dip", "Held item stays up after attacking", true));
        private boolean on;

        public OldVisuals() {
            super("old_animations", "1.8 Visuals", "1.8-style held-item animation (visual only)", Category.VISUAL, Rule.ALLOWED, false);
            noDip.addListener(new Runnable() {
                @Override
                public void run() {
                    Hooks.noCooldownDip = on && noDip.on();
                }
            });
        }

        @Override
        public boolean available() {
            return dev.starlight.core.Starlight.get() == null || !"1.8.9".equals(dev.starlight.core.Starlight.get().platform.minecraftVersion());
        }

        @Override
        public void onEnable() {
            on = true;
            Hooks.noCooldownDip = noDip.on();
        }

        @Override
        public void onDisable() {
            on = false;
            Hooks.noCooldownDip = false;
        }
    }
}
