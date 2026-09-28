package dev.starlight.forge;

import dev.starlight.core.Starlight;
import dev.starlight.core.Keys;
import dev.starlight.core.gui.Surface;
import dev.starlight.core.gui.TitleUi;
import net.minecraft.client.gui.GuiScreen;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.io.IOException;

/**
 * 1.8.9 GuiScreen hosting a core Surface: the Starlight menu (GuiRoot) or the home screen (TitleUi). Mouse is read with
 * sub-pixel precision straight from LWJGL events.
 */
public final class StarlightGuiScreen extends GuiScreen {
    private final GuiScreen parent;
    private final Surface surface;
    private final ForgeBackend backend = new ForgeBackend();
    private boolean opened;
    private int dragButton = -1;

    /** The Starlight menu over {@code parent}. */
    public StarlightGuiScreen(GuiScreen parent) {
        this(parent, Starlight.get().gui());
    }

    public StarlightGuiScreen(GuiScreen parent, Surface surface) {
        this.parent = parent;
        this.surface = surface;
    }

    /** True for the home screen that replaced the main menu. */
    public boolean isHome() {
        return surface instanceof TitleUi;
    }

    private Surface root() {
        return surface;
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
        StarlightForge.hooks().platform().refreshResolution();
        if (surface.wantsPanorama()) ForgePanorama.render();
        root().render(backend.bind((int) Starlight.get().platform.screens().guiScale()), width, height, mx, my);
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
        if (!handled && keyCode == Keyboard.KEY_ESCAPE && surface.closesOnEscape()) close();
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
