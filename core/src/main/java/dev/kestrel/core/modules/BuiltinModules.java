package dev.kestrel.core.modules;

import dev.kestrel.api.module.Module;
import dev.kestrel.core.Kestrel;

import java.util.ArrayList;
import java.util.List;

/** Every built-in module (order = default order in the Mods page). */
public final class BuiltinModules {
    private BuiltinModules() {}

    /** Fresh instances; also used by tests (RulesMatrixTest) without a running client. */
    public static List<Module> create(ClickTracker clicks) {
        List<Module> m = new ArrayList<Module>();
        m.add(new FpsModule());
        m.add(new KeystrokesModule(clicks));
        m.add(new ArmorModule());
        return m;
    }

    public static void registerAll(Kestrel k) {
        for (Module m : create(k.clicks)) k.register(m, "core");
    }
}
