package dev.mw19.core.gui.page;

import dev.mw19.core.gui.GuiRoot;
import dev.mw19.core.gui.Ui;
import dev.mw19.core.gui.widget.Button;

import java.util.Map;

/** Version, target, compatibility flags, hook health and the privacy statement. */
public final class AboutPage extends Page {
    private final Button folder;
    private float scroll, contentH;

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
        // The text scrolls above the folder button: the credits and the VulkanMod notice outgrow small windows.
        float top = y + 30, view = h - 52;
        ui.g.pushClip(x, top, w, view);
        float ty = top - scroll;
        String missing = root.k.hooks.missing(root.k.platform.inWorld());
        ui.g.text("Hooks: " + (missing.isEmpty() ? "all firing" : "not firing yet: " + missing), x, ty, missing.isEmpty() ? ui.t.good : ui.t.warn, false);
        ty += 14;
        ui.g.text("Detected mods (MW19 adapts around them):", x, ty, ui.t.text, false);
        ty += 11;
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Boolean> e : root.k.compat.all().entrySet()) if (e.getValue()) sb.append(sb.length() == 0 ? "" : ", ").append(e.getKey());
        ty = paragraph(ui, sb.length() == 0 ? "none" : sb.toString(), x + 4, ty, ui.t.textDim) + 6;
        ui.g.text("Privacy", x, ty, ui.t.text, false);
        ty += 11;
        ty = paragraph(ui, "No telemetry, accounts or analytics. MW19 only uses the network for what you ask it to (pack search,"
                + " skins, hosting); your session token only ever goes to Mojang's skin service.", x + 4, ty, ui.t.textDim) + 6;
        ty = paragraph(ui, "MIT licence · not affiliated with Mojang, Hypixel, Feather/Dawn or Lunar.", x, ty, ui.t.textDim);
        // Built-in ports' credits (MIT) and LGPL-3.0 section 4(c): the library's notice among ours
        String bundled = root.k.platform.bundledNotice();
        if (!bundled.isEmpty()) ty = paragraph(ui, bundled, x, ty, ui.t.textDim);
        ui.g.popClip();
        contentH = ty + scroll - top;
        scroll = Math.max(0, Math.min(scroll, contentH - view));
        if (scroll < contentH - view - 1) { // more below: the credits and licence notices must not go unseen
            String more = "Scroll for more";
            ui.g.text(more, x + w - ui.g.textWidth(more), y + h - 13, ui.t.textDim, false);
        }
        folder.bounds(x, y + h - 18, 110, 16).render(ui);
    }

    /** Wrapped lines from {@code ty}; returns the y below them. */
    private float paragraph(Ui ui, String text, float px, float ty, int color) {
        for (String line : dev.mw19.core.gui.Toasts.wrap(ui.g, text, w - (px - x))) {
            ui.g.text(line, px, ty, color, false);
            ty += 10;
        }
        return ty;
    }

    @Override
    public boolean mouseScrolled(Ui ui, double amount) {
        scroll = Math.max(0, Math.min(contentH - (h - 52), scroll - (float) amount * 20));
        return true;
    }

    @Override
    public boolean mouseClicked(Ui ui, int button) {
        return folder.mouseClicked(ui, button);
    }
}
