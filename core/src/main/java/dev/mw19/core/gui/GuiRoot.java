package dev.mw19.core.gui;

import dev.mw19.api.setting.KeySetting;
import dev.mw19.api.setting.Setting;
import dev.mw19.api.util.Colors;
import dev.mw19.core.Guard;
import dev.mw19.core.Mw19;
import dev.mw19.core.Keys;
import dev.mw19.core.Log;
import dev.mw19.core.gui.page.AboutPage;
import dev.mw19.core.gui.page.HudEditor;
import dev.mw19.core.gui.page.KeybindsPage;
import dev.mw19.core.gui.page.ModsPage;
import dev.mw19.core.gui.page.Page;
import dev.mw19.core.gui.page.PerfPage;
import dev.mw19.core.gui.page.PluginsPage;
import dev.mw19.core.gui.page.ProfilesPage;
import dev.mw19.core.gui.page.RulesPage;
import dev.mw19.core.gui.page.ThemesPage;
import dev.mw19.core.module.ModuleManager;
import dev.mw19.core.render.Gfx;
import dev.mw19.core.render.RenderBackend;

import java.util.ArrayList;
import java.util.List;

/**
 * The whole menu: sidebar + pages + popup layer + tooltips, or the full-screen HUD editor.
 * The platform's Screen forwards render/input here in vanilla GUI units; we apply our own menu scale.
 */
public final class GuiRoot {
    public static final float SIDEBAR = 112;

    public final Mw19 k;
    private final Ui ui = new Ui();
    private final Gfx g = new Gfx();
    private final List<Page> pages = new ArrayList<Page>();
    private final Anim[] sideHover;
    private Page page;
    private final HudEditor hudEditor;
    private final dev.mw19.core.gui.page.ChatSearchPage chatSearch;
    private boolean editing;

    private Widget popup, popupOwner, focused, captured;
    private final List<Widget> focusOrder = new ArrayList<Widget>();
    private final List<Widget> lastFocusOrder = new ArrayList<Widget>();
    private final Anim open = new Anim(0);
    private float width, height, scale = 1;
    private long hoverSince;
    private String lastTooltip;
    private final List<String[]> vanillaBinds = new ArrayList<String[]>();

    public GuiRoot(Mw19 k) {
        this.k = k;
        ui.k = k;
        ui.root = this;
        pages.add(new ModsPage(this));
        pages.add(new ProfilesPage(this));
        pages.add(new KeybindsPage(this));
        pages.add(new PluginsPage(this));
        pages.add(new RulesPage(this));
        pages.add(new PerfPage(this));
        pages.add(new ThemesPage(this));
        pages.add(new AboutPage(this));
        sideHover = new Anim[pages.size()];
        for (int i = 0; i < sideHover.length; i++) sideHover[i] = new Anim(0);
        page = pages.get(0);
        hudEditor = new HudEditor(this);
        chatSearch = new dev.mw19.core.gui.page.ChatSearchPage(this);
    }

    // ------------------------------------------------------------------ lifecycle

    /** Our screen was opened. */
    public void onOpen() {
        open.snap(0);
        open.to(1, 200, System.currentTimeMillis());
        popup = popupOwner = captured = null;
        vanillaBinds.clear();
        Guard.run("vanilla binds", new Runnable() {
            @Override
            public void run() {
                k.platform.vanillaBindings(new dev.mw19.core.platform.Platform.BindingSink() {
                    @Override
                    public void accept(String name, int key) {
                        vanillaBinds.add(new String[]{name, Integer.toString(key)});
                    }
                });
            }
        });
        page.onShow();
    }

    /** Our screen is closing (any reason). */
    public void onClose() {
        editing = false;
        popup = popupOwner = captured = null;
        k.config.markDirty();
    }

    public void close() {
        k.platform.screens().closeGui();
    }

    public void openHudEditor() {
        if (k.platform.screens().current() != dev.mw19.core.platform.ScreenHost.Kind.OURS) k.openGui();
        editing = true;
        popup = popupOwner = null;
        hudEditor.onShow();
    }

    /** Opens the menu on the chat search page (Chat Tools' search key). */
    public void openChatSearch() {
        if (k.platform.screens().current() != dev.mw19.core.platform.ScreenHost.Kind.OURS) k.openGui();
        editing = false;
        show(chatSearch);
    }

    public void closeHudEditor() {
        editing = false;
        popup = popupOwner = null;
    }

    public boolean isHudEditorOpen() {
        return editing;
    }

    public void show(Page p) {
        if (p == page) return;
        page = p;
        popup = popupOwner = null;
        focused = null;
        page.onShow();
    }

    /** Menu scale applied on top of the vanilla GUI scale (menu units x scale = vanilla GUI units). */
    public float menuScale() {
        return scale;
    }

    public List<Page> pages() {
        return pages;
    }

    public <T extends Page> T page(Class<T> type) {
        for (Page p : pages) if (type.isInstance(p)) return type.cast(p);
        return null;
    }

    // ------------------------------------------------------------------ geometry

    public float width() {
        return width;
    }

    public float height() {
        return height;
    }

    public float panelW() {
        return Math.min(width - 16, 480);
    }

    public float panelH() {
        return Math.min(height - 16, 300);
    }

    public float panelX() {
        return (width - panelW()) / 2f;
    }

    public float panelY() {
        return (height - panelH()) / 2f;
    }

    // ------------------------------------------------------------------ render

    public void render(RenderBackend backend, int screenW, int screenH, float mouseX, float mouseY) {
        long now = System.currentTimeMillis();
        scale = k.client.uiScale.floatValue();
        width = screenW / scale;
        height = screenH / scale;
        g.begin(backend, now);
        try {
            ui.g = g;
            ui.t = k.theme;
            ui.mx = mouseX / scale;
            ui.my = mouseY / scale;
            ui.now = now;
            ui.tooltip = null;
            g.push();
            g.scale(scale);
            lastFocusOrder.clear();
            lastFocusOrder.addAll(focusOrder);
            focusOrder.clear();
            if (editing) {
                hudEditor.render(ui);
            } else {
                renderMenu(now);
            }
            if (popup != null) popup.render(ui);
            k.toasts.render(g, ui.t, width, now);
            renderTooltip(now);
            g.pop();
        } catch (VirtualMachineError e) {
            throw e;
        } catch (Throwable t) {
            Log.error("GUI render failed", t);
        } finally {
            g.end();
        }
    }

    private void renderMenu(long now) {
        Theme t = ui.t;
        float o = open.get(now);
        if (!k.client.blur.on() || !k.platform.supports("blur")) g.rect(0, 0, width, height, Colors.fade(t.dim, o));
        float pw = panelW(), ph = panelH(), px = panelX(), py = panelY();
        g.push();
        float s = 0.96f + 0.04f * o;
        g.translate(px + pw / 2f, py + ph / 2f);
        g.scale(s);
        g.translate(-(px + pw / 2f), -(py + ph / 2f));
        g.pushAlpha(o);
        // panel + sidebar
        g.roundRect(px, py, pw, ph, 7, t.panel);
        g.roundRect(px, py, SIDEBAR, ph, 7, t.sidebar);
        g.rect(px + SIDEBAR - 7, py, px + SIDEBAR, py + ph, t.sidebar);
        g.roundOutline(px, py, pw, ph, 7, 1, t.border);
        // brand (original wordmark: name in accent with a small wing mark)
        g.roundRect(px + 10, py + 11, 9, 9, 2, t.accent);
        g.rect(px + 12, py + 14, px + 17, py + 15, t.onAccent);
        g.rect(px + 14, py + 16, px + 17, py + 17, t.onAccent);
        g.text(Mw19.NAME, px + 23, py + 11, 1.25f, t.text, false);
        g.text("v" + k.modVersion, px + 23, py + 23, t.textDim, false);
        // nav
        float ny = py + 40;
        for (int i = 0; i < pages.size(); i++) {
            Page p = pages.get(i);
            boolean sel = p == page;
            boolean hv = ui.hover(px + 6, ny, SIDEBAR - 12, 17);
            sideHover[i].to(sel ? 1 : hv ? 0.5f : 0, 150, now);
            float a = sideHover[i].get(now);
            if (a > 0.01f) g.roundRect(px + 6, ny, SIDEBAR - 12, 17, 4, Colors.fade(t.surface2, a));
            if (sel) g.roundRect(px + 6, ny + 4, 2, 9, 1, t.accent);
            int col = sel ? t.text : Colors.lerp(t.textDim, t.text, a);
            Icons.draw(g, p.icon(), px + 13, ny + 3.5f, sel ? t.accent : col);
            g.text(p.title(), px + 28, ny + 4.5f, col, false);
            ny += 19;
        }
        // footer: HUD editor
        float by = py + ph - 26;
        boolean hv = ui.hover(px + 8, by, SIDEBAR - 16, 18);
        g.roundRect(px + 8, by, SIDEBAR - 16, 18, 4, hv ? Colors.lerp(t.accent, 0xFFFFFFFF, 0.15f) : t.accent);
        Icons.draw(g, "hud", px + 16, by + 4, t.onAccent);
        g.text("Edit HUD", px + 30, by + 5, t.onAccent, false);
        // close
        float cx = px + SIDEBAR - 18, cy = py + 12;
        Icons.draw(g, "close", cx, cy, ui.hover(cx - 3, cy - 3, 16, 16) ? t.bad : t.textDim);
        if (ui.hover(cx - 3, cy - 3, 16, 16)) ui.tooltip = "Close (Esc)";
        // content
        page.bounds(px + SIDEBAR + 10, py + 10, pw - SIDEBAR - 20, ph - 20);
        page.render(ui);
        g.popAlpha();
        g.pop();
    }

    private void renderTooltip(long now) {
        String tip = ui.tooltip;
        if (tip == null || tip.isEmpty()) {
            lastTooltip = null;
            return;
        }
        if (!tip.equals(lastTooltip)) {
            lastTooltip = tip;
            hoverSince = now;
        }
        if (now - hoverSince < 350) return;
        List<String> lines = Toasts.wrap(g, tip, 180);
        float w = 0;
        for (String l : lines) w = Math.max(w, g.textWidth(l));
        float h = lines.size() * 10 + 6, x = Math.min(ui.mx + 8, width - w - 12), y = ui.my + 12;
        if (y + h > height - 2) y = ui.my - h - 4;
        g.roundRect(x, y, w + 10, h, 3, ui.t.surface);
        g.roundOutline(x, y, w + 10, h, 3, 1, ui.t.border);
        for (int i = 0; i < lines.size(); i++) g.text(lines.get(i), x + 5, y + 4 + i * 10, ui.t.text, false);
    }

    // ------------------------------------------------------------------ popups / focus / capture

    public void openPopup(Widget owner, Widget p) {
        popupOwner = owner;
        popup = p;
    }

    public void closePopup() {
        popup = popupOwner = null;
    }

    public Widget popupOwner() {
        return popupOwner;
    }

    /** True where a popup covers the point (so widgets beneath do not show hover). */
    public boolean blockedAt(float x, float y) {
        return popup != null && !popup.contains(x, y);
    }

    public void registerFocusable(Widget w) {
        focusOrder.add(w);
    }

    public Widget focused() {
        return focused;
    }

    public boolean isFocused(Widget w) {
        return focused == w;
    }

    public void focus(Widget w) {
        focused = w;
    }

    public void capture(Widget w) {
        captured = w;
    }

    // ------------------------------------------------------------------ key conflicts

    /** Names of other bindings (ours or vanilla) on the same key, or null. */
    public String keyConflicts(KeySetting s) {
        int code = s.key();
        StringBuilder sb = new StringBuilder();
        if (s != k.client.openGui && k.client.openGui.key() == code) sb.append("Open menu");
        for (ModuleManager.State st : k.modules.all()) {
            for (Setting<?> set : st.module.settings()) {
                if (set == s || !(set instanceof KeySetting) || ((KeySetting) set).key() != code) continue;
                if (sb.length() > 0) sb.append(", ");
                sb.append(st.module.name()).append(": ").append(set.name());
            }
        }
        for (String[] v : vanillaBinds) {
            if (Integer.parseInt(v[1]) != code) continue;
            if (sb.length() > 0) sb.append(", ");
            sb.append("Minecraft: ").append(v[0]);
        }
        return sb.length() == 0 ? null : sb.toString();
    }

    // ------------------------------------------------------------------ input (vanilla GUI units in)

    public boolean mouseClicked(float mx, float my, int button) {
        ui.mx = mx / scale;
        ui.my = my / scale;
        try {
            if (popup != null) {
                if (popup.contains(ui.mx, ui.my)) return popup.mouseClicked(ui, button) || true;
                Widget owner = popupOwner;
                closePopup();
                if (owner != null && owner.contains(ui.mx, ui.my)) return true;
            }
            Widget before = focused;
            if (focused instanceof dev.mw19.core.gui.widget.KeybindButton
                    && ((dev.mw19.core.gui.widget.KeybindButton) focused).listening()) {
                return focused.mouseClicked(ui, button);
            }
            boolean handled;
            if (editing) {
                handled = hudEditor.mouseClicked(ui, button);
            } else {
                handled = clickMenu(button);
            }
            if (focused == before && !handled) focused = null;
            return handled;
        } catch (VirtualMachineError e) {
            throw e;
        } catch (Throwable t) {
            Log.error("GUI click failed", t);
            return true;
        }
    }

    private boolean clickMenu(int button) {
        float px = panelX(), py = panelY(), pw = panelW(), ph = panelH();
        if (!(ui.mx >= px && ui.my >= py && ui.mx < px + pw && ui.my < py + ph)) {
            return false;
        }
        if (ui.hover(px + SIDEBAR - 21, py + 9, 16, 16)) {
            close();
            return true;
        }
        float ny = py + 40;
        for (Page p : pages) {
            if (ui.hover(px + 6, ny, SIDEBAR - 12, 17)) {
                show(p);
                return true;
            }
            ny += 19;
        }
        if (ui.hover(px + 8, py + ph - 26, SIDEBAR - 16, 18)) {
            openHudEditor();
            return true;
        }
        return page.mouseClicked(ui, button);
    }

    public boolean mouseReleased(float mx, float my, int button) {
        ui.mx = mx / scale;
        ui.my = my / scale;
        Widget c = captured;
        captured = null;
        if (c != null) return Guard.run("GUI release", new ReleaseCall(c, button));
        return editing && hudEditor.mouseReleased(ui, button);
    }

    private final class ReleaseCall implements Runnable {
        final Widget w;
        final int b;

        ReleaseCall(Widget w, int b) {
            this.w = w;
            this.b = b;
        }

        @Override
        public void run() {
            w.mouseReleased(ui, b);
        }
    }

    public boolean mouseDragged(float mx, float my, int button) {
        ui.mx = mx / scale;
        ui.my = my / scale;
        try {
            if (captured != null) return captured.mouseDragged(ui, button);
            return editing && hudEditor.mouseDragged(ui, button);
        } catch (VirtualMachineError e) {
            throw e;
        } catch (Throwable t) {
            Log.error("GUI drag failed", t);
            return true;
        }
    }

    public boolean mouseScrolled(float mx, float my, double amount) {
        ui.mx = mx / scale;
        ui.my = my / scale;
        try {
            if (popup != null) return popup.mouseScrolled(ui, amount);
            if (editing) return hudEditor.mouseScrolled(ui, amount);
            return page.mouseScrolled(ui, amount);
        } catch (VirtualMachineError e) {
            throw e;
        } catch (Throwable t) {
            Log.error("GUI scroll failed", t);
            return true;
        }
    }

    /** Returns false when the key should fall through (e.g. Esc closes the screen). */
    public boolean keyPressed(int key, int mods) {
        try {
            if (focused != null && focused.keyPressed(ui, key, mods)) return true;
            if (popup != null) {
                if (key == Keys.ESCAPE) {
                    closePopup();
                    return true;
                }
                if (popup.keyPressed(ui, key, mods)) return true;
            }
            if (key == Keys.TAB) {
                cycleFocus((mods & Keys.MOD_SHIFT) != 0);
                return true;
            }
            if ((key == Keys.ENTER || key == Keys.SPACE || key == Keys.KP_ENTER) && focused != null) {
                focused.activate(ui);
                return true;
            }
            if (editing) {
                if (hudEditor.keyPressed(ui, key, mods)) return true;
                if (key == Keys.ESCAPE) {
                    closeHudEditor();
                    return true;
                }
                return false;
            }
            if (page.keyPressed(ui, key, mods)) return true;
            if (key == Keys.ESCAPE) {
                if (focused != null) {
                    focused = null;
                    return true;
                }
                return false;
            }
            return false;
        } catch (VirtualMachineError e) {
            throw e;
        } catch (Throwable t) {
            Log.error("GUI key failed", t);
            return true;
        }
    }

    public boolean charTyped(char c) {
        try {
            if (focused != null && focused.charTyped(ui, c)) return true;
            return popup != null && popup.charTyped(ui, c);
        } catch (VirtualMachineError e) {
            throw e;
        } catch (Throwable t) {
            Log.error("GUI char failed", t);
            return true;
        }
    }

    private void cycleFocus(boolean back) {
        List<Widget> order = lastFocusOrder;
        if (order.isEmpty()) return;
        int i = order.indexOf(focused);
        i = i < 0 ? (back ? order.size() - 1 : 0) : (i + (back ? -1 : 1) + order.size()) % order.size();
        focused = order.get(i);
        page.reveal(focused);
    }
}
