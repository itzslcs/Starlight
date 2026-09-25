package dev.kestrel.forge;

import dev.kestrel.core.Kestrel;
import dev.kestrel.core.Keys;
import dev.kestrel.core.gui.GuiRoot;
import net.minecraft.client.gui.GuiScreen;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.io.IOException;

/** 1.8.9 GuiScreen hosting core's GuiRoot. Mouse is read with sub-pixel precision straight from LWJGL events. */
public final class KestrelGuiScreen extends GuiScreen {
    private final GuiScreen parent;
    private final ForgeBackend backend = new ForgeBackend();
    private boolean opened;
    private int dragButton = -1;

    public KestrelGuiScreen(GuiScreen parent) {
        this.parent = parent;
    }

    private GuiRoot root() {
        return Kestrel.get().gui();
    }

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        if (!opened) {
            opened = true;
            root().onOpen();
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        float mx = Mouse.getX() * width / (float) mc.displayWidth;
        float my = height - Mouse.getY() * height / (float) mc.displayHeight - 1;
        KestrelForge.hooks().platform().refreshResolution();
        root().render(backend.bind((int) Kestrel.get().platform.screens().guiScale()), width, height, mx, my);
    }

    @Override
    public void handleMouseInput() throws IOException {
        float mx = Mouse.getEventX() * width / (float) mc.displayWidth;
        float my = height - Mouse.getEventY() * height / (float) mc.displayHeight - 1;
        int button = Mouse.getEventButton();
        if (button >= 0) {
            if (Mouse.getEventButtonState()) {
                dragButton = button;
                root().mouseClicked(mx, my, button);
            } else {
                if (button == dragButton) dragButton = -1;
                root().mouseReleased(mx, my, button);
            }
        } else if (dragButton >= 0 && Mouse.isButtonDown(dragButton)) {
            root().mouseDragged(mx, my, dragButton);
        }
        int wheel = Mouse.getEventDWheel();
        if (wheel != 0) root().mouseScrolled(mx, my, wheel > 0 ? 1 : -1);
    }

    @Override
    protected void keyTyped(char c, int keyCode) throws IOException {
        int key = LwjglKeys.toGlfw(keyCode);
        int mods = (isShiftKeyDown() ? Keys.MOD_SHIFT : 0) | (isCtrlKeyDown() ? Keys.MOD_CONTROL : 0)
                | (Keyboard.isKeyDown(Keyboard.KEY_LMENU) || Keyboard.isKeyDown(Keyboard.KEY_RMENU) ? Keys.MOD_ALT : 0);
        boolean handled = key != Keys.NONE && root().keyPressed(key, mods);
        if (!handled && c >= 32 && c != 127) handled = root().charTyped(c);
        if (!handled && keyCode == Keyboard.KEY_ESCAPE) close();
    }

    public void close() {
        mc.displayGuiScreen(parent);
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
        root().onClose();
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
