package dev.mw19.core.modules;

import dev.mw19.api.hud.Anchor;
import dev.mw19.api.hud.HudStyle;
import dev.mw19.api.module.HudModule;
import dev.mw19.api.module.Rule;
import dev.mw19.api.render.Renderer;
import dev.mw19.api.setting.ChoiceSetting;
import dev.mw19.core.Mw19;

public final class FpsModule extends HudModule {
    private final ChoiceSetting format = add(new ChoiceSetting("format", "Format", "How the number is shown", "123 FPS", "123 FPS", "FPS: 123", "123"));

    private int shown = -1;
    private String fmt = "";
    private String text = "0 FPS";

    public FpsModule() {
        super("fps", "FPS", "Frames per second", Rule.ALLOWED, true, Anchor.TOP_LEFT, 4, 4);
    }

    private String text() {
        int fps = Mw19.get().platform.fps();
        if (fps != shown || !fmt.equals(format.get())) {
            shown = fps;
            fmt = format.get();
            text = format.is("FPS: 123") ? "FPS: " + fps : format.is("123") ? Integer.toString(fps) : fps + " FPS";
        }
        return text;
    }

    @Override
    public float width(Renderer r) {
        return r.textWidth(text());
    }

    @Override
    public float height(Renderer r) {
        return r.lineHeight() - 1;
    }

    @Override
    public void render(Renderer r, HudStyle style, boolean preview) {
        r.text(text(), 0, 0, style.textColor(), style.textShadow());
    }
}
