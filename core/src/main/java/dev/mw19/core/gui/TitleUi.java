package dev.mw19.core.gui;

import dev.mw19.api.util.Colors;
import dev.mw19.core.Guard;
import dev.mw19.core.Mw19;
import dev.mw19.core.gui.widget.Button;
import dev.mw19.core.render.Gfx;
import dev.mw19.core.render.RenderBackend;

import java.util.Random;

/**
 * The MW19 home screen that replaces Minecraft's title screen (Themes → Custom home screen). It uses an original design,
 * drawn with the same Gfx as the menu, so it looks the same on 1.8.9 and 26.x. The background is a few fills plus 40 ember
 * quads per frame, with no per-pixel effects. "Vanilla menu" shows Minecraft's own title screen once, which has the
 * buttons other mods add there.
 */
public final class TitleUi implements Surface {
    private static final int EMBERS = 40;
    private static final String[] TIPS = {
        "Right Shift opens MW19 in game",
        "Edit HUD: drag, snap and scale",
        "Profiles switch per server",
    };

    private final Mw19 k;
    private final Gfx g = new Gfx();
    private final Ui ui = new Ui();
    private Item[] items;
    private final Button vanilla;
    private final Anim intro = new Anim(0);
    private final float[] ex = new float[EMBERS], speed = new float[EMBERS], phase = new float[EMBERS], size = new float[EMBERS];
    private final String footer;
    private final long start = System.currentTimeMillis();
    private float w, h;

    public TitleUi(final Mw19 k) {
        this.k = k;
        items = new Item[]{
            new Item("Singleplayer", new Runnable() {
                @Override
                public void run() {
                    k.platform.screens().openSingleplayer();
                }
            }),
            new Item("Multiplayer", new Runnable() {
                @Override
                public void run() {
                    k.platform.screens().openMultiplayer();
                }
            }),
            new Item("Options", new Runnable() {
                @Override
                public void run() {
                    k.platform.screens().openOptions();
                }
            }),
            new Item(Mw19.NAME + " Menu", new Runnable() {
                @Override
                public void run() {
                    k.openGui();
                }
            }),
            new Item("Quit Game", new Runnable() {
                @Override
                public void run() {
                    k.platform.quit();
                }
            }),
        };
        if (k.platform.screens().hasModList()) {
            Item[] withMods = java.util.Arrays.copyOf(items, items.length + 1);
            System.arraycopy(withMods, 3, withMods, 4, items.length - 3);
            withMods[3] = new Item("Mods", new Runnable() {
                @Override
                public void run() {
                    k.platform.screens().openModList();
                }
            });
            items = withMods;
        }
        vanilla = new Button("Vanilla menu", Button.Style.GHOST, new Runnable() {
            @Override
            public void run() {
                k.platform.screens().openVanillaTitle();
            }
        });
        vanilla.tooltip = "Minecraft's own title screen (buttons other mods add live there)";
        Random r = new Random(19);
        for (int i = 0; i < EMBERS; i++) {
            ex[i] = r.nextFloat();
            speed[i] = 0.012f + r.nextFloat() * 0.03f;
            phase[i] = r.nextFloat();
            size[i] = 1f + r.nextFloat() * 1.5f;
        }
        String loader = k.platform.loader();
        footer = Mw19.NAME + " " + k.modVersion + "  ·  Minecraft " + k.platform.minecraftVersion() + "  ·  "
                + (loader.isEmpty() ? loader : Character.toUpperCase(loader.charAt(0)) + loader.substring(1));
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
            ui.root = k.gui();
            ui.mx = mouseX;
            ui.my = mouseY;
            ui.now = now;
            ui.tooltip = null;
            background(now);
            intro.to(1, 450, now);
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

    private void background(long now) {
        Theme t = ui.t;
        g.gradient(0, 0, w, h, Colors.lerp(t.sidebar, 0xFF000000, 0.1f) | 0xFF000000, Colors.lerp(t.panel, 0xFF000000, 0.45f) | 0xFF000000);
        // Two soft accent washes (horizontal + vertical gradients); cheap stand-ins for radial glows.
        g.gradientH(w * 0.45f, 0, w, h, 0x00000000, Colors.fade(t.accent, 0.10f));
        g.gradient(0, h * 0.55f, w, h, 0x00000000, Colors.fade(t.accent, 0.07f));
        // Giant faint wordmark on the right.
        float ws = Math.max(4f, h / 34f);
        float ww = g.textWidth(Mw19.NAME) * ws;
        g.text(Mw19.NAME, w - ww - w * 0.04f, h * 0.10f, ws, Colors.fade(t.text, 0.08f), false);
        // Embers drifting up.
        float s = (now - start) / 1000f; // relative: epoch millis in a float have no sub-second precision
        for (int i = 0; i < EMBERS; i++) {
            float p = (phase[i] + s * speed[i]) % 1f;
            float y = h * (1.05f - p * 1.1f);
            float x = w * ex[i] + (float) Math.sin(p * 12.566f + i) * 6f;
            float a = (float) Math.sin(p * Math.PI) * 0.75f;
            g.rect(x, y, x + size[i], y + size[i], Colors.fade(t.accent, a));
        }
        g.gradient(0, h * 0.78f, w, h, 0x00000000, 0x55000000);
    }

    private void content() {
        Theme t = ui.t;
        float m = Math.max(20f, w * 0.07f);
        float scale = h < 250 ? 3f : 4f;
        float ly = Math.max(14f, h * 0.2f);
        // Wordmark: "MW" + accent "19", then a small letter-spaced "CLIENT" under an accent rule.
        String a = Mw19.NAME.substring(0, 2), b = Mw19.NAME.substring(2);
        g.text(a, m, ly, scale, t.text, true);
        g.text(b, m + g.textWidth(a) * scale, ly, scale, t.accent, true);
        float under = ly + 9 * scale + 2;
        g.roundRect(m, under, 16, 2, 1, t.accent);
        g.text("C L I E N T", m + 22, under - 3, t.textDim, false);

        float iy = under + 16, iw = 130, ih = h < 250 ? 16 : 18, gap = 4;
        for (Item it : items) {
            it.bounds(m, iy, iw, ih).render(ui);
            iy += ih + gap;
        }

        if (w >= 360) tips(t, m);

        g.text(footer, 6, h - 11, t.textDim, false);
        float vw = g.textWidth(vanilla.label) + 12;
        vanilla.bounds(w - vw - 4, h - 16, vw, 13).render(ui);
        g.text("Not affiliated with Mojang or Microsoft", 6, h - 20, 0.75f, Colors.fade(t.textDim, 0.7f), false);
    }

    private void tips(Theme t, float m) {
        float tw = 0;
        for (String tip : TIPS) tw = Math.max(tw, g.textWidth(tip));
        float cw = Math.min(tw + 26, w * 0.45f), ch = 18 + TIPS.length * 12, cx = w - cw - m, cy = h * 0.52f - ch / 2f;
        g.roundRect(cx, cy, cw, ch, 5, Colors.fade(t.surface, 0.72f));
        g.roundOutline(cx, cy, cw, ch, 5, 1, t.border);
        g.text("Tips", cx + 8, cy + 6, t.accent, false);
        float y = cy + 18;
        for (String tip : TIPS) {
            g.rect(cx + 9, y + 3, cx + 11, y + 5, t.textDim);
            g.text(g.ellipsize(tip, cw - 22), cx + 15, y, t.text, false);
            y += 12;
        }
    }

    private void tooltip(String text) {
        float tw = g.textWidth(text) + 8, tx = Math.min(ui.mx + 8, w - tw - 2), ty = Math.max(2, ui.my - 14);
        g.roundRect(tx, ty, tw, 12, 3, 0xF0101216);
        g.text(text, tx + 4, ty + 2, ui.t.text, false);
    }

    @Override
    public boolean mouseClicked(float mx, float my, int button) {
        ui.mx = mx;
        ui.my = my;
        for (Item it : items) if (it.mouseClicked(ui, button)) return true;
        return vanilla.mouseClicked(ui, button);
    }

    @Override
    public boolean mouseReleased(float mx, float my, int button) {
        return false;
    }

    @Override
    public boolean mouseDragged(float mx, float my, int button) {
        return false;
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
    }

    @Override
    public void onClose() {}

    @Override
    public boolean closesOnEscape() {
        return false;
    }

    @Override
    public boolean wantsVanillaBackground() {
        return false;
    }

    /** A left-aligned menu entry: an accent bar grows and the label slides on hover. */
    private final class Item extends Widget {
        private final String label;
        private final Runnable action;
        private final Anim hover = new Anim(0);

        Item(String label, Runnable action) {
            this.label = label;
            this.action = action;
        }

        @Override
        public void render(Ui ui) {
            hover.to(hovered(ui) ? 1 : 0, 140, ui.now);
            float t = hover.get(ui.now);
            ui.g.roundRect(x, y, w, h, 3, Colors.fade(ui.t.surface2, 0.35f + 0.55f * t));
            ui.g.roundRect(x, y + 3, 2 + 2 * t, h - 6, 1, Colors.lerp(Colors.fade(ui.t.accent, 0.55f), ui.t.accent, t));
            ui.g.text(label, x + 10 + 3 * t, y + (h - 8) / 2f, Colors.lerp(Colors.lerp(ui.t.textDim, ui.t.text, 0.6f), ui.t.text, t), false);
        }

        @Override
        public boolean mouseClicked(Ui ui, int button) {
            if (button != 0 || !contains(ui.mx, ui.my)) return false;
            Guard.run("home: " + label, action);
            return true;
        }
    }
}
