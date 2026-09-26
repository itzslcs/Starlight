package dev.mw19.forge;

import dev.mw19.api.render.ItemRef;
import dev.mw19.core.render.RenderBackend;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import org.lwjgl.opengl.GL11;

/**
 * Immediate-mode backend for 1.8.9 (raw GL is fine on this target). Consecutive fills/gradients are batched
 * into one POSITION_COLOR quad draw; text, items and scissor changes flush first.
 */
public final class ForgeBackend implements RenderBackend {
    private final Minecraft mc = Minecraft.getMinecraft();
    private float gs = 1;
    private int screenH;
    private boolean batching;

    public ForgeBackend bind(int scaleFactor) {
        this.gs = scaleFactor;
        this.screenH = mc.displayHeight;
        return this;
    }

    private void beginQuads() {
        if (batching) return;
        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.disableAlpha();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.shadeModel(GL11.GL_SMOOTH);
        Tessellator.getInstance().getWorldRenderer().begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        batching = true;
    }

    @Override
    public void flush() {
        if (!batching) return;
        batching = false;
        Tessellator.getInstance().draw();
        GlStateManager.shadeModel(GL11.GL_FLAT);
        GlStateManager.enableAlpha();
        GlStateManager.enableTexture2D();
        GlStateManager.color(1f, 1f, 1f, 1f);
    }

    private static void vertex(WorldRenderer wr, float x, float y, int argb) {
        wr.pos(x, y, 0).color((argb >> 16) & 255, (argb >> 8) & 255, argb & 255, argb >>> 24).endVertex();
    }

    @Override
    public void fill(float x1, float y1, float x2, float y2, int argb) {
        gradient(x1, y1, x2, y2, argb, argb);
    }

    @Override
    public void gradient(float x1, float y1, float x2, float y2, int top, int bottom) {
        beginQuads();
        WorldRenderer wr = Tessellator.getInstance().getWorldRenderer();
        vertex(wr, x1, y2, bottom);
        vertex(wr, x2, y2, bottom);
        vertex(wr, x2, y1, top);
        vertex(wr, x1, y1, top);
    }

    @Override
    public void text(String text, float x, float y, float scale, int argb, boolean shadow) {
        flush();
        FontRenderer f = mc.fontRendererObj;
        GlStateManager.enableBlend();
        if (scale == 1f) {
            f.drawString(text, x, y, argb, shadow); // takes floats: no matrix push per string
            return;
        }
        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, 0);
        GlStateManager.scale(scale, scale, 1f);
        f.drawString(text, 0, 0, argb, shadow);
        GlStateManager.popMatrix();
    }

    @Override
    public float textWidth(String text) {
        return mc.fontRendererObj.getStringWidth(text);
    }

    @Override
    public float lineHeight() {
        return mc.fontRendererObj.FONT_HEIGHT;
    }

    @Override
    public void item(ItemRef item, float x, float y, float scale) {
        if (!(item instanceof ForgeItem)) return;
        flush();
        ForgeItem fi = (ForgeItem) item;
        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, 0);
        if (scale != 1f) GlStateManager.scale(scale, scale, 1f);
        RenderHelper.enableGUIStandardItemLighting();
        mc.getRenderItem().renderItemAndEffectIntoGUI(fi.stack, 0, 0);
        mc.getRenderItem().renderItemOverlays(mc.fontRendererObj, fi.stack, 0, 0);
        RenderHelper.disableStandardItemLighting();
        GlStateManager.popMatrix();
        GlStateManager.enableBlend();
    }

    @Override
    public void scissor(float x1, float y1, float x2, float y2) {
        flush();
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        int px = Math.round(x1 * gs), pw = Math.round((x2 - x1) * gs), ph = Math.round((y2 - y1) * gs);
        GL11.glScissor(px, screenH - Math.round(y2 * gs), Math.max(0, pw), Math.max(0, ph));
    }

    @Override
    public void endScissor() {
        flush();
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
    }

    @Override
    public float guiScale() {
        return gs;
    }
}
