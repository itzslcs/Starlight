package dev.starlight.core.gui.page;

import dev.starlight.api.util.Colors;
import dev.starlight.core.Starlight;
import dev.starlight.core.gui.GuiRoot;
import dev.starlight.core.gui.Toasts;
import dev.starlight.core.gui.Ui;
import dev.starlight.core.gui.Widget;
import dev.starlight.core.gui.widget.Button;
import dev.starlight.core.gui.widget.ScrollList;
import dev.starlight.core.gui.widget.TextField;
import dev.starlight.core.skin.SkinLibrary;
import dev.starlight.core.skin.SkinService;

import java.nio.file.Path;
import java.util.List;

/**
 * Skins: a 3D preview (drag to turn it), the skin folder as a list, copying a player's skin by name, and applying the
 * selected skin to the account through Mojang's skin API. Skins are PNG files in {@code Starlight/skins}; dropping files on
 * the window adds them (where the platform supports drops).
 */
public final class SkinsPage extends Page {
    private static final float LEFT = 118;

    private final ScrollList list = new ScrollList();
    private final Preview preview = new Preview();
    private final Button classic, slim, apply, folder, refresh, copy;
    private final TextField name = new TextField("");
    /** Selected library entry; null shows the account's own skin. */
    private SkinLibrary.Entry selected;
    private String status = "";
    private int statusColor;
    private boolean busy, loaded;

    public SkinsPage(final GuiRoot root) {
        super(root);
        list.gap = 2;
        classic = new Button("Classic", Button.Style.SECONDARY, new Runnable() {
            @Override
            public void run() {
                if (selected != null) root.k.skins.setSlim(selected, false);
            }
        });
        slim = new Button("Slim", Button.Style.SECONDARY, new Runnable() {
            @Override
            public void run() {
                if (selected != null) root.k.skins.setSlim(selected, true);
            }
        });
        classic.tooltip = "Arms 4 pixels wide (Steve)";
        slim.tooltip = "Arms 3 pixels wide (Alex)";
        apply = new Button("Use this skin", Button.Style.PRIMARY, new Runnable() {
            @Override
            public void run() {
                applySelected();
            }
        });
        apply.tooltip = "Changes your Minecraft skin for everyone (through Mojang's skin service)";
        folder = new Button("Open folder", Button.Style.SECONDARY, new Runnable() {
            @Override
            public void run() {
                root.k.skins.refresh();
                root.k.platform.openFolder(root.k.skins.folder());
            }
        });
        folder.tooltip = "Put 64×64 PNG skins here, then press Refresh";
        refresh = new Button("Refresh", Button.Style.SECONDARY, new Runnable() {
            @Override
            public void run() {
                reload();
            }
        });
        copy = new Button("Copy", Button.Style.SECONDARY, new Runnable() {
            @Override
            public void run() {
                copyFromPlayer();
            }
        });
        copy.tooltip = "Adds that player's current skin to your folder";
        name.placeholder = "Player name";
        name.maxLength = 16;
        name.onSubmit = new Runnable() {
            @Override
            public void run() {
                copyFromPlayer();
            }
        };
    }

    @Override
    public String title() {
        return "Skins";
    }

    @Override
    public String icon() {
        return "skins";
    }

    @Override
    public void onShow() {
        if (!loaded) reload();
    }

    private void reload() {
        for (SkinLibrary.Entry e : root.k.skins.entries()) release(e);
        root.k.skins.refresh();
        loaded = true;
        if (selected != null && !root.k.skins.entries().contains(selected)) selected = null;
        rebuild();
    }

    @Override
    public void dispose() {
        for (SkinLibrary.Entry e : root.k.skins.entries()) release(e);
    }

    private void release(SkinLibrary.Entry e) {
        if (e.handle > 0) root.k.platform.skins().releaseImage(e.handle);
        e.handle = 0;
    }

    private void rebuild() {
        list.clear();
        list.add(new Row(null));
        for (SkinLibrary.Entry e : root.k.skins.entries()) list.add(new Row(e));
    }

    private void say(String msg, int color) {
        status = msg;
        statusColor = color;
    }

    private void applySelected() {
        final SkinLibrary.Entry e = selected;
        if (e == null || busy) return;
        busy = true;
        say("Uploading " + e.label() + "…", root.k.theme.textDim);
        SkinService.upload(root.k.http, root.k.platform.skins().accessToken(), e.png, e.slim, new SkinService.Result() {
            @Override
            public void done(boolean ok, String message, String texture) {
                if (!ok) {
                    busy = false;
                    say(message, root.k.theme.bad);
                    root.k.toast("Skin not changed", message, root.k.theme.bad);
                    return;
                }
                // Mojang has it; the game still shows the skin it loaded at start until the profile is fetched again.
                say("Skin changed. Loading it in game…", root.k.theme.good);
                root.k.platform.skins().refreshOwnSkin(texture, new dev.starlight.core.platform.Skins.Refreshed() {
                    @Override
                    public void done(boolean shown) {
                        busy = false;
                        String m = shown ? "Skin changed. You see it now; other players see it after you rejoin a server."
                                : "Skin changed on your account. It shows in game after you restart Minecraft.";
                        say(m, root.k.theme.good);
                        root.k.toast("Skin changed", m, root.k.theme.good);
                    }
                });
            }
        });
    }

    private void copyFromPlayer() {
        final String n = name.text.trim();
        if (busy) return;
        if (!SkinService.validName(n)) {
            say("Player names are 3-16 letters, digits or _", root.k.theme.bad);
            return;
        }
        busy = true;
        say("Looking up " + n + "…", root.k.theme.textDim);
        SkinService.fetch(root.k.http, n, new SkinService.Fetched() {
            @Override
            public void done(byte[] png, boolean isSlim, String error) {
                busy = false;
                if (png == null) {
                    say(error, root.k.theme.bad);
                    return;
                }
                try {
                    selected = root.k.skins.add(n, png, isSlim);
                    rebuild();
                    name.setText("");
                    say("Added " + selected.label() + " to your skins", root.k.theme.good);
                } catch (java.io.IOException ex) {
                    say("Could not save the skin: " + ex.getMessage(), root.k.theme.bad);
                }
            }
        });
    }

    @Override
    public void filesDropped(List<Path> files) {
        int added = 0;
        String why = null;
        for (Path p : files) {
            String err = root.k.skins.importFile(p);
            if (err == null) added++;
            else why = p.getFileName() + ": " + err;
        }
        reload();
        if (added > 0) say("Added " + added + " skin" + (added == 1 ? "" : "s"), root.k.theme.good);
        else if (why != null) say(why, root.k.theme.bad);
    }

    @Override
    public void render(Ui ui) {
        heading(ui, "Skins", "Preview skins and change yours. Drop PNG files here to add them.");
        float top = y + 30;
        // left: preview + model + apply
        preview.bounds(x, top, LEFT - 8, h - 30 - 70);
        preview.render(ui);
        boolean own = selected == null;
        String label = own ? "Your skin" : selected.label();
        ui.g.textCentered(ui.g.ellipsize(label, LEFT - 10), x + (LEFT - 8) / 2f, preview.y + preview.h + 4, ui.t.text, false);
        float by = preview.y + preview.h + 16, bw = (LEFT - 12) / 2f;
        classic.enabled = slim.enabled = !own;
        boolean isSlim = own ? root.k.platform.skins().ownSkinSlim() : selected.slim;
        classic.style = isSlim ? Button.Style.SECONDARY : Button.Style.PRIMARY;
        slim.style = isSlim ? Button.Style.PRIMARY : Button.Style.SECONDARY;
        classic.bounds(x, by, bw, 14).render(ui);
        slim.bounds(x + bw + 4, by, bw, 14).render(ui);
        apply.enabled = !own && !busy;
        apply.bounds(x, by + 18, LEFT - 8, 16).render(ui);
        if (!status.isEmpty()) {
            List<String> lines = Toasts.wrap(ui.g, status, LEFT - 8);
            for (int i = 0; i < lines.size() && i < 3; i++) ui.g.text(lines.get(i), x, by + 38 + i * 9, 0.85f, statusColor, false);
        }
        // right: tools + list
        float rx = x + LEFT, rw = w - LEFT;
        float cw = ui.g.textWidth(copy.label) + 14;
        name.bounds(rx, top, rw - cw - 4, 16).render(ui);
        copy.enabled = !busy;
        copy.bounds(rx + rw - cw, top, cw, 16).render(ui);
        float fw = ui.g.textWidth(folder.label) + 14, rfw = ui.g.textWidth(refresh.label) + 14;
        folder.bounds(rx, top + 20, fw, 14).render(ui);
        refresh.bounds(rx + fw + 4, top + 20, rfw, 14).render(ui);
        list.bounds(rx, top + 40, rw, h - 70);
        list.render(ui);
    }

    @Override
    public boolean mouseClicked(Ui ui, int button) {
        return preview.mouseClicked(ui, button) || classic.mouseClicked(ui, button) || slim.mouseClicked(ui, button)
                || apply.mouseClicked(ui, button) || name.mouseClicked(ui, button) || copy.mouseClicked(ui, button)
                || folder.mouseClicked(ui, button) || refresh.mouseClicked(ui, button) || list.mouseClicked(ui, button);
    }

    @Override
    public boolean mouseScrolled(Ui ui, double amount) {
        return list.mouseScrolled(ui, amount);
    }

    @Override
    public void reveal(Widget w) {
        list.reveal(w);
    }

    /** The model; drag to turn it. */
    private final class Preview extends Widget {
        private float yaw = 30, pitch = -5, lastX, lastY;

        @Override
        public void render(Ui ui) {
            ui.g.roundRect(x, y, w, h, 5, Colors.fade(ui.t.surface, 0.6f));
            int handle = 0;
            boolean s = root.k.platform.skins().ownSkinSlim();
            if (selected != null) {
                handle = texture(selected);
                s = selected.slim;
            }
            ui.g.player(x + 4, y + 6, w - 8, h - 10, handle, s, yaw, pitch);
        }

        @Override
        public boolean mouseClicked(Ui ui, int button) {
            if (button != 0 || !contains(ui.mx, ui.my)) return false;
            lastX = ui.mx;
            lastY = ui.my;
            ui.root.capture(this);
            return true;
        }

        @Override
        public boolean mouseDragged(Ui ui, int button) {
            yaw += (ui.mx - lastX) * 2.5f;
            pitch = Math.max(-50, Math.min(50, pitch - (ui.my - lastY) * 2.5f));
            lastX = ui.mx;
            lastY = ui.my;
            return true;
        }
    }

    private int texture(SkinLibrary.Entry e) {
        if (e.handle == 0) e.handle = root.k.platform.skins().loadImage(e.png);
        if (e.handle == 0) e.handle = -1; // unreadable: do not retry every frame
        return Math.max(0, e.handle);
    }

    /** One skin in the list: its face, name and model. */
    private final class Row extends Widget {
        private final SkinLibrary.Entry e;

        Row(SkinLibrary.Entry e) {
            this.e = e;
            this.h = 20;
        }

        @Override
        public void render(Ui ui) {
            boolean sel = selected == e, hv = hovered(ui);
            if (sel || hv) ui.g.roundRect(x, y, w, h, 3, sel ? ui.t.accentSoft(0.25f) : Colors.fade(ui.t.surface2, 0.6f));
            float fx = x + 3, fy = y + 2, fs = 16;
            if (e == null) {
                ui.g.roundRect(fx, fy, fs, fs, 2, ui.t.surface2);
                ui.g.textCentered("?", fx + fs / 2f, fy + 4, ui.t.textDim, false);
                ui.g.text("Your skin", x + 25, y + 6, ui.t.text, false);
                ui.g.textRight("current", x + w - 4, y + 6, ui.t.textDim, false);
            } else {
                int t = texture(e);
                if (t > 0) {
                    float vh = e.texH;
                    ui.g.image(t, fx, fy, fs, fs, 8 / 64f, 8 / vh, 16 / 64f, 16 / vh);
                    ui.g.image(t, fx, fy, fs, fs, 40 / 64f, 8 / vh, 48 / 64f, 16 / vh);
                }
                ui.g.text(ui.g.ellipsize(e.label(), w - 60), x + 25, y + 6, ui.t.text, false);
                ui.g.textRight(e.slim ? "slim" : "classic", x + w - 4, y + 6, ui.t.textDim, false);
            }
        }

        @Override
        public boolean mouseClicked(Ui ui, int button) {
            if (button != 0 || !contains(ui.mx, ui.my)) return false;
            selected = e;
            status = "";
            return true;
        }
    }

    /** Smoke: reloads the folder and selects {@code file}; false if it is not there. */
    public boolean select(String file) {
        reload();
        for (SkinLibrary.Entry e : root.k.skins.entries()) {
            if (e.file.equals(file)) {
                selected = e;
                return true;
            }
        }
        return false;
    }

    /** For the home screen's Skins button. */
    public static void open(Starlight k) {
        k.gui().openPage(SkinsPage.class);
    }
}
