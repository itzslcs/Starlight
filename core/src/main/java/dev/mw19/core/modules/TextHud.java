package dev.mw19.core.modules;

import dev.mw19.api.hud.Anchor;
import dev.mw19.api.hud.HudStyle;
import dev.mw19.api.module.HudModule;
import dev.mw19.api.module.Rule;
import dev.mw19.api.render.Renderer;

/**
 * Single-line text HUD. {@link #key()} is cheap and called every frame; {@link #build()} only runs when the key
 * changes, so steady state allocates nothing.
 */
abstract class TextHud extends HudModule {
    private long lastKey = Long.MIN_VALUE;
    private String text = "";

    TextHud(String id, String name, String description, Rule rule, boolean def, Anchor anchor, float x, float y) {
        super(id, name, description, rule, def, anchor, x, y);
    }

    /** Anything that changes whenever the text would change. */
    protected abstract long key();

    protected abstract String build();

    /** Text shown in the HUD editor when there is no live value. */
    protected String previewText() {
        return build();
    }

    /** Hide when there is nothing to show (preview still draws). */
    protected boolean hasContent() {
        return true;
    }

    /** Settings changes should call this so the text rebuilds even if the key did not change. */
    protected final void invalidate() {
        lastKey = Long.MIN_VALUE;
    }

    final String current(boolean preview) {
        if (preview && !hasContent()) return previewText();
        long k = key();
        if (k != lastKey) {
            lastKey = k;
            text = build();
        }
        return text;
    }

    @Override
    public boolean visible(boolean preview) {
        return preview || hasContent();
    }

    @Override
    public float width(Renderer r) {
        return r.textWidth(current(false));
    }

    @Override
    public float height(Renderer r) {
        return r.lineHeight() - 1;
    }

    @Override
    public void render(Renderer r, HudStyle style, boolean preview) {
        r.text(current(preview), 0, 0, style.textColor(), style.textShadow());
    }
}
