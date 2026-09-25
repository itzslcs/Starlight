package dev.kestrel.core.gui.widget;

import dev.kestrel.api.setting.BoolSetting;
import dev.kestrel.api.setting.ChoiceSetting;
import dev.kestrel.api.setting.ColorSetting;
import dev.kestrel.api.setting.KeySetting;
import dev.kestrel.api.setting.ListSetting;
import dev.kestrel.api.setting.MultiChoiceSetting;
import dev.kestrel.api.setting.NumberSetting;
import dev.kestrel.api.setting.Setting;
import dev.kestrel.api.setting.TextSetting;
import dev.kestrel.core.gui.Ui;
import dev.kestrel.core.gui.Widget;

import java.util.List;

/** Label + the right control for a Setting type. */
public class SettingRow extends Widget {
    public static final float H = 20;
    private final Setting<?> setting;
    private final Widget control;
    private final float controlW;

    public SettingRow(Setting<?> setting) {
        this.setting = setting;
        this.control = controlFor(setting);
        this.controlW = setting instanceof ListSetting ? -1 : setting instanceof NumberSetting ? 120
                : setting instanceof BoolSetting ? 24 : setting instanceof ColorSetting ? 90 : 100;
        this.control.tooltip = setting.description();
    }

    @SuppressWarnings("unchecked")
    public static Widget controlFor(final Setting<?> s) {
        if (s instanceof BoolSetting) {
            final BoolSetting b = (BoolSetting) s;
            return new Toggle(new Toggle.Model() {
                @Override
                public boolean get() {
                    return b.on();
                }

                @Override
                public void set(boolean v) {
                    b.set(v);
                }
            });
        }
        if (s instanceof NumberSetting) return new Slider((NumberSetting) s);
        if (s instanceof ChoiceSetting) {
            final ChoiceSetting c = (ChoiceSetting) s;
            return new Dropdown(new Dropdown.Model() {
                @Override
                public List<String> options() {
                    return c.options();
                }

                @Override
                public boolean selected(String o) {
                    return c.is(o);
                }

                @Override
                public void pick(String o) {
                    c.set(o);
                }

                @Override
                public String summary() {
                    return c.get();
                }
            }, false);
        }
        if (s instanceof MultiChoiceSetting) {
            final MultiChoiceSetting m = (MultiChoiceSetting) s;
            return new Dropdown(new Dropdown.Model() {
                @Override
                public List<String> options() {
                    return m.options();
                }

                @Override
                public boolean selected(String o) {
                    return m.has(o);
                }

                @Override
                public void pick(String o) {
                    m.toggle(o);
                }

                @Override
                public String summary() {
                    return m.get().isEmpty() ? "None" : m.get().size() == m.options().size() ? "All" : m.get().size() + " selected";
                }
            }, true);
        }
        if (s instanceof TextSetting) {
            final TextSetting t = (TextSetting) s;
            TextField f = new TextField(t.get());
            f.maxLength = t.maxLength();
            f.onChange = new TextField.Listener() {
                @Override
                public void changed(String text) {
                    t.set(text);
                }
            };
            return f;
        }
        if (s instanceof KeySetting) return new KeybindButton((KeySetting) s);
        if (s instanceof ColorSetting) return new ColorSwatch((ColorSetting) s);
        if (s instanceof ListSetting) return new ListEditor((ListSetting) s);
        throw new IllegalArgumentException("no control for " + s.getClass());
    }

    public float preferredHeight() {
        if (!setting.visible()) return 0;
        return control instanceof ListEditor ? 14 + ((ListEditor) control).preferredHeight() : H;
    }

    @Override
    public void render(Ui ui) {
        if (!setting.visible()) return;
        boolean hv = ui.hover(x, y, w, h);
        if (hv) ui.g.roundRect(x, y, w, h, 3, ui.t.accentSoft(0.06f));
        ui.g.text(setting.name(), x + 6, y + 6, ui.t.text, false);
        if (hv && !setting.description().isEmpty()) ui.tooltip = setting.description();
        if (controlW < 0) {
            control.bounds(x + 6, y + 16, w - 12, h - 16);
        } else {
            control.bounds(x + w - controlW - 6, y + 3, controlW, h - 6);
        }
        control.render(ui);
    }

    @Override
    public boolean mouseClicked(Ui ui, int button) {
        if (!setting.visible()) return false;
        if (control.contains(ui.mx, ui.my) && control.mouseClicked(ui, button)) return true;
        // right click on the label resets to default
        if (button == 1 && ui.mx < control.x) {
            setting.reset();
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(Ui ui, double amount) {
        return control.contains(ui.mx, ui.my) && control.mouseScrolled(ui, amount);
    }
}
