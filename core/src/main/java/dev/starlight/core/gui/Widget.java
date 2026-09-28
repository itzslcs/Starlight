package dev.starlight.core.gui;

/** Base of all GUI widgets. Bounds are absolute GUI coordinates assigned by the parent's layout each frame. */
public abstract class Widget {
    public float x, y, w, h;
    public boolean visible = true;
    public String tooltip;

    public Widget bounds(float x, float y, float w, float h) {
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
        return this;
    }

    public boolean contains(float px, float py) {
        return px >= x && py >= y && px < x + w && py < y + h;
    }

    public abstract void render(Ui ui);

    public boolean mouseClicked(Ui ui, int button) {
        return false;
    }

    public boolean mouseReleased(Ui ui, int button) {
        return false;
    }

    public boolean mouseDragged(Ui ui, int button) {
        return false;
    }

    public boolean mouseScrolled(Ui ui, double amount) {
        return false;
    }

    public boolean keyPressed(Ui ui, int key, int mods) {
        return false;
    }

    public boolean charTyped(Ui ui, char c) {
        return false;
    }

    /** Keyboard navigation: can receive focus with Tab. */
    public boolean focusable() {
        return false;
    }

    /** Enter/Space while focused. */
    public void activate(Ui ui) {}

    /** Registers for Tab order and draws the focus ring; call from render() of focusable widgets. */
    protected final void focusRing(Ui ui, float radius) {
        ui.root.registerFocusable(this);
        if (ui.root.focused() == this) ui.g.roundOutline(x - 1, y - 1, w + 2, h + 2, radius + 1, 1, ui.t.accent);
    }

    protected final boolean hovered(Ui ui) {
        return ui.hover(x, y, w, h);
    }
}
