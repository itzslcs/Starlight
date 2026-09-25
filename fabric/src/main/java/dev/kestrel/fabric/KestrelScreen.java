package dev.kestrel.fabric;

import dev.kestrel.core.Kestrel;
import dev.kestrel.core.Keys;
import dev.kestrel.core.gui.GuiRoot;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
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

    /** Vanilla calls this before render(); it blurs at most once per frame, so we only choose whether to. */
    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float delta) {
        Kestrel k = Kestrel.get();
        if (k.client.blur.on() && !k.gui().isHudEditorOpen()) super.renderBackground(g, mouseX, mouseY, delta);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        root().render(backend.bind(g), width, height, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent e, boolean doubleClick) {
        return root().mouseClicked((float) e.x(), (float) e.y(), e.button());
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent e) {
        return root().mouseReleased((float) e.x(), (float) e.y(), e.button());
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent e, double dx, double dy) {
        return root().mouseDragged((float) e.x(), (float) e.y(), e.button());
    }

    @Override
    public boolean mouseScrolled(double x, double y, double h, double v) {
        return root().mouseScrolled((float) x, (float) y, v);
    }

    @Override
    public boolean keyPressed(KeyEvent e) {
        if (root().keyPressed(e.key(), e.modifiers())) return true;
        if (e.key() == Keys.ESCAPE) {
            onClose();
            return true;
        }
        return false;
    }

    @Override
    public boolean charTyped(CharacterEvent e) {
        int cp = e.codepoint();
        return Character.isBmpCodePoint(cp) && root().charTyped((char) cp);
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
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
