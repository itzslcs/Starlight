package dev.starlight.core.gui.page;

import dev.starlight.api.setting.KeySetting;
import dev.starlight.api.setting.Setting;
import dev.starlight.core.Guard;
import dev.starlight.core.gui.GuiRoot;
import dev.starlight.core.gui.Ui;
import dev.starlight.core.gui.Widget;
import dev.starlight.core.gui.widget.Button;
import dev.starlight.core.gui.widget.KeybindButton;
import dev.starlight.core.gui.widget.ScrollList;
import dev.starlight.core.gui.widget.TextField;
import dev.starlight.core.module.ModuleManager;
import dev.starlight.core.platform.Platform;

/**
 * Bind profiles (saved sets of Minecraft's own key bindings, shared by every instance), then Minecraft's key bindings by
 * category (set here, they apply to the game at once), then Starlight's; conflicts (with ours or vanilla) are outlined.
 */
public final class KeybindsPage extends Page {
    private final ScrollList list = new ScrollList();
    /** Minecraft's bindings on this page, by id: read from the game, and setting one applies it to the game. */
    private final java.util.Map<String, KeySetting> vanilla = new java.util.LinkedHashMap<String, KeySetting>();
    private boolean syncing;
    private final Button resetVanilla = new Button("Reset all", Button.Style.SECONDARY, new Runnable() {
        @Override
        public void run() {
            java.util.Map<String, Integer> defaults = new java.util.LinkedHashMap<String, Integer>();
            for (KeySetting s : vanilla.values()) defaults.put(s.id(), s.defaultValue());
            int n = root.k.platform.applyVanillaBindings(defaults);
            syncVanilla();
            root.k.toast("Minecraft keys", "Back to Minecraft's defaults (" + n + (n == 1 ? " key" : " keys") + " changed).", root.k.theme.accent);
        }
    });

    public KeybindsPage(GuiRoot root) {
        super(root);
        list.gap = 2;
    }

    @Override
    public String title() {
        return "Keybinds";
    }

    @Override
    public String icon() {
        return "keys";
    }

    @Override
    public void onShow() {
        list.clear();
        vanilla.clear();
        if (root.k.binds != null) list.add(new ProfilesPanel());
        list.add(new Header("Minecraft", "kept in bind profiles", resetVanilla));
        final String[] category = {null};
        Guard.run("keybinds page", new Runnable() {
            @Override
            public void run() {
                root.k.platform.vanillaBindings(new Platform.BindingSink() {
                    @Override
                    public void accept(final String id, String name, String cat, int key, int def) {
                        if (!cat.equals(category[0])) list.add(new Header(category[0] = cat, null, null));
                        final KeySetting s = new KeySetting(id, name, "", def);
                        s.set(key);
                        s.addListener(new Runnable() {
                            @Override
                            public void run() {
                                if (syncing) return;
                                root.k.platform.applyVanillaBindings(java.util.Collections.singletonMap(id, s.key()));
                                root.refreshVanillaBinds();
                            }
                        });
                        vanilla.put(id, s);
                        list.add(new Row("", s));
                    }
                });
            }
        });
        list.add(new Header("Starlight", null, null));
        list.add(new Row("Starlight", root.k.client.openGui));
        for (ModuleManager.State s : root.k.modules.all()) {
            if ((s.suspend() & ModuleManager.SUSPEND_UNAVAILABLE) != 0) continue;
            for (Setting<?> set : s.module.settings()) {
                if (set instanceof KeySetting) list.add(new Row(s.module.name(), (KeySetting) set));
            }
        }
    }

    /** Smoke: the row for Minecraft's binding {@code id} (its setting applies to the game when changed), or null. */
    public KeySetting minecraftKey(String id) {
        return vanilla.get(id);
    }

    /** The rows show the game's keys again (after a profile was applied or the keys were reset). */
    private void syncVanilla() {
        syncing = true;
        try {
            Guard.run("keybinds page", new Runnable() {
                @Override
                public void run() {
                    root.k.platform.vanillaBindings(new Platform.BindingSink() {
                        @Override
                        public void accept(String id, String name, String cat, int key, int def) {
                            KeySetting s = vanilla.get(id);
                            if (s != null) s.set(key);
                        }
                    });
                }
            });
        } finally {
            syncing = false;
        }
        root.refreshVanillaBinds();
    }

    @Override
    public void render(Ui ui) {
        heading(ui, "Keybinds", "Click a key and press a new one. Amber = shared key.");
        list.bounds(x, y + 28, w, h - 28);
        list.render(ui);
    }

    @Override
    public boolean mouseClicked(Ui ui, int button) {
        return list.mouseClicked(ui, button);
    }

    @Override
    public boolean mouseScrolled(Ui ui, double amount) {
        return list.mouseScrolled(ui, amount);
    }

    @Override
    public void reveal(Widget w) {
        list.reveal(w);
    }

    /**
     * Bind profiles: chips for the saved profiles (the default for new instances has a star), a name field, and Save
     * current binds / Apply / Default / Delete. Profiles are sets of Minecraft's own key bindings (hotbar, movement...).
     */
    private final class ProfilesPanel extends Widget {
        private final TextField name = new TextField("");
        private final Button save, apply, makeDefault, delete;
        private final Button[] actions;
        private final java.util.List<Button> chips = new java.util.ArrayList<Button>();
        private String selected, status = "";
        private final String where = root.k.binds.file().startsWith(root.k.config.root) ? "This instance only" : "Shared by your instances";

        ProfilesPanel() {
            name.placeholder = "Name for a new profile";
            name.maxLength = 32;
            save = new Button("Save current binds", Button.Style.PRIMARY, new Runnable() {
                @Override
                public void run() {
                    String n = name.text.trim().isEmpty() ? root.k.binds.freeName() : name.text.trim();
                    root.k.binds.put(n, root.k.platform.vanillaBindingMap());
                    selected = n;
                    name.text = "";
                    status = "Saved your current Minecraft binds as " + n + ".";
                    rebuild();
                }
            });
            apply = new Button("Apply", Button.Style.SECONDARY, new Runnable() {
                @Override
                public void run() {
                    java.util.Map<String, Integer> b = selected == null ? null : root.k.binds.get(selected);
                    if (b == null) return;
                    int n = root.k.platform.applyVanillaBindings(b);
                    syncVanilla();
                    status = "Applied " + selected + ": " + n + (n == 1 ? " Minecraft key" : " Minecraft keys") + " changed.";
                }
            });
            makeDefault = new Button("Set default", Button.Style.SECONDARY, new Runnable() {
                @Override
                public void run() {
                    if (selected == null) return;
                    boolean was = selected.equals(root.k.binds.defaultName());
                    root.k.binds.setDefault(was ? null : selected);
                    status = was ? "New instances keep Minecraft's own binds." : "New instances start with " + selected + ".";
                    rebuild();
                }
            });
            makeDefault.tooltip = "The profile a new instance applies on its first start";
            delete = new Button("Delete", Button.Style.DANGER, new Runnable() {
                @Override
                public void run() {
                    if (selected == null) return;
                    root.k.binds.delete(selected);
                    status = "Deleted " + selected + ".";
                    selected = null;
                    rebuild();
                }
            });
            actions = new Button[]{apply, makeDefault, delete};
            java.util.List<String> names = root.k.binds.names();
            selected = root.k.binds.defaultName() != null ? root.k.binds.defaultName() : names.isEmpty() ? null : names.get(0);
            rebuild();
        }

        private void rebuild() {
            chips.clear();
            for (final String n : root.k.binds.names()) {
                Button c = new Button(n + (n.equals(root.k.binds.defaultName()) ? " \u2605" : ""), Button.Style.SECONDARY, new Runnable() {
                    @Override
                    public void run() {
                        selected = n;
                    }
                });
                chips.add(c);
            }
        }

        @Override
        public void render(Ui ui) {
            float yy = y + 4;
            ui.g.text("Bind profiles", x + 6, yy, ui.t.text, false);
            ui.g.text(where, x + w - 6 - ui.g.textWidth(where), yy, ui.t.textDim, false);
            yy += 14;
            float cx = x + 6;
            if (chips.isEmpty()) {
                ui.g.text("None yet: set your Minecraft keys below, then save them as a profile.", cx, yy + 4, ui.t.textDim, false);
                yy += 18;
            } else {
                java.util.List<String> names = root.k.binds.names();
                for (int i = 0; i < chips.size(); i++) {
                    Button c = chips.get(i);
                    float cw = ui.g.textWidth(c.label) + 14;
                    if (cx + cw > x + w - 6 && cx > x + 6) {
                        cx = x + 6;
                        yy += 20;
                    }
                    c.style = i < names.size() && names.get(i).equals(selected) ? Button.Style.PRIMARY : Button.Style.SECONDARY;
                    c.bounds(cx, yy, cw, 16).render(ui);
                    cx += cw + 4;
                }
                yy += 20;
            }
            // [name .......][Save current binds]   then   [Apply][Set default][Delete] for the selected profile
            float bw1 = ui.g.textWidth(save.label) + 14, right = x + w - 6;
            name.bounds(x + 6, yy, Math.max(60, right - (x + 6) - bw1 - 4), 16).render(ui);
            save.bounds(right - bw1, yy, bw1, 16).render(ui);
            yy += 20;
            apply.enabled = makeDefault.enabled = delete.enabled = selected != null;
            float bx = x + 6;
            for (Button b : actions) {
                float bw = ui.g.textWidth(b.label) + 14;
                b.bounds(bx, yy, bw, 16).render(ui);
                bx += bw + 4;
            }
            yy += 20;
            if (!status.isEmpty()) {
                ui.g.text(ui.g.ellipsize(status, w - 12), x + 6, yy, ui.t.textDim, false);
                yy += 12;
            }
            h = yy - y + 4;
        }

        @Override
        public boolean mouseClicked(Ui ui, int button) {
            for (Button c : chips) if (c.mouseClicked(ui, button)) return true;
            return name.mouseClicked(ui, button) || save.mouseClicked(ui, button) || apply.mouseClicked(ui, button)
                    || makeDefault.mouseClicked(ui, button) || delete.mouseClicked(ui, button);
        }
    }

    private static final class Row extends Widget {
        private final String owner;
        private final KeySetting setting;
        private final KeybindButton button;

        Row(String owner, KeySetting setting) {
            this.owner = owner;
            this.setting = setting;
            this.button = new KeybindButton(setting);
            this.h = 20;
        }

        @Override
        public void render(Ui ui) {
            ui.g.roundRect(x, y, w, h, 3, ui.t.surface);
            float nx = x + 7;
            if (!owner.isEmpty()) {
                ui.g.text(owner, nx, y + 6, ui.t.textDim, false);
                nx += ui.g.textWidth(owner) + 6;
            }
            ui.g.text(setting.name(), nx, y + 6, ui.t.text, false);
            button.bounds(x + w - 96, y + 2, 92, 16).render(ui);
        }

        @Override
        public boolean mouseClicked(Ui ui, int b) {
            return button.contains(ui.mx, ui.my) && button.mouseClicked(ui, b);
        }
    }

    /** A section title (with a note and a button) or, without them, a category label. */
    private static final class Header extends Widget {
        private final String title, note;
        private final Button action;

        Header(String title, String note, Button action) {
            this.title = title;
            this.note = note;
            this.action = action;
            this.h = action != null || note != null ? 20 : 14;
        }

        @Override
        public void render(Ui ui) {
            boolean major = h == 20;
            float ty = y + h - 11;
            ui.g.text(title, x + 2, ty, major ? ui.t.accent : ui.t.textDim, false);
            float bw = action == null ? 0 : ui.g.textWidth(action.label) + 14, nx = x + 2 + ui.g.textWidth(title) + 6;
            if (note != null && nx + ui.g.textWidth(note) + 8 <= x + w - bw) ui.g.text(note, nx, ty, ui.t.textDim, false);
            if (action != null) action.bounds(x + w - bw, y + 3, bw, 15).render(ui);
        }

        @Override
        public boolean mouseClicked(Ui ui, int button) {
            return action != null && action.mouseClicked(ui, button);
        }
    }
}
