package dev.kestrel.core.gui.page;

import dev.kestrel.core.gui.GuiRoot;
import dev.kestrel.core.gui.Ui;
import dev.kestrel.core.gui.widget.Button;

/** Plugins (the loader arrives in Phase 4; this page already lets users find the folder). */
public final class PluginsPage extends Page {
    private final Button folder;

    public PluginsPage(final GuiRoot root) {
        super(root);
        folder = new Button("Open plugins folder", Button.Style.SECONDARY, new Runnable() {
            @Override
            public void run() {
                root.k.platform.openFolder(root.k.config.pluginsDir);
            }
        });
    }

    @Override
    public String title() {
        return "Plugins";
    }

    @Override
    public String icon() {
        return "plugins";
    }

    @Override
    public void render(Ui ui) {
        heading(ui, "Plugins", "Jars in Kestrel/plugins run with full mod privileges");
        ui.g.text("No plugins loaded.", x, y + 30, ui.t.textDim, false);
        folder.bounds(x, y + 44, 120, 16).render(ui);
    }

    @Override
    public boolean mouseClicked(Ui ui, int button) {
        return folder.mouseClicked(ui, button);
    }
}
