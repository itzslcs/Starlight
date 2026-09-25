package dev.kestrel.core.gui.widget;

import dev.kestrel.core.gui.Anim;
import dev.kestrel.core.gui.Ui;
import dev.kestrel.core.gui.Widget;

import java.util.ArrayList;
import java.util.List;

/** Vertical stack of children with clipping, wheel scrolling (eased) and a scrollbar. */
public class ScrollList extends Widget {
    public final List<Widget> children = new ArrayList<Widget>();
    public float gap = 4;
    private float target;
    private final Anim scroll = new Anim(0);
    private float contentH;

    public void clear() {
        children.clear();
        target = 0;
        scroll.snap(0);
    }

    public <T extends Widget> T add(T w) {
        children.add(w);
        return w;
    }

    /** Child height: widgets report theirs through h (set before render) or SettingRow.preferredHeight(). */
    protected float heightOf(Widget c) {
        if (c instanceof SettingRow) return ((SettingRow) c).preferredHeight();
        return c.h > 0 ? c.h : 20;
    }

    @Override
    public void render(Ui ui) {
        contentH = 0;
        for (Widget c : children) if (c.visible) contentH += heightOf(c) + gap;
        float max = Math.max(0, contentH - h);
        target = Math.max(0, Math.min(max, target));
        scroll.to(target, 160, ui.now);
        float off = scroll.get(ui.now);
        ui.g.pushClip(x, y, w, h);
        float yy = y - off;
        for (Widget c : children) {
            if (!c.visible) continue;
            float ch = heightOf(c);
            c.bounds(x, yy, w - (max > 0 ? 6 : 0), ch);
            if (yy + ch >= y - 40 && yy <= y + h + 40) c.render(ui);
            yy += ch + gap;
        }
        ui.g.popClip();
        if (max > 0) {
            float bh = Math.max(16, h * h / contentH);
            float by = y + (h - bh) * (off / max);
            ui.g.roundRect(x + w - 3, by, 3, bh, 1.5f, ui.t.border);
        }
    }

    private boolean inView(Ui ui) {
        return ui.mx >= x && ui.mx < x + w && ui.my >= y && ui.my < y + h;
    }

    @Override
    public boolean mouseClicked(Ui ui, int button) {
        if (!inView(ui)) return false;
        for (int i = children.size() - 1; i >= 0; i--) {
            Widget c = children.get(i);
            if (c.visible && c.contains(ui.mx, ui.my) && c.mouseClicked(ui, button)) return true;
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(Ui ui, double amount) {
        if (!inView(ui)) return false;
        for (Widget c : children) if (c.visible && c.contains(ui.mx, ui.my) && c.mouseScrolled(ui, amount)) return true;
        target -= (float) amount * 24;
        return true;
    }

    /** Keeps a focused child visible (keyboard navigation). */
    public void reveal(Widget w) {
        if (w.y < y) target -= (y - w.y) + 4;
        else if (w.y + w.h > y + h) target += (w.y + w.h) - (y + h) + 4;
    }
}
