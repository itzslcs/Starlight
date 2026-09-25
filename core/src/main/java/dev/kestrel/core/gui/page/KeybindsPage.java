package dev.kestrel.core.gui.page;

import dev.kestrel.api.setting.KeySetting;
import dev.kestrel.api.setting.Setting;
import dev.kestrel.core.gui.GuiRoot;
import dev.kestrel.core.gui.Ui;
import dev.kestrel.core.gui.Widget;
import dev.kestrel.core.gui.widget.KeybindButton;
import dev.kestrel.core.gui.widget.ScrollList;
import dev.kestrel.core.module.ModuleManager;

/** Every Kestrel key binding in one list; conflicts (with ours or vanilla) are outlined. */
public final class KeybindsPage extends Page {
    private final ScrollList list = new ScrollList();

    public KeybindsPage(GuiRoot root) {
        super(root);
        list.gap = 2;
    }

    @Override
    public String title() {
        return "Keybinds";
    }

    @Override
    public String icon() {
        return "keys";
    }

    @Override
    public void onShow() {
        list.clear();
        list.add(new Row("Kestrel", root.k.client.openGui));
        for (ModuleManager.State s : root.k.modules.all()) {
            if ((s.suspend() & ModuleManager.SUSPEND_UNAVAILABLE) != 0) continue;
            for (Setting<?> set : s.module.settings()) {
                if (set instanceof KeySetting) list.add(new Row(s.module.name(), (KeySetting) set));
            }
        }
    }

    @Override
    public void render(Ui ui) {
        heading(ui, "Keybinds", "Click a binding, then press a key or mouse button. Amber = also used elsewhere.");
        list.bounds(x, y + 28, w, h - 28);
        list.render(ui);
    }

    @Override
    public boolean mouseClicked(Ui ui, int button) {
        return list.mouseClicked(ui, button);
    }

    @Override
    public boolean mouseScrolled(Ui ui, double amount) {
        return list.mouseScrolled(ui, amount);
    }

    @Override
    public void reveal(Widget w) {
        list.reveal(w);
    }

    private static final class Row extends Widget {
        private final String owner;
        private final KeySetting setting;
        private final KeybindButton button;

        Row(String owner, KeySetting setting) {
            this.owner = owner;
            this.setting = setting;
            this.button = new KeybindButton(setting);
            this.h = 20;
        }

        @Override
        public void render(Ui ui) {
            ui.g.roundRect(x, y, w, h, 3, ui.t.surface);
            ui.g.text(owner, x + 7, y + 6, ui.t.textDim, false);
            ui.g.text(setting.name(), x + 7 + ui.g.textWidth(owner) + 6, y + 6, ui.t.text, false);
            button.bounds(x + w - 96, y + 2, 92, 16).render(ui);
        }

        @Override
        public boolean mouseClicked(Ui ui, int b) {
            return button.contains(ui.mx, ui.my) && button.mouseClicked(ui, b);
        }
    }
}
