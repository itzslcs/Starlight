package dev.mw19.core.gui.page;

import dev.mw19.api.util.Colors;
import dev.mw19.core.gui.GuiRoot;
import dev.mw19.core.gui.Toasts;
import dev.mw19.core.gui.Ui;
import dev.mw19.core.gui.widget.Button;
import dev.mw19.core.gui.widget.TextField;
import dev.mw19.core.gui.widget.Toggle;
import dev.mw19.core.host.WorldHost;

import java.util.List;

/** Host World: open the singleplayer world to friends on the local network and, through the router, the internet. */
public final class HostPage extends Page {
    private static final String[] MODES = {"Survival", "Creative", "Adventure", "Spectator"};

    private int mode;
    private boolean commands;
    private final Button choose, modeButton, open, copyLan, copyNet, invite;
    private final Toggle commandsToggle, internet;
    private final TextField name = new TextField("");
    private float removeX, removeY0;

    public HostPage(final GuiRoot root) {
        super(root);
        choose = new Button("Choose a world", Button.Style.PRIMARY, new Runnable() {
            @Override
            public void run() {
                root.k.hostWhenWorldOpens = true;
                root.k.platform.screens().openSingleplayer();
            }
        });
        modeButton = new Button("", Button.Style.SECONDARY, new Runnable() {
            @Override
            public void run() {
                mode = (mode + 1) % MODES.length;
            }
        });
        commandsToggle = new Toggle(new Toggle.Model() {
            @Override
            public boolean get() {
                return commands;
            }

            @Override
            public void set(boolean v) {
                commands = v;
            }
        });
        open = new Button("Open to friends", Button.Style.PRIMARY, new Runnable() {
            @Override
            public void run() {
                if (root.k.host.open(mode, commands) <= 0) root.k.toast("Host World", "Minecraft could not open the world (is the port in use?).", root.k.theme.bad);
            }
        });
        copyLan = new Button("Copy", Button.Style.SECONDARY, new Runnable() {
            @Override
            public void run() {
                copy(root.k.host.lanAddress());
            }
        });
        copyNet = new Button("Copy", Button.Style.SECONDARY, new Runnable() {
            @Override
            public void run() {
                copy(root.k.host.publicAddress());
            }
        });
        internet = new Toggle(new Toggle.Model() {
            @Override
            public boolean get() {
                WorldHost.Net n = root.k.host.net();
                return n == WorldHost.Net.WORKING || n == WorldHost.Net.OPEN;
            }

            @Override
            public void set(boolean v) {
                if (v) root.k.host.openInternet();
                else root.k.host.closeInternet();
            }
        });
        internet.tooltip = "Asks your router (UPnP) to forward the world's port. Only invited players can join.";
        invite = new Button("Invite", Button.Style.SECONDARY, new Runnable() {
            @Override
            public void run() {
                sendInvite();
            }
        });
        name.placeholder = "Friend's player name";
        name.maxLength = 16;
        name.onSubmit = new Runnable() {
            @Override
            public void run() {
                sendInvite();
            }
        };
    }

    private void sendInvite() {
        root.k.host.invite(name.text.trim());
        name.setText("");
    }

    private void copy(String s) {
        if (s == null) return;
        root.k.platform.setClipboard(s);
        root.k.toast("Copied", s, root.k.theme.good);
    }

    @Override
    public String title() {
        return "Host World";
    }

    @Override
    public String icon() {
        return "host";
    }

    @Override
    public void render(Ui ui) {
        heading(ui, "Host World", "Let friends join the world you are playing.");
        WorldHost host = root.k.host;
        float yy = y + 32;
        if (!host.available()) {
            boolean onServer = root.k.platform.inWorld();
            ui.g.text(onServer ? "You are on a server. Hosting works for singleplayer worlds." : "Open a singleplayer world to host it.", x, yy, ui.t.text, false);
            if (!onServer) {
                ui.g.text("It opens this page again once the world has loaded.", x, yy + 11, ui.t.textDim, false);
                choose.bounds(x, yy + 28, ui.g.textWidth(choose.label) + 20, 18).render(ui);
            }
            return;
        }
        int port = host.port();
        if (port <= 0) {
            row(ui, "Game mode for friends", yy);
            modeButton.label = MODES[mode];
            modeButton.bounds(x + w - 90, yy - 3, 90, 15).render(ui);
            row(ui, "Allow commands (cheats)", yy + 22);
            commandsToggle.bounds(x + w - 40, yy + 19, 40, 15).render(ui);
            open.bounds(x, yy + 48, ui.g.textWidth(open.label) + 24, 18).render(ui);
            ui.g.text("Friends on your network can join right away; the internet is one switch away after this.", x, yy + 74, 0.85f, ui.t.textDim, false);
            return;
        }
        String lan = host.lanAddress();
        row(ui, "On your network", yy);
        ui.g.textRight(lan == null ? "port " + port : lan, x + w - 44, yy, ui.t.text, false);
        copyLan.enabled = lan != null;
        copyLan.bounds(x + w - 38, yy - 3, 38, 14).render(ui);
        yy += 20;
        row(ui, "Over the internet", yy);
        internet.bounds(x + w - 40, yy - 3, 40, 15).render(ui);
        yy += 14;
        WorldHost.Net n = host.net();
        if (!host.netMessage().isEmpty()) {
            int col = n == WorldHost.Net.FAILED ? ui.t.bad : n == WorldHost.Net.OPEN ? ui.t.good : ui.t.textDim;
            List<String> lines = Toasts.wrap(ui.g, host.netMessage(), w);
            for (int i = 0; i < lines.size() && i < 3; i++) ui.g.text(lines.get(i), x, yy + i * 10, col, false);
            yy += Math.min(3, lines.size()) * 10 + 2;
        }
        String pub = host.publicAddress();
        if (pub != null) {
            row(ui, "Internet address", yy + 2);
            ui.g.textRight(pub, x + w - 44, yy + 2, ui.t.text, false);
            copyNet.bounds(x + w - 38, yy - 1, 38, 14).render(ui);
            yy += 20;
        }
        yy += 6;
        ui.g.rect(x, yy, x + w, yy + 1, ui.t.border);
        yy += 7;
        ui.g.text("Invited players", x, yy, ui.t.text, false);
        ui.g.textRight(host.inviteMessage(), x + w, yy, ui.t.textDim, false);
        yy += 13;
        float iw = ui.g.textWidth(invite.label) + 16;
        name.bounds(x, yy, w - iw - 4, 16).render(ui);
        invite.bounds(x + w - iw, yy, iw, 16).render(ui);
        yy += 21;
        removeX = x + w - 12;
        removeY0 = yy;
        List<String[]> inv = host.invited();
        if (inv.isEmpty()) {
            List<String> note = Toasts.wrap(ui.g, "Nobody yet. Over the internet, only players you invite can join.", w);
            for (int i = 0; i < note.size(); i++) ui.g.text(note.get(i), x, yy + i * 10, ui.t.textDim, false);
        }
        for (int i = 0; i < inv.size() && yy + 11 < y + h; i++, yy += 12) {
            ui.g.text(inv.get(i)[0], x + 2, yy, ui.t.text, false);
            boolean hv = ui.hover(removeX - 2, yy - 2, 12, 12);
            ui.g.text("×", removeX, yy, hv ? ui.t.bad : ui.t.textDim, false);
        }
    }

    private void row(Ui ui, String label, float yy) {
        ui.g.text(label, x, yy, Colors.lerp(ui.t.textDim, ui.t.text, 0.7f), false);
    }

    @Override
    public boolean mouseClicked(Ui ui, int button) {
        WorldHost host = root.k.host;
        if (!host.available()) return choose.mouseClicked(ui, button);
        if (host.port() <= 0) return modeButton.mouseClicked(ui, button) || commandsToggle.mouseClicked(ui, button) || open.mouseClicked(ui, button);
        if (copyLan.mouseClicked(ui, button) || internet.mouseClicked(ui, button) || copyNet.mouseClicked(ui, button)
                || name.mouseClicked(ui, button) || invite.mouseClicked(ui, button)) return true;
        List<String[]> inv = host.invited();
        for (int i = 0; i < inv.size(); i++) {
            if (ui.hover(removeX - 2, removeY0 + i * 12 - 2, 12, 12)) {
                host.uninvite(inv.get(i));
                return true;
            }
        }
        return false;
    }
}
