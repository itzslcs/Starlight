package dev.starlight.forge;

import dev.starlight.api.render.ItemRef;
import dev.starlight.core.render.RenderBackend;
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
    private boolean batching, textBatching;
    /** The game's font atlas (a resource pack may replace its content; the location stays). */
    private static final net.minecraft.util.ResourceLocation ASCII = new net.minecraft.util.ResourceLocation("textures/font/ascii.png");

    public ForgeBackend bind(int scaleFactor) {
        this.gs = scaleFactor;
        this.screenH = mc.displayHeight;
        return this;
    }

    private void beginQuads() {
        if (batching) return;
        if (textBatching) flush();
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
        if (textBatching) {
            textBatching = false;
            Tessellator.getInstance().draw();
            GlStateManager.color(1f, 1f, 1f, 1f);
            return;
        }
        if (!batching) return;
        batching = false;
        Tessellator.getInstance().draw();
        GlStateManager.shadeModel(GL11.GL_FLAT);
        GlStateManager.enableAlpha();
        GlStateManager.enableTexture2D();
        GlStateManager.color(1f, 1f, 1f, 1f);
    }

    /**
     * Plain ASCII text goes into one textured quad batch instead of the font renderer, which issues a separate
     * immediate-mode draw per glyph (KeyCPS alone cost 253 µs/frame that way in the 2026-09-26 smoke). Glyphs come from
     * the same atlas cells and widths the font renderer uses; anything else (colour codes, other characters, the
     * Unicode font, right-to-left languages) still goes through the font renderer.
     */
    private boolean batchable(String text) {
        FontRenderer f = mc.fontRendererObj;
        if (f.getUnicodeFlag() || f.getBidiFlag()) return false;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c < 32 || c > 126) return false;
        }
        return true;
    }

    private void beginText() {
        if (textBatching) return;
        flush();
        GlStateManager.enableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.color(1f, 1f, 1f, 1f);
        mc.getTextureManager().bindTexture(ASCII);
        Tessellator.getInstance().getWorldRenderer().begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
        textBatching = true;
    }

    private void glyphs(String text, float x, float y, float k, int argb) {
        WorldRenderer wr = Tessellator.getInstance().getWorldRenderer();
        FontRenderer f = mc.fontRendererObj;
        int a = argb >>> 24, r = (argb >> 16) & 255, g = (argb >> 8) & 255, b = argb & 255;
        if (a == 0) a = 255; // like the font renderer: no alpha given means opaque
        float cx = x;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            int w = f.getCharWidth(c);
            if (c != ' ' && w > 0) {
                float u = (c % 16) * 8 / 128f, v = (c / 16) * 8 / 128f, gw = w - 1.01f, gh = 7.99f;
                float x2 = cx + gw * k, y2 = y + gh * k, u2 = u + gw / 128f, v2 = v + gh / 128f;
                wr.pos(cx, y, 0).tex(u, v).color(r, g, b, a).endVertex();
                wr.pos(cx, y2, 0).tex(u, v2).color(r, g, b, a).endVertex();
                wr.pos(x2, y2, 0).tex(u2, v2).color(r, g, b, a).endVertex();
                wr.pos(x2, y, 0).tex(u2, v).color(r, g, b, a).endVertex();
            }
            cx += w * k;
        }
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
        if (batchable(text)) {
            beginText();
            if (shadow) glyphs(text, x + scale, y + scale, scale, (argb & 0xFCFCFC) >> 2 | (argb & 0xFF000000));
            glyphs(text, x, y, scale, argb);
            return;
        }
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

    @Override
    public void image(int handle, float x1, float y1, float x2, float y2, float u0, float v0, float u1, float v1, float alpha) {
        flush();
        ForgeMedia.INSTANCE.blit(handle, x1, y1, x2, y2, u0, v0, u1, v1, alpha);
    }

    @Override
    public void player(float x1, float y1, float x2, float y2, int skin, boolean slim, float yaw, float pitch) {
        flush();
        ForgeMedia.INSTANCE.player(x1, y1, x2, y2, skin, slim, yaw, pitch);
    }
}
