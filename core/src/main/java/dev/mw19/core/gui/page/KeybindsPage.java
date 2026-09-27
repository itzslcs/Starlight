package dev.mw19.core.gui.page;

import dev.mw19.api.setting.KeySetting;
import dev.mw19.api.setting.Setting;
import dev.mw19.core.gui.GuiRoot;
import dev.mw19.core.gui.Ui;
import dev.mw19.core.gui.Widget;
import dev.mw19.core.gui.widget.Button;
import dev.mw19.core.gui.widget.KeybindButton;
import dev.mw19.core.gui.widget.ScrollList;
import dev.mw19.core.gui.widget.TextField;
import dev.mw19.core.module.ModuleManager;

/**
 * Bind profiles (saved sets of Minecraft's own key bindings, shared by every instance), then every MW19 key binding in
 * one list; conflicts (with ours or vanilla) are outlined.
 */
public final class KeybindsPage extends Page {
    private final ScrollList list = new ScrollList();

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
        if (root.k.binds != null) list.add(new ProfilesPanel());
        list.add(new Row("MW19", root.k.client.openGui));
        for (ModuleManager.State s : root.k.modules.all()) {
            if ((s.suspend() & ModuleManager.SUSPEND_UNAVAILABLE) != 0) continue;
            for (Setting<?> set : s.module.settings()) {
                if (set instanceof KeySetting) list.add(new Row(s.module.name(), (KeySetting) set));
            }
        }
    }

    @Override
    public void render(Ui ui) {
        heading(ui, "Keybinds", "Bind profiles for Minecraft's own keys, then MW19's: click one and press a key. Amber = also used elsewhere.");
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
                    status = "Applied " + selected + ": " + n + (n == 1 ? " bind" : " binds") + " changed.";
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
            String where = "Every instance · " + root.k.binds.file().getParent().getFileName();
            ui.g.text(where, x + w - 6 - ui.g.textWidth(where), yy, ui.t.textDim, false);
            yy += 14;
            float cx = x + 6;
            if (chips.isEmpty()) {
                ui.g.text("None yet: set your binds in Minecraft's Controls, then save them here.", cx, yy + 4, ui.t.textDim, false);
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
            ui.g.text(owner, x + 7, y + 6, ui.t.textDim, false);
            ui.g.text(setting.name(), x + 7 + ui.g.textWidth(owner) + 6, y + 6, ui.t.text, false);
            button.bounds(x + w - 96, y + 2, 92, 16).render(ui);
        }

        @Override
        public boolean mouseClicked(Ui ui, int b) {
            return button.contains(ui.mx, ui.my) && button.mouseClicked(ui, b);
        }
    }
}
