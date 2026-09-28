package dev.starlight.core.gui.page;

import dev.starlight.core.gui.GuiRoot;
import dev.starlight.core.gui.Ui;
import dev.starlight.core.gui.Widget;
import dev.starlight.core.gui.widget.Button;
import dev.starlight.core.gui.widget.Dropdown;
import dev.starlight.core.gui.widget.ScrollList;
import dev.starlight.core.gui.widget.TextField;

import java.util.ArrayList;
import java.util.List;

/** Unlimited profiles: switch, create, duplicate, rename, delete, export/import strings, per-server auto-switch. */
public final class ProfilesPage extends Page {
    private final ScrollList list = new ScrollList();
    private final TextField newName = new TextField("");
    private final Button create, duplicate, importB;
    private final TextField pattern = new TextField("");
    private String patternProfile;
    private final Dropdown patternPick;
    private final Button addRule;

    public ProfilesPage(final GuiRoot root) {
        super(root);
        newName.placeholder = "New profile name";
        newName.maxLength = 32;
        create = new Button("Create", Button.Style.PRIMARY, new Runnable() {
            @Override
            public void run() {
                make(false);
            }
        });
        duplicate = new Button("Duplicate current", Button.Style.SECONDARY, new Runnable() {
            @Override
            public void run() {
                make(true);
            }
        });
        importB = new Button("Import from clipboard", Button.Style.SECONDARY, new Runnable() {
            @Override
            public void run() {
                try {
                    String n = root.k.config.importProfile(root.k.platform.clipboard());
                    root.k.toast("Profile imported", "Added \"" + n + "\"", root.k.theme.good);
                    rebuild();
                } catch (Exception e) {
                    root.k.toast("Import failed", e.getMessage(), root.k.theme.bad);
                }
            }
        });
        importB.tooltip = "Paste a STARLIGHT-P1:… string copied with Export";
        pattern.placeholder = "e.g. *.hypixel.net";
        patternPick = new Dropdown(new Dropdown.Model() {
            @Override
            public List<String> options() {
                return root.k.config.profiles();
            }

            @Override
            public boolean selected(String o) {
                return o.equals(patternProfile);
            }

            @Override
            public void pick(String o) {
                patternProfile = o;
            }

            @Override
            public String summary() {
                return patternProfile == null ? "Profile…" : patternProfile;
            }
        }, false);
        addRule = new Button("Add", Button.Style.SECONDARY, new Runnable() {
            @Override
            public void run() {
                String p = pattern.text.trim();
                if (p.isEmpty() || patternProfile == null) return;
                List<String[]> rules = new ArrayList<String[]>(root.k.config.autoProfiles());
                rules.add(new String[]{p, patternProfile});
                root.k.config.setAutoProfiles(rules);
                pattern.setText("");
                rebuild();
            }
        });
    }

    private void make(boolean copy) {
        try {
            root.k.config.create(newName.text.trim(), copy);
            root.k.toast("Profile created", newName.text.trim(), root.k.theme.good);
            newName.setText("");
            rebuild();
        } catch (Exception e) {
            root.k.toast("Could not create profile", e.getMessage(), root.k.theme.bad);
        }
    }

    @Override
    public String title() {
        return "Profiles";
    }

    @Override
    public String icon() {
        return "profiles";
    }

    @Override
    public void onShow() {
        rebuild();
    }

    private void rebuild() {
        list.clear();
        list.gap = 2;
        for (String p : root.k.config.profiles()) list.add(new Row(p));
        Widget header = new Widget() {
            @Override
            public void render(Ui ui) {
                ui.g.text("Auto-switch when joining a server", x + 2, y + 6, ui.t.text, false);
            }
        };
        header.h = 18;
        list.add(header);
        for (String[] r : root.k.config.autoProfiles()) list.add(new RuleRow(r));
    }

    @Override
    public void render(Ui ui) {
        heading(ui, "Profiles", "Active: " + root.k.config.active() + " · switching is instant");
        float ry = y + 26;
        newName.bounds(x, ry, w - 190, 16).render(ui);
        create.bounds(x + w - 186, ry, 50, 16).render(ui);
        duplicate.bounds(x + w - 132, ry, 132, 16).render(ui);
        importB.bounds(x, ry + 20, 130, 16).render(ui);
        pattern.bounds(x + 136, ry + 20, w - 136 - 122, 16).render(ui);
        patternPick.bounds(x + w - 118, ry + 20, 80, 16).render(ui);
        addRule.bounds(x + w - 34, ry + 20, 34, 16).render(ui);
        list.bounds(x, ry + 42, w, h - (ry + 42 - y));
        list.render(ui);
    }

    @Override
    public boolean mouseClicked(Ui ui, int button) {
        return newName.mouseClicked(ui, button) || create.mouseClicked(ui, button) || duplicate.mouseClicked(ui, button)
                || importB.mouseClicked(ui, button) || pattern.mouseClicked(ui, button) || patternPick.mouseClicked(ui, button)
                || addRule.mouseClicked(ui, button) || list.mouseClicked(ui, button);
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
        private final String name;
        private final Button use, export, rename, delete;
        private TextField editing;

        Row(final String name) {
            this.name = name;
            this.h = 20;
            use = new Button("Use", Button.Style.PRIMARY, new Runnable() {
                @Override
                public void run() {
                    root.k.config.switchTo(name);
                    root.k.toast("Profile", "Now using \"" + name + "\"", root.k.theme.accent);
                }
            });
            export = new Button("Export", Button.Style.SECONDARY, new Runnable() {
                @Override
                public void run() {
                    try {
                        root.k.platform.setClipboard(root.k.config.export(name));
                        root.k.toast("Profile exported", "\"" + name + "\" copied to clipboard", root.k.theme.good);
                    } catch (Exception e) {
                        root.k.toast("Export failed", e.getMessage(), root.k.theme.bad);
                    }
                }
            });
            rename = new Button("Rename", Button.Style.SECONDARY, new Runnable() {
                @Override
                public void run() {
                    editing = new TextField(name);
                    editing.maxLength = 32;
                    editing.onSubmit = new Runnable() {
                        @Override
                        public void run() {
                            try {
                                root.k.config.rename(name, editing.text.trim());
                                rebuild();
                            } catch (Exception e) {
                                root.k.toast("Rename failed", e.getMessage(), root.k.theme.bad);
                            }
                        }
                    };
                    root.focus(editing);
                }
            });
            delete = new Button("", Button.Style.DANGER, new Runnable() {
                @Override
                public void run() {
                    try {
                        root.k.config.delete(name);
                        rebuild();
                    } catch (Exception e) {
                        root.k.toast("Delete failed", e.getMessage(), root.k.theme.bad);
                    }
                }
            }).icon("close");
            delete.tooltip = "Delete (a backup is kept in Starlight/backups)";
        }

        @Override
        public void render(Ui ui) {
            boolean active = name.equals(root.k.config.active());
            ui.g.roundRect(x, y, w, h, 4, active ? ui.t.accentSoft(0.15f) : ui.t.surface);
            if (editing != null && root.isFocused(editing)) {
                editing.bounds(x + 4, y + 2, w - 170, 16).render(ui);
            } else {
                editing = null;
                ui.g.text(name, x + 7, y + 6, ui.t.text, false);
                if (active) ui.g.text("active", x + 12 + ui.g.textWidth(name), y + 6, ui.t.accent, false);
            }
            float bx = x + w - 4;
            delete.bounds(bx -= 18, y + 2, 18, 16).render(ui);
            rename.bounds(bx -= 48, y + 2, 44, 16).render(ui);
            export.bounds(bx -= 46, y + 2, 42, 16).render(ui);
            if (!active) use.bounds(bx -= 36, y + 2, 32, 16).render(ui);
        }

        @Override
        public boolean mouseClicked(Ui ui, int button) {
            boolean active = name.equals(root.k.config.active());
            return (editing != null && editing.mouseClicked(ui, button)) || delete.mouseClicked(ui, button) || rename.mouseClicked(ui, button)
                    || export.mouseClicked(ui, button) || (!active && use.mouseClicked(ui, button));
        }
    }

    private final class RuleRow extends Widget {
        private final String[] rule;
        private final Button remove;

        RuleRow(final String[] rule) {
            this.rule = rule;
            this.h = 18;
            remove = new Button("", Button.Style.GHOST, new Runnable() {
                @Override
                public void run() {
                    List<String[]> rules = new ArrayList<String[]>();
                    for (String[] r : root.k.config.autoProfiles()) if (!(r[0].equals(rule[0]) && r[1].equals(rule[1]))) rules.add(r);
                    root.k.config.setAutoProfiles(rules);
                    rebuild();
                }
            }).icon("close");
        }

        @Override
        public void render(Ui ui) {
            ui.g.roundRect(x, y, w, h, 3, ui.t.surface);
            ui.g.text(rule[0] + "  →  " + rule[1], x + 7, y + 5, ui.t.text, false);
            remove.bounds(x + w - 20, y + 1, 18, 16).render(ui);
        }

        @Override
        public boolean mouseClicked(Ui ui, int button) {
            return remove.mouseClicked(ui, button);
        }
    }
}
