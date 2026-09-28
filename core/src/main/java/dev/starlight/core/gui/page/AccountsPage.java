package dev.starlight.core.gui.page;

import dev.starlight.core.account.Accounts;
import dev.starlight.core.gui.GuiRoot;
import dev.starlight.core.gui.Ui;
import dev.starlight.core.gui.Widget;
import dev.starlight.core.gui.widget.Button;
import dev.starlight.core.gui.widget.ScrollList;

import java.nio.file.Path;
import java.util.List;

/**
 * Accounts (DECISIONS D-034): switching between the accounts Prism Launcher signed in. The owner puts his own
 * {@code accounts.json} in the Starlight folder (or drops it on this page); Starlight only reads it, and signs nobody
 * in. A switch applies to the next server join, not the world already joined.
 */
public final class AccountsPage extends Page {
    private final ScrollList list = new ScrollList();
    private final Button reload, folder;

    public AccountsPage(final GuiRoot root) {
        super(root);
        list.gap = 2;
        reload = new Button("Reload file", Button.Style.SECONDARY, new Runnable() {
            @Override
            public void run() {
                rebuild();
            }
        });
        folder = new Button("Open folder", Button.Style.SECONDARY, new Runnable() {
            @Override
            public void run() {
                root.k.platform.openFolder(root.k.accounts.file().getParent());
            }
        });
        folder.tooltip = "Put Prism's accounts.json here (or drop it on this page)";
    }

    @Override
    public String title() {
        return "Accounts";
    }

    @Override
    public String icon() {
        return "accounts";
    }

    @Override
    public void onShow() {
        rebuild();
    }

    private void rebuild() {
        root.k.accounts.reload();
        list.clear();
        for (Accounts.Entry e : root.k.accounts.all()) list.add(new Row(e));
    }

    @Override
    public void filesDropped(List<Path> files) {
        for (Path p : files) {
            if (!"accounts.json".equals(String.valueOf(p.getFileName()))) continue;
            try {
                root.k.accounts.importFrom(p);
                list.clear();
                for (Accounts.Entry e : root.k.accounts.all()) list.add(new Row(e));
                root.k.toast("Accounts", root.k.accounts.all().size() + " account(s) read from the dropped file", root.k.theme.good);
            } catch (Exception e) {
                root.k.toast("Accounts", "Could not copy that file in (" + e.getMessage() + ")", root.k.theme.bad);
            }
            return;
        }
        root.k.toast("Accounts", "Drop Prism's accounts.json file", root.k.theme.bad);
    }

    @Override
    public void render(Ui ui) {
        heading(ui, "Accounts", "Signed in as " + root.k.platform.playerName() + " · from Prism's accounts.json");
        float ry = y + 26;
        reload.bounds(x, ry, 84, 16).render(ui);
        folder.bounds(x + 88, ry, 88, 16).render(ui);
        String err = root.k.accounts.error();
        if (!err.isEmpty()) ui.g.text(ui.g.ellipsize(err, w), x, ry + 24, ui.t.textDim, false);
        list.bounds(x, ry + 42, w, h - (ry + 42 - y));
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

    @Override
    public void reveal(Widget w) {
        list.reveal(w);
    }

    private final class Row extends Widget {
        private final Accounts.Entry e;
        private final Button use;

        Row(final Accounts.Entry e) {
            this.e = e;
            this.h = 20;
            use = new Button("Use", Button.Style.PRIMARY, new Runnable() {
                @Override
                public void run() {
                    if (!root.k.platform.setSession(e.name, e.id, e.token)) {
                        root.k.toast("Accounts", "This version cannot switch accounts", root.k.theme.bad);
                        return;
                    }
                    root.k.toast("Accounts", "Now " + e.name + (root.k.platform.inWorld() ? " · from the next server join" : ""), root.k.theme.accent);
                }
            });
        }

        @Override
        public void render(Ui ui) {
            boolean active = e.id.equals(root.k.platform.playerUuid());
            ui.g.roundRect(x, y, w, h, 4, active ? ui.t.accentSoft(0.15f) : ui.t.surface);
            ui.g.text(e.name, x + 7, y + 6, ui.t.text, false);
            String tag = active ? "active" : e.offline ? "offline" : e.expired() ? "token expired" : "";
            if (!tag.isEmpty()) {
                ui.g.text(tag, x + 12 + ui.g.textWidth(e.name), y + 6, active ? ui.t.accent : ui.t.textDim, false);
            }
            if (!active) use.bounds(x + w - 36, y + 2, 32, 16).render(ui);
        }

        @Override
        public boolean mouseClicked(Ui ui, int button) {
            return !e.id.equals(root.k.platform.playerUuid()) && use.mouseClicked(ui, button);
        }
    }
}
