package dev.kestrel.core.gui.widget;

import dev.kestrel.api.setting.ListSetting;
import dev.kestrel.core.gui.Icons;
import dev.kestrel.core.gui.Ui;
import dev.kestrel.core.gui.Widget;

import java.util.ArrayList;
import java.util.List;

/** Edits a ListSetting: rows with remove buttons plus an add field. Height grows with entries. */
public class ListEditor extends Widget {
    public static final float ROW = 13;
    private final ListSetting s;
    private final TextField add = new TextField("");
    private final Button addBtn;

    public ListEditor(final ListSetting s) {
        this.s = s;
        add.placeholder = "Add entry…";
        final Runnable doAdd = new Runnable() {
            @Override
            public void run() {
                String v = add.text.trim();
                if (v.isEmpty()) return;
                List<String> n = new ArrayList<String>(s.get());
                n.add(v);
                s.set(n);
                add.setText("");
            }
        };
        add.onSubmit = doAdd;
        addBtn = new Button("Add", Button.Style.SECONDARY, doAdd);
    }

    public float preferredHeight() {
        return s.get().size() * ROW + 18;
    }

    @Override
    public void render(Ui ui) {
        List<String> items = s.get();
        float yy = y;
        for (int i = 0; i < items.size(); i++) {
            boolean hv = ui.hover(x, yy, w, ROW);
            if (hv) ui.g.roundRect(x, yy, w, ROW, 2, ui.t.surface2);
            ui.g.text(ui.g.ellipsize(items.get(i), w - 20), x + 4, yy + 2.5f, ui.t.text, false);
            Icons.draw(ui.g, "close", x + w - 12, yy + 1.5f, ui.hover(x + w - 14, yy, 14, ROW) ? ui.t.bad : ui.t.textDim);
            yy += ROW;
        }
        add.bounds(x, yy + 2, w - 38, 14).render(ui);
        addBtn.bounds(x + w - 34, yy + 2, 34, 14).render(ui);
    }

    @Override
    public boolean mouseClicked(Ui ui, int button) {
        if (!contains(ui.mx, ui.my)) return false;
        List<String> items = s.get();
        int i = (int) ((ui.my - y) / ROW);
        if (i >= 0 && i < items.size() && ui.mx >= x + w - 14) {
            List<String> n = new ArrayList<String>(items);
            n.remove(i);
            s.set(n);
            return true;
        }
        return add.mouseClicked(ui, button) || addBtn.mouseClicked(ui, button);
    }
}
