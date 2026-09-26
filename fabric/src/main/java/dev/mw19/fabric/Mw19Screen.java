package dev.mw19.fabric;

import dev.mw19.core.Mw19;
import dev.mw19.core.Keys;
import dev.mw19.core.gui.Surface;
import dev.mw19.core.gui.TitleUi;
//? if >=26.1 {
/*import net.minecraft.client.gui.GuiGraphicsExtractor;
*///?} else {
import net.minecraft.client.gui.GuiGraphics;
//?}
import net.minecraft.client.gui.screens.Screen;
//? if >=1.21.9 {
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
//?}
import net.minecraft.network.chat.Component;

/**
 * Vanilla Screen hosting a core {@link Surface}: the MW19 menu (GuiRoot) or the home screen (TitleUi). Input and
 * rendering are forwarded in GUI units, and every version-specific override lives here once.
 */
public final class Mw19Screen extends Screen {
    private final Screen parent;
    private final Surface surface;
    private final FabricBackend backend = new FabricBackend();
    private boolean opened;

    /** The MW19 menu over {@code parent}. */
    public Mw19Screen(Screen parent) {
        this(parent, Mw19.get().gui());
    }

    public Mw19Screen(Screen parent, Surface surface) {
        super(Component.literal(Mw19.NAME));
        this.parent = parent;
        this.surface = surface;
    }

    /** True for the home screen that replaced the title screen. */
    public boolean isHome() {
        return surface instanceof TitleUi;
    }

    private Surface root() {
        return surface;
    }

    @Override
    protected void init() {
        if (!opened) {
            opened = true;
            root().onOpen();
        }
    }

    /**
     * Blur/menu background only when the user wants it. From 1.21.6 vanilla calls this before render() (and blurs at most
     * once per frame); up to 1.21.5 Screen.render() called it, so our render() does that itself below.
     */
    //? if >=26.1 {
    /*@Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        if (surface.wantsVanillaBackground()) super.extractBackground(g, mouseX, mouseY, delta);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        root().render(backend.bind(g), width, height, mouseX, mouseY);
    }
    *///?} else {
    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float delta) {
        if (surface.wantsVanillaBackground()) super.renderBackground(g, mouseX, mouseY, delta);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        //? if <1.21.6
        /*renderBackground(g, mouseX, mouseY, delta);*/
        root().render(backend.bind(g), width, height, mouseX, mouseY);
    }
    //?}

    //? if >=1.21.9 {
    @Override
    public boolean mouseClicked(MouseButtonEvent e, boolean doubleClick) {
        return root().mouseClicked((float) e.x(), (float) e.y(), FabricCompat.button(e.button()));
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent e) {
        return root().mouseReleased((float) e.x(), (float) e.y(), FabricCompat.button(e.button()));
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent e, double dx, double dy) {
        return root().mouseDragged((float) e.x(), (float) e.y(), FabricCompat.button(e.button()));
    }
    //?} else {
    /*@Override
    public boolean mouseClicked(double x, double y, int button) {
        return root().mouseClicked((float) x, (float) y, button);
    }

    @Override
    public boolean mouseReleased(double x, double y, int button) {
        return root().mouseReleased((float) x, (float) y, button);
    }

    @Override
    public boolean mouseDragged(double x, double y, int button, double dx, double dy) {
        return root().mouseDragged((float) x, (float) y, button);
    }
    *///?}

    @Override
    public boolean mouseScrolled(double x, double y, double h, double v) {
        return root().mouseScrolled((float) x, (float) y, v);
    }

    //? if >=1.21.9 {
    @Override
    public boolean keyPressed(KeyEvent e) {
        return key(FabricCompat.key(e.key()), FabricCompat.mods(e.modifiers()));
    }

    @Override
    public boolean charTyped(CharacterEvent e) {
        int cp = e.codepoint();
        return Character.isBmpCodePoint(cp) && root().charTyped((char) cp);
    }
    //?} else {
    /*@Override
    public boolean keyPressed(int key, int scancode, int modifiers) {
        return key(key, modifiers);
    }

    @Override
    public boolean charTyped(char c, int modifiers) {
        return root().charTyped(c);
    }
    *///?}

    private boolean key(int key, int mods) {
        if (root().keyPressed(key, mods)) return true;
        if (key == Keys.ESCAPE && surface.closesOnEscape()) {
            onClose();
            return true;
        }
        return false;
    }

    @Override
    public void onClose() {
        FabricCompat.setScreen(minecraft, parent);
    }

    @Override
    public void removed() {
        root().onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return surface.closesOnEscape();
    }
}
