package dev.kestrel.core;

import dev.kestrel.core.render.Gfx;
import dev.kestrel.core.render.RenderBackend;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;

import static org.junit.jupiter.api.Assertions.*;

/** debug-log 2026-09-26: GUI clicks measure text between render passes; that must work (it threw before). */
class GfxMeasureTest {
    private static RenderBackend fakeBackend() {
        return (RenderBackend) Proxy.newProxyInstance(RenderBackend.class.getClassLoader(), new Class<?>[]{RenderBackend.class},
                (proxy, m, args) -> {
                    switch (m.getName()) {
                        case "textWidth": return ((String) args[0]).length() * 5f;
                        case "lineHeight": return 9f;
                        case "guiScale": return 2f;
                        default:
                            Class<?> r = m.getReturnType();
                            if (r == boolean.class) return false;
                            if (r == float.class) return 0f;
                            if (r == int.class) return 0;
                            return null;
                    }
                });
    }

    @Test
    void measuresBetweenRenderPasses() {
        Gfx g = new Gfx();
        assertEquals(18f, g.textWidth("abc"), "estimate before any frame, never a crash");
        g.begin(fakeBackend(), 0);
        g.end();
        assertEquals(15f, g.textWidth("abc"));
        assertEquals(9f, g.lineHeight());
        assertEquals("abc", g.ellipsize("abc", 100));
    }
}
