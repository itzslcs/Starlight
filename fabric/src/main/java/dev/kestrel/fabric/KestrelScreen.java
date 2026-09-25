package dev.kestrel.fabric;

import dev.kestrel.core.Kestrel;
import dev.kestrel.core.Keys;
import dev.kestrel.core.gui.GuiRoot;
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

/** Vanilla Screen hosting core's GuiRoot; input and rendering are forwarded in GUI units. */
public final class KestrelScreen extends Screen {
    private final Screen parent;
    private final FabricBackend backend = new FabricBackend();
    private boolean opened;

    public KestrelScreen(Screen parent) {
        super(Component.literal(Kestrel.NAME));
        this.parent = parent;
    }

    private GuiRoot root() {
        return Kestrel.get().gui();
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
        Kestrel k = Kestrel.get();
        if (k.client.blur.on() && !k.gui().isHudEditorOpen()) super.extractBackground(g, mouseX, mouseY, delta);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        root().render(backend.bind(g), width, height, mouseX, mouseY);
    }
    *///?} else {
    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float delta) {
        Kestrel k = Kestrel.get();
        if (k.client.blur.on() && !k.gui().isHudEditorOpen()) super.renderBackground(g, mouseX, mouseY, delta);
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
        if (key == Keys.ESCAPE) {
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
}
