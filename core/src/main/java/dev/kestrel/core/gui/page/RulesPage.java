package dev.kestrel.core.gui.page;

import dev.kestrel.api.module.Rule;
import dev.kestrel.core.gui.GuiRoot;
import dev.kestrel.core.gui.Ui;
import dev.kestrel.core.gui.Widget;
import dev.kestrel.core.gui.widget.Button;
import dev.kestrel.core.gui.widget.ScrollList;
import dev.kestrel.core.gui.widget.SettingRow;
import dev.kestrel.core.module.ModuleManager;
import dev.kestrel.core.rules.ServerRules;

/** Current server, competitive-safe, and the data-driven serverrules.json. */
public final class RulesPage extends Page {
    private final ScrollList list = new ScrollList();
    private final Button reload, folder;

    public RulesPage(final GuiRoot root) {
        super(root);
        list.gap = 2;
        reload = new Button("Reload rules", Button.Style.SECONDARY, new Runnable() {
            @Override
            public void run() {
                root.k.loadServerRules();
                onShow();
                root.k.toast("Server rules", root.k.serverRules.rules().size() + " rule set(s) loaded", root.k.theme.good);
            }
        });
        reload.tooltip = "Re-read Kestrel/serverrules.json (merged over the built-in rules)";
        folder = new Button("Open folder", Button.Style.SECONDARY, new Runnable() {
            @Override
            public void run() {
                root.k.platform.openFolder(root.k.config.root);
            }
        });
    }

    @Override
    public String title() {
        return "Server Rules";
    }

    @Override
    public String icon() {
        return "rules";
    }

    @Override
    public void onShow() {
        list.clear();
        list.add(new SettingRow(root.k.client.competitiveSafe));
        for (final ServerRules.Rule r : root.k.serverRules.rules()) {
            Widget row = new Widget() {
                @Override
                public void render(Ui ui) {
                    ui.g.roundRect(x, y, w, h, 3, ui.t.surface);
                    ui.g.text(r.name, x + 7, y + 5, ui.t.text, false);
                    ui.g.text(ui.g.ellipsize("Servers: " + join(r.match), w - 14), x + 7, y + 16, ui.t.textDim, false);
                    ui.g.text(ui.g.ellipsize("Blocks: " + (r.disallow.isEmpty() ? "nothing" : join(r.disallow)), w - 14), x + 7, y + 26, ui.t.textDim, false);
                    if (ui.hover(x, y, w, h) && !r.note.isEmpty()) ui.tooltip = r.note;
                }
            };
            row.h = 38;
            list.add(row);
        }
    }

    static String join(Iterable<String> it) {
        StringBuilder sb = new StringBuilder();
        for (String s : it) sb.append(sb.length() == 0 ? "" : ", ").append(s);
        return sb.toString();
    }

    @Override
    public void render(Ui ui) {
        String server = root.k.currentServer;
        heading(ui, "Server Rules", server == null ? "Not on a server" : "Connected to " + server);
        StringBuilder blocked = new StringBuilder(), gray = new StringBuilder();
        for (ModuleManager.State s : root.k.modules.all()) {
            if ((s.suspend() & ModuleManager.SUSPEND_SERVER) != 0) blocked.append(blocked.length() == 0 ? "" : ", ").append(s.module.name());
            if (s.module.rule() != Rule.ALLOWED && s.enabled()) gray.append(gray.length() == 0 ? "" : ", ").append(s.module.name());
        }
        ui.g.text(ui.g.ellipsize("Blocked here: " + (blocked.length() == 0 ? "none" : blocked), w), x, y + 26, blocked.length() == 0 ? ui.t.textDim : ui.t.warn, false);
        ui.g.text(ui.g.ellipsize("Enabled GRAY/restricted modules: " + (gray.length() == 0 ? "none" : gray), w), x, y + 37, ui.t.textDim, false);
        reload.bounds(x + w - 170, y, 80, 16).render(ui);
        folder.bounds(x + w - 86, y, 86, 16).render(ui);
        list.bounds(x, y + 50, w, h - 50);
        list.render(ui);
    }

    @Override
    public boolean mouseClicked(Ui ui, int button) {
        return reload.mouseClicked(ui, button) || folder.mouseClicked(ui, button) || list.mouseClicked(ui, button);
    }

    @Override
    public boolean mouseScrolled(Ui ui, double amount) {
        return list.mouseScrolled(ui, amount);
    }
}
