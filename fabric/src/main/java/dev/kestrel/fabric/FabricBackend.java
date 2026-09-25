package dev.kestrel.fabric;

import dev.kestrel.api.render.ItemRef;
import dev.kestrel.core.render.RenderBackend;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/**
 * RenderBackend over GuiGraphics. Core passes absolute GUI coordinates; sub-unit geometry is drawn in
 * physical-pixel space by scaling the pose by 1/guiScale. Bound to one GuiGraphics per pass.
 */
public final class FabricBackend implements RenderBackend {
    private GuiGraphics g;
    private Font font;
    private float gs = 1;
    private boolean scissor;

    public FabricBackend bind(GuiGraphics graphics) {
        this.g = graphics;
        Minecraft mc = Minecraft.getInstance();
        this.font = mc.font;
        this.gs = (float) mc.getWindow().getGuiScale();
        this.scissor = false;
        return this;
    }

    private static boolean whole(float v) {
        return v == (int) v;
    }

    @Override
    public void fill(float x1, float y1, float x2, float y2, int argb) {
        if (whole(x1) && whole(y1) && whole(x2) && whole(y2)) {
            g.fill((int) x1, (int) y1, (int) x2, (int) y2, argb);
            return;
        }
        push();
        scale(1f / gs);
        g.fill(Math.round(x1 * gs), Math.round(y1 * gs), Math.round(x2 * gs), Math.round(y2 * gs), argb);
        pop();
    }

    @Override
    public void gradient(float x1, float y1, float x2, float y2, int topArgb, int bottomArgb) {
        push();
        scale(1f / gs);
        g.fillGradient(Math.round(x1 * gs), Math.round(y1 * gs), Math.round(x2 * gs), Math.round(y2 * gs), topArgb, bottomArgb);
        pop();
    }

    @Override
    public void text(String text, float x, float y, float scale, int argb, boolean shadow) {
        if (scale == 1f && whole(x) && whole(y)) {
            g.drawString(font, text, (int) x, (int) y, argb, shadow);
            return;
        }
        push();
        translate(x, y);
        scale(scale);
        g.drawString(font, text, 0, 0, argb, shadow);
        pop();
    }

    @Override
    public float textWidth(String text) {
        return font.width(text);
    }

    @Override
    public float lineHeight() {
        return font.lineHeight;
    }

    @Override
    public void item(ItemRef item, float x, float y, float scale) {
        if (!(item instanceof FabricItem)) return;
        FabricItem fi = (FabricItem) item;
        push();
        translate(x, y);
        scale(scale);
        g.renderItem(fi.stack, 0, 0);
        g.renderItemDecorations(font, fi.stack, 0, 0);
        pop();
    }

    @Override
    public void scissor(float x1, float y1, float x2, float y2) {
        if (scissor) g.disableScissor();
        // enableScissor transforms by the current pose; ours is identity-at-GUI-scale here (core passes absolutes).
        g.enableScissor((int) Math.floor(x1), (int) Math.floor(y1), (int) Math.ceil(x2), (int) Math.ceil(y2));
        scissor = true;
    }

    @Override
    public void endScissor() {
        if (scissor) g.disableScissor();
        scissor = false;
    }

    @Override
    public float guiScale() {
        return gs;
    }

    // GUI pose: PoseStack up to 1.21.5, Matrix3x2fStack from 1.21.6 (deferred GUI render state).
    private void push() {
        //? if >=1.21.6 {
        g.pose().pushMatrix();
        //?} else {
        /*g.pose().pushPose();
        *///?}
    }

    private void pop() {
        //? if >=1.21.6 {
        g.pose().popMatrix();
        //?} else {
        /*g.pose().popPose();
        *///?}
    }

    private void translate(float x, float y) {
        //? if >=1.21.6 {
        g.pose().translate(x, y);
        //?} else {
        /*g.pose().translate(x, y, 0f);
        *///?}
    }

    private void scale(float s) {
        //? if >=1.21.6 {
        g.pose().scale(s, s);
        //?} else {
        /*g.pose().scale(s, s, 1f);
        *///?}
    }
}
