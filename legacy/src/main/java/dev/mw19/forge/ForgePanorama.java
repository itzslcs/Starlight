package dev.mw19.forge;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.Project;

/**
 * The title-screen panorama for the 1.8.9 home screen: the game's six panorama textures on a slowly turning cube,
 * seen from its centre (1.8.9's own renderer is private to GuiMainMenu).
 */
final class ForgePanorama {
    private static final ResourceLocation[] FACES = new ResourceLocation[6];
    private static final long START = System.currentTimeMillis();

    static {
        for (int i = 0; i < 6; i++) FACES[i] = new ResourceLocation("textures/gui/title/background/panorama_" + i + ".png");
    }

    private ForgePanorama() {}

    static void render() {
        Minecraft mc = Minecraft.getMinecraft();
        float t = (System.currentTimeMillis() - START) / 50f; // ticks
        GlStateManager.matrixMode(GL11.GL_PROJECTION);
        GlStateManager.pushMatrix();
        GlStateManager.loadIdentity();
        Project.gluPerspective(85f, (float) mc.displayWidth / Math.max(1, mc.displayHeight), 0.05f, 10f);
        GlStateManager.matrixMode(GL11.GL_MODELVIEW);
        GlStateManager.pushMatrix();
        GlStateManager.loadIdentity();
        GlStateManager.color(1f, 1f, 1f, 1f);
        GlStateManager.rotate(180f, 1f, 0f, 0f);
        GlStateManager.rotate((float) Math.sin(t / 400f) * 6f + 12f, 1f, 0f, 0f);
        GlStateManager.rotate(-t * 0.08f, 0f, 1f, 0f);
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
        GlStateManager.disableAlpha();
        GlStateManager.disableCull();
        GlStateManager.disableDepth();
        GlStateManager.depthMask(false);
        WorldRenderer wr = Tessellator.getInstance().getWorldRenderer();
        for (int k = 0; k < 6; k++) {
            GlStateManager.pushMatrix();
            if (k == 1) GlStateManager.rotate(90f, 0f, 1f, 0f);
            else if (k == 2) GlStateManager.rotate(180f, 0f, 1f, 0f);
            else if (k == 3) GlStateManager.rotate(-90f, 0f, 1f, 0f);
            else if (k == 4) GlStateManager.rotate(90f, 1f, 0f, 0f);
            else if (k == 5) GlStateManager.rotate(-90f, 1f, 0f, 0f);
            mc.getTextureManager().bindTexture(FACES[k]);
            wr.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
            wr.pos(-1, -1, 1).tex(0, 0).endVertex();
            wr.pos(1, -1, 1).tex(1, 0).endVertex();
            wr.pos(1, 1, 1).tex(1, 1).endVertex();
            wr.pos(-1, 1, 1).tex(0, 1).endVertex();
            Tessellator.getInstance().draw();
            GlStateManager.popMatrix();
        }
        GlStateManager.depthMask(true);
        GlStateManager.enableDepth();
        GlStateManager.enableCull();
        GlStateManager.enableAlpha();
        GlStateManager.enableBlend();
        GlStateManager.matrixMode(GL11.GL_PROJECTION);
        GlStateManager.popMatrix();
        GlStateManager.matrixMode(GL11.GL_MODELVIEW);
        GlStateManager.popMatrix();
    }
}
