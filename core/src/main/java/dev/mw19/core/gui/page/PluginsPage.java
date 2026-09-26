package dev.mw19.core.gui.page;

import dev.mw19.api.util.Colors;
import dev.mw19.core.Log;
import dev.mw19.core.gui.GuiRoot;
import dev.mw19.core.gui.Toasts;
import dev.mw19.core.gui.Ui;
import dev.mw19.core.gui.Widget;
import dev.mw19.core.gui.widget.Button;
import dev.mw19.core.gui.widget.ScrollList;
import dev.mw19.core.gui.widget.Toggle;
import dev.mw19.core.plugin.PanelRegistry;
import dev.mw19.core.plugin.PluginManager;

import java.util.List;

/**
 * Installed plugins: first-run consent, enable/disable, failures, and panels registered by plugins.
 * Consent is per jar (id + SHA-256): an updated jar asks again.
 */
public final class PluginsPage extends Page {
    private static final String WARNING = "Plugins run inside the game with the same privileges as MW19: they can read and write "
            + "files and use the network. Only allow plugins from people you trust.";
    private final ScrollList list = new ScrollList();
    private final Button folder, rescan;
    private final PanelPage panelPage;

    public PluginsPage(final GuiRoot root) {
        super(root);
        list.gap = 4;
        panelPage = new PanelPage(root, this);
        folder = new Button("Open folder", Button.Style.SECONDARY, new Runnable() {
            @Override
            public void run() {
                root.k.platform.openFolder(root.k.config.pluginsDir);
            }
        });
        rescan = new Button("Rescan", Button.Style.SECONDARY, new Runnable() {
            @Override
            public void run() {
                try {
                    root.k.plugins.shutdown();
                    root.k.plugins.loadAll();
                } catch (Throwable t) {
                    Log.error("plugin rescan failed", t);
                }
                onShow();
            }
        });
        rescan.tooltip = "Disable all plugins and load the folder again";
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
    public void onShow() {
        list.clear();
        for (PluginManager.Entry e : root.k.plugins.entries()) list.add(new Card(e));
        List<PanelRegistry.Item> panels = root.k.panels.all();
        if (!panels.isEmpty()) {
            Widget header = new Widget() {
                @Override
                public void render(Ui ui) {
                    ui.g.text("Plugin panels", x + 2, y + 5, ui.t.text, false);
                }
            };
            header.h = 16;
            list.add(header);
            for (final PanelRegistry.Item it : panels) {
                Button b = new Button(it.title, Button.Style.SECONDARY, new Runnable() {
                    @Override
                    public void run() {
                        panelPage.item = it;
                        root.show(panelPage);
                    }
                });
                b.h = 18;
                list.add(b);
            }
        }
    }

    @Override
    public void render(Ui ui) {
        heading(ui, "Plugins", "Jars in MW19/plugins (plugin API " + dev.mw19.api.Mw19Api.VERSION + ")");
        folder.bounds(x + w - 150, y, 80, 16).render(ui);
        rescan.bounds(x + w - 66, y, 66, 16).render(ui);
        list.bounds(x, y + 28, w, h - 28);
        if (list.children.isEmpty()) {
            ui.g.text("No plugins installed. Put plugin jars in the folder, then Rescan.", x, y + 34, ui.t.textDim, false);
        } else {
            list.render(ui);
        }
    }

    @Override
    public boolean mouseClicked(Ui ui, int button) {
        return folder.mouseClicked(ui, button) || rescan.mouseClicked(ui, button) || list.mouseClicked(ui, button);
    }

    @Override
    public boolean mouseScrolled(Ui ui, double amount) {
        return list.mouseScrolled(ui, amount);
    }

    @Override
    public void reveal(Widget w) {
        list.reveal(w);
    }

    private final class Card extends Widget {
        private final PluginManager.Entry e;
        private final Toggle toggle;
        private final Button allow;
        private List<String> warnLines, errLines;

        Card(final PluginManager.Entry e) {
            this.e = e;
            toggle = new Toggle(new Toggle.Model() {
                @Override
                public boolean get() {
                    return e.state == PluginManager.State.ENABLED;
                }

                @Override
                public void set(boolean v) {
                    root.k.plugins.setEnabled(e, v);
                }
            });
            allow = new Button("Allow and enable", Button.Style.PRIMARY, new Runnable() {
                @Override
                public void run() {
                    root.k.plugins.grantConsent(e);
                    onShow();
                }
            });
            h = height(); // estimate until the first render wraps the text
        }

        private float height() {
            float hh = 36;
            if (e.state == PluginManager.State.NEEDS_CONSENT) hh += (warnLines == null ? 3 : warnLines.size()) * 10 + 22;
            if (e.error != null) hh += (errLines == null ? 1 : errLines.size()) * 10 + 2;
            return hh;
        }

        @Override
        public void render(Ui ui) {
            if (warnLines == null) warnLines = Toasts.wrap(ui.g, WARNING, w - 20);
            if (errLines == null && e.error != null) errLines = Toasts.wrap(ui.g, e.error, w - 16);
            h = height();
            ui.g.roundRect(x, y, w, h, 5, ui.t.surface);
            String name = e.descriptor != null ? e.descriptor.name + "  " + e.descriptor.version : e.jar.getFileName().toString();
            ui.g.text(ui.g.ellipsize(name, w - 120), x + 8, y + 7, ui.t.text, false);
            String sub = e.descriptor == null ? e.jar.getFileName().toString()
                    : (e.descriptor.authors.isEmpty() ? "" : "by " + join(e.descriptor.authors) + " · ") + e.descriptor.description;
            ui.g.text(ui.g.ellipsize(sub, w - 16), x + 8, y + 19, ui.t.textDim, false);
            int color;
            String label;
            switch (e.state) {
                case ENABLED: color = ui.t.good; label = "ENABLED"; break;
                case NEEDS_CONSENT: color = ui.t.warn; label = "NEEDS APPROVAL"; break;
                case FAILED: color = ui.t.bad; label = "FAILED"; break;
                case INCOMPATIBLE: color = ui.t.bad; label = "INCOMPATIBLE"; break;
                default: color = ui.t.textDim; label = "DISABLED"; break;
            }
            float lw = ui.g.textWidth(label) * 0.75f + 6;
            ui.g.roundRect(x + w - lw - 36, y + 6, lw, 9, 2, Colors.fade(color, 0.2f));
            ui.g.text(label, x + w - lw - 33, y + 7.5f, 0.75f, color, false);
            if (e.state == PluginManager.State.ENABLED || e.state == PluginManager.State.DISABLED) {
                toggle.bounds(x + w - 30, y + 5, 24, 12).render(ui);
            }
            float yy = y + 32;
            if (e.error != null && errLines != null) {
                for (String l : errLines) {
                    ui.g.text(l, x + 8, yy, ui.t.bad, false);
                    yy += 10;
                }
                yy += 2;
            }
            if (e.state == PluginManager.State.NEEDS_CONSENT) {
                ui.g.roundRect(x + 6, yy - 2, w - 12, warnLines.size() * 10 + 4, 3, Colors.fade(ui.t.warn, 0.12f));
                for (String l : warnLines) {
                    ui.g.text(l, x + 10, yy, ui.t.warn, false);
                    yy += 10;
                }
                allow.bounds(x + 8, yy + 4, 110, 15).render(ui);
            }
        }

        @Override
        public boolean mouseClicked(Ui ui, int button) {
            if (e.state == PluginManager.State.NEEDS_CONSENT && allow.mouseClicked(ui, button)) return true;
            return (e.state == PluginManager.State.ENABLED || e.state == PluginManager.State.DISABLED) && toggle.mouseClicked(ui, button);
        }
    }

    static String join(List<String> l) {
        StringBuilder sb = new StringBuilder();
        for (String s : l) sb.append(sb.length() == 0 ? "" : ", ").append(s);
        return sb.toString();
    }

    /** Hosts one plugin panel (its render callback gets panel-local coordinates). */
    static final class PanelPage extends Page {
        PanelRegistry.Item item;
        private final PluginsPage owner;
        private final Button back;

        PanelPage(final GuiRoot root, final PluginsPage owner) {
            super(root);
            this.owner = owner;
            back = new Button("", Button.Style.GHOST, new Runnable() {
                @Override
                public void run() {
                    root.show(owner);
                }
            }).icon("back");
        }

        @Override
        public String title() {
            return item == null ? "Panel" : item.title;
        }

        @Override
        public String icon() {
            return "plugins";
        }

        @Override
        public void render(Ui ui) {
            back.bounds(x, y, 18, 16).render(ui);
            if (item == null) return;
            ui.g.text(item.title, x + 24, y + 1, 1.25f, ui.t.text, false);
            float py = y + 22, ph = h - 22;
            ui.g.pushClip(x, py, w, ph);
            ui.g.push();
            ui.g.translate(x, py);
            try {
                item.panel.render(ui.g, w, ph, ui.mx - x, ui.my - py);
            } catch (Throwable t) {
                Log.error("plugin panel " + item.title + " failed to render", t);
                item = null;
            } finally {
                ui.g.pop();
                ui.g.popClip();
            }
        }

        @Override
        public boolean mouseClicked(Ui ui, int button) {
            if (back.mouseClicked(ui, button)) return true;
            if (item == null) return false;
            try {
                return item.panel.mouseClicked(ui.mx - x, ui.my - (y + 22), button);
            } catch (Throwable t) {
                Log.error("plugin panel click failed", t);
                return true;
            }
        }

        @Override
        public boolean keyPressed(Ui ui, int key, int mods) {
            if (item == null) return false;
            try {
                return item.panel.keyPressed(key);
            } catch (Throwable t) {
                Log.error("plugin panel key failed", t);
                return true;
            }
        }
    }
}
