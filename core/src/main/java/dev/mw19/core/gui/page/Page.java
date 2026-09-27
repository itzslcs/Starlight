package dev.mw19.core.gui.page;

import dev.mw19.core.gui.GuiRoot;
import dev.mw19.core.gui.Ui;
import dev.mw19.core.gui.Widget;

public abstract class Page extends Widget {
    protected final GuiRoot root;

    protected Page(GuiRoot root) {
        this.root = root;
    }

    public abstract String title();

    public abstract String icon();

    /** Page became visible (rebuild lists here). */
    public void onShow() {}

    /** The menu closed and is being unloaded: free textures and anything else heavy. */
    public void dispose() {}

    /** Files dropped onto the window while this page is shown. */
    public void filesDropped(java.util.List<java.nio.file.Path> files) {}

    /** Scroll so the keyboard-focused widget is visible. */
    public void reveal(Widget w) {}

    protected void heading(Ui ui, String text, String sub) {
        ui.g.text(text, x, y, 1.25f, ui.t.text, false);
        if (sub != null) ui.g.text(ui.g.ellipsize(sub, w), x, y + 13, ui.t.textDim, false);
    }
}
