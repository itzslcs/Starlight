package dev.mw19.core.gui;

import dev.mw19.api.util.Colors;
import dev.mw19.core.Guard;
import dev.mw19.core.Mw19;
import dev.mw19.core.gui.page.PacksPage;
import dev.mw19.core.gui.page.SkinsPage;
import dev.mw19.core.render.Gfx;
import dev.mw19.core.render.RenderBackend;

import java.util.ArrayList;
import java.util.List;

/**
 * The MW19 home screen that replaces Minecraft's title screen (Themes → Custom home screen): the MW19 backdrop
 * (vanilla's panorama with "MW19 game menus" off), a pixel-block logo, the usual buttons as keycaps in the middle, the
 * player's skin on the left (drag to turn it) and MW19's own screens on the right. "Vanilla menu" shows Minecraft's own
 * title screen once, where buttons from other mods live.
 */
public final class TitleUi implements Surface {
    /** Pixel logo, 7 rows per glyph ('#' = block). Drawn from rects: no bundled image (DECISIONS D-009). */
    private static final String[][] GLYPHS = {
        {"#.....#", "##...##", "#.#.#.#", "#..#..#", "#.....#", "#.....#", "#.....#"},
        {"#.....#", "#.....#", "#.....#", "#..#..#", "#.#.#.#", "##...##", "#.....#"},
        {".##", "###", ".##", ".##", ".##", ".##", "####"},
        {".###.", "#...#", "#...#", ".####", "....#", "#...#", ".###."},
    };

    private final Mw19 k;
    private final Gfx g = new Gfx();
    private final Ui ui = new Ui();
    private final List<HomeButton> main = new ArrayList<HomeButton>(), side = new ArrayList<HomeButton>();
    private final HomeButton options, quit, skins, vanilla;
    private final Anim intro = new Anim(0);
    private long openedAt;
    private final String footer;
    private float w, h;
    private float modelX, modelY, modelW, modelH, yaw = 25, lastX;
    private boolean turning;

    public TitleUi(final Mw19 k) {
        this.k = k;
        main.add(new HomeButton("Singleplayer", new Runnable() {
            @Override
            public void run() {
                k.platform.screens().openSingleplayer();
            }
        }));
        main.add(new HomeButton("Multiplayer", new Runnable() {
            @Override
            public void run() {
                k.platform.screens().openMultiplayer();
            }
        }));
        if (k.platform.screens().hasModList()) {
            main.add(new HomeButton("Mods", new Runnable() {
                @Override
                public void run() {
                    k.platform.screens().openModList();
                }
            }));
        }
        options = new HomeButton("Options…", new Runnable() {
            @Override
            public void run() {
                k.platform.screens().openOptions();
            }
        });
        quit = new HomeButton("Quit Game", new Runnable() {
            @Override
            public void run() {
                k.platform.quit();
            }
        });
        side.add(new HomeButton("Host World", new Runnable() {
            @Override
            public void run() {
                k.hostWhenWorldOpens = true;
                k.platform.screens().openSingleplayer();
            }
        }).tip("Pick a world; it opens to friends once it has loaded"));
        side.add(new HomeButton("Packs", new Runnable() {
            @Override
            public void run() {
                k.gui().openPage(PacksPage.class);
            }
        }).tip("Find and install resource packs from Modrinth"));
        side.add(new HomeButton(Mw19.NAME + " Menu", new Runnable() {
            @Override
            public void run() {
                k.openGui();
            }
        }).tip("Mods, HUD, profiles and settings (Right Shift in game)"));
        skins = new HomeButton("Skins", new Runnable() {
            @Override
            public void run() {
                SkinsPage.open(k);
            }
        });
        vanilla = new HomeButton("Vanilla menu", new Runnable() {
            @Override
            public void run() {
                k.platform.screens().openVanillaTitle();
            }
        }).tip("Minecraft's own title screen (buttons other mods add live there)");
        vanilla.flat = true;
        String loader = k.platform.loader();
        footer = Mw19.NAME + " " + k.modVersion + " · Minecraft " + k.platform.minecraftVersion()
                + (loader.isEmpty() ? "" : " · " + Character.toUpperCase(loader.charAt(0)) + loader.substring(1));
    }

    @Override
    public void render(RenderBackend backend, int screenW, int screenH, float mouseX, float mouseY) {
        long now = System.currentTimeMillis();
        w = screenW;
        h = screenH;
        g.begin(backend, now);
        try {
            ui.g = g;
            ui.t = k.theme;
            ui.k = k;
            ui.root = null; // the home screen has no popups; never build the menu just to draw it
            ui.mx = mouseX;
            ui.my = mouseY;
            ui.now = now;
            ui.tooltip = null;
            if (k.client.styleMenus.on()) MenuStyle.backdrop(g, w, h, false, MenuStyle.glow(k.theme.accent), now, Anim.speed <= 0);
            else g.gradient(0, h * 0.62f, w, h, 0x00000000, 0x7A000000); // over the panorama: shade where text sits
            intro.to(1, 350, now);
            g.pushAlpha(intro.get(now));
            content();
            g.popAlpha();
            k.toasts.render(g, ui.t, w, now);
            if (ui.tooltip != null) tooltip(ui.tooltip);
        } catch (VirtualMachineError e) {
            throw e;
        } catch (Throwable t) {
            dev.mw19.core.Log.error("home screen render failed", t);
        } finally {
            g.end();
        }
    }

    private void content() {
        float px = Math.max(2, Math.min(5, (float) Math.floor(h / 55f)));
        float logoY = Math.max(8, h * 0.09f), lp = stagger(0);
        g.pushAlpha(lp);
        logo(w / 2f, logoY - (1 - lp) * 10, px);
        g.popAlpha();
        float bh = h < 230 ? 18 : 20, gap = 4, bw = Math.min(200, w * 0.42f);
        float cx = w / 2f - bw / 2f, top = Math.max(logoY + 7 * px + px + 18, h * 0.36f);
        float yy = top;
        int n = 0;
        for (HomeButton b : main) {
            enter(b, n++, cx, yy, bw, bh, -14);
            yy += bh + gap;
        }
        float half = (bw - gap) / 2f;
        yy += 8;
        enter(options, n, cx, yy, half, bh, -14);
        enter(quit, n++, cx + half + gap, yy, half, bh, 14);
        boolean wide = w >= 400;
        if (wide) {
            float sw = Math.min(104, (w - bw) / 2f - 24), sx = w - sw - Math.max(12, w * 0.05f);
            float sy = top;
            int j = 1;
            for (HomeButton b : side) {
                enter(b, j++, sx, sy, sw, bh, 16);
                sy += bh + gap;
            }
            float modelTop = logoY; // the logo is centred, so the left column can start level with it
            g.pushAlpha(stagger(3));
            player(Math.max(12, w * 0.05f), modelTop, Math.min(104, (w - bw) / 2f - 24), yy + bh - modelTop);
            g.popAlpha();
        } else {
            // Narrow window: MW19's screens become a second row of small buttons.
            float sy = yy + bh + gap, sw = (bw - gap * (side.size() - 1)) / side.size();
            for (int i = 0; i < side.size(); i++) side.get(i).bounds(cx + i * (sw + gap), sy, sw, bh).render(ui);
        }
        g.text(footer, 4, h - 11, 0xFFE0E0E0, true);
        float vw = g.textWidth(vanilla.label) + 10;
        vanilla.bounds(w - vw - 3, h - 14, vw, 12).render(ui);
    }

    /** The player's model with their name above it and the Skins button below. */
    private void player(float x, float y, float colW, float colH) {
        String name = k.platform.playerName();
        float nameH = 12, btnH = 18;
        modelX = x;
        modelY = y + nameH;
        modelW = colW;
        modelH = colH - nameH - btnH - 6;
        if (modelH < 40) return;
        if (name != null && !name.isEmpty()) g.textCentered(name, x + colW / 2f, y, 0xFFFFFFFF, true);
        g.player(modelX, modelY, modelW, modelH, 0, k.platform.skins().ownSkinSlim(), yaw, -5);
        float sw = Math.min(colW, 80);
        skins.bounds(x + (colW - sw) / 2f, modelY + modelH + 6, sw, btnH).render(ui);
    }

    private void logo(float cx, float y, float px) {
        float total = 0;
        for (String[] gl : GLYPHS) total += gl[0].length() * px + px;
        total -= px;
        float x = cx - total / 2f, depth = Math.max(1, px / 2f);
        for (int gi = 0; gi < GLYPHS.length; gi++) {
            String[] gl = GLYPHS[gi];
            boolean accent = gi >= 2;
            for (int pass = 0; pass < 3; pass++) {
                for (int r = 0; r < gl.length; r++) {
                    for (int c = 0; c < gl[r].length(); c++) {
                        if (gl[r].charAt(c) != '#') continue;
                        float bx = x + c * px, by = y + r * px;
                        if (pass == 0) {
                            g.rect(bx - 1, by - 1, bx + px + depth + 1, by + px + depth + 1, 0xFF101010); // outline
                        } else if (pass == 1) {
                            g.rect(bx + depth, by + depth, bx + px + depth, by + px + depth, accent ? Colors.lerp(ui.t.accent, 0xFF000000, 0.55f) : 0xFF4A4A4A);
                        } else {
                            float shade = r / 6f;
                            int face = accent ? Colors.lerp(Colors.lerp(ui.t.accent, 0xFFFFFFFF, 0.25f), ui.t.accent, shade)
                                    : Colors.lerp(0xFFF4F4F4, 0xFFB8B8B8, shade);
                            g.rect(bx, by, bx + px, by + px, face);
                        }
                    }
                }
            }
            x += gl[0].length() * px + px;
        }
    }

    private void tooltip(String text) {
        float tw = g.textWidth(text) + 8, tx = Math.min(ui.mx + 8, w - tw - 2), ty = Math.max(2, ui.my - 14);
        g.rect(tx, ty, tx + tw, ty + 12, 0xF0101010);
        g.text(text, tx + 4, ty + 2, 0xFFFFFFFF, false);
    }

    private List<HomeButton> all() {
        List<HomeButton> l = new ArrayList<HomeButton>(main);
        l.add(options);
        l.add(quit);
        l.addAll(side);
        l.add(skins);
        l.add(vanilla);
        return l;
    }

    @Override
    public boolean mouseClicked(float mx, float my, int button) {
        ui.mx = mx;
        ui.my = my;
        for (HomeButton b : all()) if (b.visible && b.mouseClicked(ui, button)) return true;
        if (button == 0 && mx >= modelX && my >= modelY && mx < modelX + modelW && my < modelY + modelH) {
            turning = true;
            lastX = mx;
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseReleased(float mx, float my, int button) {
        turning = false;
        return false;
    }

    @Override
    public boolean mouseDragged(float mx, float my, int button) {
        if (!turning) return false;
        yaw += (mx - lastX) * 2.5f;
        lastX = mx;
        return true;
    }

    @Override
    public boolean mouseScrolled(float mx, float my, double amount) {
        return false;
    }

    @Override
    public boolean keyPressed(int key, int mods) {
        return false;
    }

    @Override
    public boolean charTyped(char c) {
        return false;
    }

    @Override
    public void onOpen() {
        intro.snap(0);
        openedAt = System.currentTimeMillis();
    }

    /** Staggered entrance: element {@code i} eases in 45 ms after the one before (1 when animations are off). */
    private float stagger(int i) {
        if (Anim.speed <= 0) return 1;
        float t = ((ui.now - openedAt) / Anim.speed - 60 - i * 45) / 260f;
        if (t >= 1) return 1;
        if (t <= 0) return 0;
        return 1 - (1 - t) * (1 - t) * (1 - t);
    }

    private void enter(HomeButton b, int i, float x, float y, float w, float h, float dx) {
        float p = stagger(i);
        g.pushAlpha(p);
        b.bounds(x + (1 - p) * dx, y, w, h).render(ui);
        g.popAlpha();
    }

    @Override
    public void onClose() {
        turning = false;
    }

    @Override
    public boolean closesOnEscape() {
        return false;
    }

    @Override
    public boolean wantsVanillaBackground() {
        return false;
    }

    /** Vanilla's panorama only with "MW19 game menus" off; otherwise the MW19 backdrop is drawn in {@link #render}. */
    @Override
    public boolean wantsPanorama() {
        return !k.client.styleMenus.on();
    }

    /** A keycap button (MenuStyle); {@code flat} draws text only (the Vanilla menu link). */
    private static final class HomeButton extends Widget {
        final String label;
        private final Runnable action;
        private final Anim hover = new Anim(0);
        boolean flat;

        HomeButton(String label, Runnable action) {
            this.label = label;
            this.action = action;
        }

        HomeButton tip(String t) {
            tooltip = t;
            return this;
        }

        @Override
        public void render(Ui ui) {
            boolean hv = ui.mx >= x && ui.my >= y && ui.mx < x + w && ui.my < y + h;
            hover.to(hv ? 1 : 0, 110, ui.now);
            float t = hover.get(ui.now);
            if (flat) {
                ui.g.textCentered(label, x + w / 2f, y + (h - 8) / 2f, Colors.lerp(0xFFC8C8C8, 0xFFFFFFFF, t), true);
                if (t > 0.01f) ui.g.rect(x + 5, y + h - 2, x + w - 5, y + h - 1, Colors.fade(0xFFFFFFFF, t));
            } else {
                MenuStyle.key(ui.g, x, y, w, h, t, true, MenuStyle.glow(ui.t.accent));
                ui.g.textCentered(label, x + w / 2f, y + (h - 8) / 2f, Colors.lerp(MenuStyle.LABEL, MenuStyle.LABEL_HOVER, t), true);
            }
            if (hv && tooltip != null) ui.tooltip = tooltip;
        }

        @Override
        public boolean mouseClicked(Ui ui, int button) {
            if (button != 0 || !contains(ui.mx, ui.my)) return false;
            Guard.run("home: " + label, action);
            return true;
        }
    }
}
