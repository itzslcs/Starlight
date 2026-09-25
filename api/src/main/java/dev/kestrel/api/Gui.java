package dev.kestrel.api;

import dev.kestrel.api.render.Renderer;

public interface Gui {
    /** Adds a page under Plugins → [title] drawn by the plugin. */
    Subscription registerPanel(String title, Panel panel);

    /** Short notification in the corner of the screen. */
    void toast(String title, String message);

    interface Panel {
        /** Draw inside (0,0)-(width,height); mouse coordinates are panel-local. */
        void render(Renderer r, float width, float height, float mouseX, float mouseY);

        default boolean mouseClicked(float mouseX, float mouseY, int button) {
            return false;
        }

        default boolean keyPressed(int key) {
            return false;
        }
    }
}
