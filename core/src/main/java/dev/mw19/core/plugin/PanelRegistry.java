package dev.mw19.core.plugin;

import dev.mw19.api.Gui;
import dev.mw19.api.Subscription;

import java.util.ArrayList;
import java.util.List;

/** Panels plugins add under the Plugins page. */
public final class PanelRegistry {
    public static final class Item {
        public final String title;
        public final Gui.Panel panel;

        Item(String title, Gui.Panel panel) {
            this.title = title;
            this.panel = panel;
        }
    }

    private final List<Item> items = new ArrayList<Item>();

    public synchronized Subscription add(String title, Gui.Panel panel) {
        final Item it = new Item(title, panel);
        items.add(it);
        return new Subscription() {
            @Override
            public void cancel() {
                synchronized (PanelRegistry.this) {
                    items.remove(it);
                }
            }
        };
    }

    public synchronized List<Item> all() {
        return new ArrayList<Item>(items);
    }
}
