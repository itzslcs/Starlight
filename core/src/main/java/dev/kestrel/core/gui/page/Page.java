package dev.kestrel.core.gui.page;

import dev.kestrel.core.gui.GuiRoot;
import dev.kestrel.core.gui.Ui;
import dev.kestrel.core.gui.Widget;

public abstract class Page extends Widget {
    protected final GuiRoot root;

    protected Page(GuiRoot root) {
        this.root = root;
    }

    public abstract String title();

    public abstract String icon();

    /** Page became visible (rebuild lists here). */
    public void onShow() {}

    /** Scroll so the keyboard-focused widget is visible. */
    public void reveal(Widget w) {}

    protected void heading(Ui ui, String text, String sub) {
        ui.g.text(text, x, y, 1.25f, ui.t.text, false);
        if (sub != null) ui.g.text(ui.g.ellipsize(sub, w), x, y + 13, ui.t.textDim, false);
    }
}
