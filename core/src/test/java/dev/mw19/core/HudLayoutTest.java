package dev.mw19.core;

import dev.mw19.api.hud.Anchor;
import dev.mw19.core.hud.HudElement;
import dev.mw19.core.hud.HudLayout;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HudLayoutTest {
    private static final float SW = 400, SH = 240;

    @Test
    void anchorsMeasureInwards() {
        HudElement e = new HudElement(Anchor.TOP_LEFT, 4, 6);
        e.padding = 0;
        HudLayout.place(e, 20, 10, SW, SH);
        assertEquals(4, e.bx, 1e-4);
        assertEquals(6, e.by, 1e-4);
        e.anchor = Anchor.BOTTOM_RIGHT;
        HudLayout.place(e, 20, 10, SW, SH);
        assertEquals(SW - 20 - 4, e.bx, 1e-4);
        assertEquals(SH - 10 - 6, e.by, 1e-4);
        e.anchor = Anchor.CENTER;
        e.x = e.y = 0;
        HudLayout.place(e, 20, 10, SW, SH);
        assertEquals(SW / 2 - 10, e.bx, 1e-4);
        assertEquals(SH / 2 - 5, e.by, 1e-4);
    }

    @Test
    void scaleAndPaddingDefineTheBox() {
        HudElement e = new HudElement(Anchor.TOP_LEFT, 0, 0);
        e.padding = 3;
        e.scale = 2;
        HudLayout.place(e, 10, 5, SW, SH);
        assertEquals(32, e.bw, 1e-4);
        assertEquals(22, e.bh, 1e-4);
    }

    @Test
    void clampsToScreen() {
        HudElement e = new HudElement(Anchor.TOP_LEFT, 1000, -50);
        e.padding = 0;
        HudLayout.place(e, 20, 10, SW, SH);
        assertEquals(SW - 20, e.bx, 1e-4);
        assertEquals(0, e.by, 1e-4);
    }

    @Test
    void moveToIsInverseOfPlaceForEveryAnchorAndMode() {
        for (Anchor a : Anchor.values()) {
            for (boolean pct : new boolean[]{false, true}) {
                HudElement e = new HudElement(a, 0, 0);
                e.autoAnchor = false;
                e.percent = pct;
                HudLayout.place(e, 30, 12, SW, SH);
                HudLayout.moveTo(e, 123, 77, SW, SH);
                HudLayout.place(e, 30, 12, SW, SH);
                assertEquals(123, e.bx, 1e-3, a + " pct=" + pct);
                assertEquals(77, e.by, 1e-3, a + " pct=" + pct);
            }
        }
    }

    @Test
    void percentOffsetsFollowScreenSize() {
        HudElement e = new HudElement(Anchor.TOP_LEFT, 0, 0);
        e.autoAnchor = false;
        e.percent = true;
        e.padding = 0;
        HudLayout.place(e, 10, 10, SW, SH);
        HudLayout.moveTo(e, 100, 60, SW, SH); // 25% / 25%
        HudLayout.place(e, 10, 10, SW * 2, SH * 2);
        assertEquals(200, e.bx, 1e-3);
        assertEquals(120, e.by, 1e-3);
    }

    @Test
    void autoAnchorPicksScreenThird() {
        assertEquals(Anchor.TOP_LEFT, HudLayout.nearestAnchor(10, 10, SW, SH));
        assertEquals(Anchor.BOTTOM_RIGHT, HudLayout.nearestAnchor(390, 230, SW, SH));
        assertEquals(Anchor.CENTER, HudLayout.nearestAnchor(200, 120, SW, SH));
        HudElement e = new HudElement(Anchor.TOP_LEFT, 0, 0);
        e.padding = 0;
        HudLayout.place(e, 20, 10, SW, SH);
        HudLayout.moveTo(e, 370, 220, SW, SH);
        assertEquals(Anchor.BOTTOM_RIGHT, e.anchor);
    }

    @Test
    void snapsEdgesCentresAndReportsGuide() {
        float[] guides = {0, 200, 400};
        float[] hit = new float[1];
        assertEquals(200, HudLayout.snap1(197, 30, guides, 3, 4, hit), 1e-4);
        assertEquals(200, hit[0], 1e-4);
        assertEquals(170, HudLayout.snap1(172, 30, guides, 3, 4, hit), 1e-4); // right edge to 200
        assertEquals(185, HudLayout.snap1(187, 30, guides, 3, 4, hit), 1e-4); // centre to 200
        assertEquals(150, HudLayout.snap1(150, 30, guides, 3, 4, hit), 1e-4);
        assertTrue(Float.isNaN(hit[0]));
        assertEquals(16, HudLayout.grid(14.1f, 8), 1e-4);
    }

    @Test
    void jsonRoundTripClampsGarbage() {
        HudElement e = new HudElement(Anchor.LEFT, 5, 6);
        e.scale = 1.5f;
        e.bgColor = 0x80FF0000;
        HudElement copy = new HudElement(Anchor.TOP_LEFT, 0, 0);
        copy.fromJson(e.toJson());
        assertEquals(Anchor.LEFT, copy.anchor);
        assertEquals(1.5f, copy.scale, 1e-6);
        assertEquals(0x80FF0000, copy.bgColor);
        java.util.Map<String, Object> bad = e.toJson();
        bad.put("scale", 99.0);
        bad.put("anchor", "NOPE");
        copy.fromJson(bad);
        assertEquals(4f, copy.scale, 1e-6);
        assertEquals(Anchor.LEFT, copy.anchor);
    }
}
