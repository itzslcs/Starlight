package dev.kestrel.core.gui.page;

import dev.kestrel.core.gui.GuiRoot;
import dev.kestrel.core.gui.Ui;
import dev.kestrel.core.gui.widget.Button;
import dev.kestrel.core.gui.widget.TextField;
import dev.kestrel.core.modules.ChatModule;
import dev.kestrel.core.module.ModuleManager;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

/** Searches the in-memory chat history kept by Chat Tools (newest first). Not shown in the sidebar. */
public final class ChatSearchPage extends Page {
    private final TextField query = new TextField("");
    private final Button back;
    private List<String[]> results = Collections.emptyList();
    private String lastQuery;
    private float scroll;

    public ChatSearchPage(final GuiRoot root) {
        super(root);
        query.placeholder = "Search chat…";
        back = new Button("", Button.Style.GHOST, new Runnable() {
            @Override
            public void run() {
                root.show(root.pages().get(0));
            }
        }).icon("back");
    }

    @Override
    public String title() {
        return "Chat Search";
    }

    @Override
    public String icon() {
        return "search";
    }

    @Override
    public void onShow() {
        lastQuery = null;
        scroll = 0;
        root.focus(query);
    }

    private ChatModule chat() {
        ModuleManager.State s = root.k.modules.get("chat");
        return s != null && s.active() ? (ChatModule) s.module : null;
    }

    @Override
    public void render(Ui ui) {
        back.bounds(x, y, 18, 16).render(ui);
        query.bounds(x + 22, y, w - 22, 16).render(ui);
        ChatModule c = chat();
        if (c == null) {
            ui.g.text("Turn on Chat Tools to keep a searchable history.", x, y + 26, ui.t.textDim, false);
            return;
        }
        if (!query.text.equals(lastQuery)) {
            lastQuery = query.text;
            results = c.search(query.text, 200);
            scroll = 0;
        }
        ui.g.text(results.size() + " line(s)", x, y + 22, ui.t.textDim, false);
        ui.g.pushClip(x, y + 34, w, h - 34);
        float ly = y + 34 - scroll;
        String q = query.text.toLowerCase(Locale.ROOT);
        for (String[] r : results) {
            if (ly > y + h) break;
            if (ly > y + 20) {
                String t = ui.g.ellipsize(r[0], w - 8);
                ui.g.text(t, x + 4, ly, !q.isEmpty() && r[0].toLowerCase(Locale.ROOT).contains(q) ? ui.t.text : ui.t.textDim, false);
            }
            ly += 10;
        }
        ui.g.popClip();
    }

    @Override
    public boolean mouseClicked(Ui ui, int button) {
        return back.mouseClicked(ui, button) || query.mouseClicked(ui, button);
    }

    @Override
    public boolean mouseScrolled(Ui ui, double amount) {
        scroll = Math.max(0, Math.min(Math.max(0, results.size() * 10 - (h - 34)), scroll - (float) amount * 20));
        return true;
    }
}
