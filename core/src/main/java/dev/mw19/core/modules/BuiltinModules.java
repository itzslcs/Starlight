package dev.mw19.core.modules;

import dev.mw19.api.module.Module;
import dev.mw19.core.Mw19;

import java.util.ArrayList;
import java.util.List;

/** Every built-in module (order = default order in the Mods page). */
public final class BuiltinModules {
    private BuiltinModules() {}

    /** Fresh instances; also used by tests (RulesMatrixTest) without a running client. */
    public static List<Module> create(InputRates rates) {
        List<Module> m = new ArrayList<Module>();
        // HUD
        m.add(new FpsModule());
        m.add(new KeyCpsModule(rates));
        m.add(new SpeedModule());
        m.add(new PingModule());
        m.add(new CoordsModule());
        m.add(new DirectionModule());
        m.add(new ArmorModule());
        m.add(new EffectsModule());
        m.add(new ItemCounterModule());
        m.add(new ClockModule());
        m.add(new SystemModule());
        m.add(new FpsGraphModule());
        m.add(new ServerAddressModule());
        m.add(new ComboModule());
        // visual / utility
        m.add(new CrosshairModule());
        m.add(new ZoomModule());
        m.add(new ToggleModule("toggle_sprint", "Toggle Sprint", dev.mw19.api.game.Game.Binding.SPRINT, "Sprinting", 40));
        m.add(new ToggleModule("toggle_sneak", "Toggle Sneak", dev.mw19.api.game.Game.Binding.SNEAK, "Sneaking", 28));
        m.add(new SimpleVisuals.Fullbright());
        m.add(new SimpleVisuals.HitColor());
        m.add(new SimpleVisuals.DamageTilt());
        m.add(new SimpleVisuals.LowFire());
        m.add(new SimpleVisuals.ShieldOverlay());
        m.add(new SimpleVisuals.Particles());
        m.add(new ChatModule());
        m.add(new EntityCullingModule());
        m.add(new FastChestsModule());
        m.add(new ClearWeatherModule());
        m.add(new ExploitProtectionModule());
        m.add(new MoreHud.Saturation());
        m.add(new MoreHud.SessionTime());
        m.add(new MoreHud.PackDisplay());
        m.add(new MoreHud.Stopwatch());
        m.add(new MoreHud.DayCounter());
        m.add(new MoreHud.LowHealth());
        m.add(new MoreHud.Hitboxes());
        m.add(new MoreHud.TntTimer());
        m.add(new MoreHud.Reach());
        m.add(new MoreHud.QuickCommands());
        m.add(new DurabilityAlertModule());
        m.add(new ScreenshotModule());
        m.add(new FreelookModule());
        m.add(new SimpleVisuals.OwnNametag());
        m.add(new SimpleVisuals.OldVisuals());
        return m;
    }

    public static void registerAll(Mw19 k) {
        for (Module m : create(k.rates)) k.register(m, "core");
    }
}
