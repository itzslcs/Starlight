package dev.mw19.core.gui.page;

import dev.mw19.core.gui.GuiRoot;
import dev.mw19.core.gui.Ui;
import dev.mw19.core.gui.widget.Button;

import java.util.Map;

/** Version, target, compatibility flags, hook health and the privacy statement. */
public final class AboutPage extends Page {
    private final Button folder;

    public AboutPage(final GuiRoot root) {
        super(root);
        folder = new Button("Open MW19 folder", Button.Style.SECONDARY, new Runnable() {
            @Override
            public void run() {
                root.k.platform.openFolder(root.k.config.root);
            }
        });
    }

    @Override
    public String title() {
        return "About";
    }

    @Override
    public String icon() {
        return "about";
    }

    @Override
    public void render(Ui ui) {
        heading(ui, "MW19 " + root.k.modVersion, "Minecraft " + root.k.platform.minecraftVersion() + " · " + root.k.platform.loader());
        float ty = y + 30;
        String missing = root.k.hooks.missing(root.k.platform.inWorld());
        ui.g.text("Hooks: " + (missing.isEmpty() ? "all firing" : "not firing yet: " + missing), x, ty, missing.isEmpty() ? ui.t.good : ui.t.warn, false);
        ty += 14;
        ui.g.text("Detected mods (MW19 adapts around them):", x, ty, ui.t.text, false);
        ty += 11;
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Boolean> e : root.k.compat.all().entrySet()) if (e.getValue()) sb.append(sb.length() == 0 ? "" : ", ").append(e.getKey());
        for (String line : dev.mw19.core.gui.Toasts.wrap(ui.g, sb.length() == 0 ? "none" : sb.toString(), w)) {
            ui.g.text(line, x + 4, ty, ui.t.textDim, false);
            ty += 10;
        }
        ty += 6;
        ui.g.text("Privacy", x, ty, ui.t.text, false);
        ty += 11;
        String privacy = "No telemetry, accounts or analytics. MW19 only uses the network for what you ask it to (pack search,"
                + " skins, hosting); your session token only ever goes to Mojang's skin service.";
        for (String line : dev.mw19.core.gui.Toasts.wrap(ui.g, privacy, w)) {
            ui.g.text(line, x + 4, ty, ui.t.textDim, false);
            ty += 10;
        }
        ty += 6;
        ui.g.text("Original work · MIT licence · not affiliated with Mojang, Hypixel, Feather/Dawn or Lunar.", x, ty, ui.t.textDim, false);
        String bundled = root.k.platform.bundledNotice(); // LGPL-3.0 section 4(c): the library's notice among ours
        if (!bundled.isEmpty()) {
            for (String line : dev.mw19.core.gui.Toasts.wrap(ui.g, bundled, w)) {
                ty += 10;
                ui.g.text(line, x, ty, ui.t.textDim, false);
            }
        }
        folder.bounds(x, y + h - 18, 110, 16).render(ui);
    }

    @Override
    public boolean mouseClicked(Ui ui, int button) {
        return folder.mouseClicked(ui, button);
    }
}
