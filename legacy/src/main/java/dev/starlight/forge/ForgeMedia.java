package dev.starlight.forge;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import dev.starlight.core.platform.Packs;
import dev.starlight.core.platform.Skins;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelPlayer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.client.resources.ResourcePackRepository;
import net.minecraft.client.resources.SkinManager;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** 1.8.9: textures from PNG bytes, the player model drawn from ModelPlayer's parts, and resource packs. Game thread. */
final class ForgeMedia implements Skins, Packs {
    static final ForgeMedia INSTANCE = new ForgeMedia();

    private final Minecraft mc = Minecraft.getMinecraft();
    private final Map<Integer, Tex> textures = new HashMap<Integer, Tex>();
    private int next = 1;
    private ModelPlayer wide, slim;
    private ResourceLocation ownSkin;
    private boolean ownSlim, ownRequested;

    private static final class Tex {
        final ResourceLocation id;

        Tex(ResourceLocation id) {
            this.id = id;
        }
    }

    private ForgeMedia() {}

    @Override
    public int loadImage(byte[] png) {
        if (png == null || png.length < 8 || (png[0] & 0xFF) != 0x89 || png[1] != 'P' || png[2] != 'N' || png[3] != 'G') return 0;
        try {
            BufferedImage img = TextureUtil.readBufferedImage(new ByteArrayInputStream(png));
            if (img == null) return 0;
            int handle = next++;
            ResourceLocation id = mc.getTextureManager().getDynamicTextureLocation("starlight_" + handle, new DynamicTexture(img));
            textures.put(handle, new Tex(id));
            return handle;
        } catch (Exception e) {
            return 0;
        }
    }

    @Override
    public void releaseImage(int handle) {
        Tex t = textures.remove(handle);
        if (t != null) mc.getTextureManager().deleteTexture(t.id);
    }

    void blit(int handle, float x1, float y1, float x2, float y2, float u0, float v0, float u1, float v1, float alpha) {
        Tex t = textures.get(handle);
        if (t == null) return;
        mc.getTextureManager().bindTexture(t.id);
        GlStateManager.enableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.color(1f, 1f, 1f, alpha);
        WorldRenderer wr = Tessellator.getInstance().getWorldRenderer();
        wr.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
        wr.pos(x1, y2, 0).tex(u0, v1).endVertex();
        wr.pos(x2, y2, 0).tex(u1, v1).endVertex();
        wr.pos(x2, y1, 0).tex(u1, v0).endVertex();
        wr.pos(x1, y1, 0).tex(u0, v0).endVertex();
        Tessellator.getInstance().draw();
        GlStateManager.color(1f, 1f, 1f, 1f);
    }

    // ------------------------------------------------------------------ skins

    /** The signed-in player's skin, from the profile the launcher passed in (the default skin until it loads). */
    private ResourceLocation ownSkin() {
        if (!ownRequested) {
            ownRequested = true;
            GameProfile p = mc.getSession().getProfile();
            ownSkin = DefaultPlayerSkin.getDefaultSkin(p.getId());
            ownSlim = "slim".equals(DefaultPlayerSkin.getSkinType(p.getId()));
            mc.getSkinManager().loadProfileTextures(p, new SkinManager.SkinAvailableCallback() {
                @Override
                public void skinAvailable(MinecraftProfileTexture.Type type, ResourceLocation location, MinecraftProfileTexture texture) {
                    if (type != MinecraftProfileTexture.Type.SKIN) return;
                    ownSkin = location;
                    ownSlim = "slim".equals(texture.getMetadata("model"));
                }
            }, false);
        }
        return ownSkin;
    }

    @Override
    public boolean ownSkinSlim() {
        ownSkin();
        return ownSlim;
    }

    private ModelPlayer model(boolean small) {
        ModelPlayer m = small ? slim : wide;
        if (m == null) {
            m = new ModelPlayer(0f, small);
            m.bipedRightArm.rotateAngleZ = m.bipedRightArmwear.rotateAngleZ = 0.1f; // arms a little away from the body
            m.bipedLeftArm.rotateAngleZ = m.bipedLeftArmwear.rotateAngleZ = -0.1f;
            if (small) slim = m;
            else wide = m;
        }
        return m;
    }

    /** The model standing in the rect (feet on its bottom edge), turned by yaw and pitch. */
    void player(float x1, float y1, float x2, float y2, int handle, boolean small, float yaw, float pitch) {
        ResourceLocation tex;
        boolean s = small;
        if (handle == 0) {
            tex = ownSkin();
            s = ownSlim;
        } else {
            Tex t = textures.get(handle);
            if (t == null) return;
            tex = t.id;
        }
        ModelPlayer m = model(s);
        float block = (y2 - y1) / 2f * 0.94f; // the model is two blocks tall
        mc.getTextureManager().bindTexture(tex);
        GlStateManager.pushMatrix();
        GlStateManager.translate((x1 + x2) / 2f, y2 - 1.5f * block, 100f);
        GlStateManager.scale(block, block, block);
        GlStateManager.translate(0f, 0.5f, 0f);
        GlStateManager.rotate(-pitch, 1f, 0f, 0f);
        GlStateManager.rotate(yaw, 0f, 1f, 0f);
        GlStateManager.translate(0f, -0.5f, 0f);
        GlStateManager.color(1f, 1f, 1f, 1f);
        GlStateManager.enableDepth();
        GlStateManager.enableRescaleNormal();
        GlStateManager.enableAlpha();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        RenderHelper.enableStandardItemLighting();
        float px = 0.0625f;
        m.bipedHead.render(px);
        m.bipedBody.render(px);
        m.bipedRightArm.render(px);
        m.bipedLeftArm.render(px);
        m.bipedRightLeg.render(px);
        m.bipedLeftLeg.render(px);
        m.bipedHeadwear.render(px);
        m.bipedBodyWear.render(px);
        m.bipedRightArmwear.render(px);
        m.bipedLeftArmwear.render(px);
        m.bipedRightLegwear.render(px);
        m.bipedLeftLegwear.render(px);
        RenderHelper.disableStandardItemLighting();
        GlStateManager.disableRescaleNormal();
        GlStateManager.popMatrix();
        GlStateManager.clear(GL11.GL_DEPTH_BUFFER_BIT); // later GUI (tooltips) must draw over the model
    }

    @Override
    public String accessToken() {
        String t = mc.getSession().getToken();
        if (t == null || !t.startsWith("eyJ")) return null;
        int dots = 0;
        for (int i = 0; i < t.length(); i++) if (t.charAt(i) == '.') dots++;
        return dots == 2 ? t : null;
    }

    // ------------------------------------------------------------------ resource packs

    @Override
    public Path folder() {
        return mc.getResourcePackRepository().getDirResourcepacks().toPath();
    }

    @Override
    public List<String> enabled() {
        List<String> out = new ArrayList<String>();
        for (ResourcePackRepository.Entry e : mc.getResourcePackRepository().getRepositoryEntries()) out.add(0, e.getResourcePackName());
        return out;
    }

    /** The repository list runs bottom to top; the new pack goes last (on top), then resources reload. */
    @Override
    public boolean enable(String fileName) {
        ResourcePackRepository repo = mc.getResourcePackRepository();
        repo.updateRepositoryEntriesAll();
        ResourcePackRepository.Entry found = null;
        for (ResourcePackRepository.Entry e : repo.getRepositoryEntriesAll()) if (e.getResourcePackName().equals(fileName)) found = e;
        if (found == null) return false;
        List<ResourcePackRepository.Entry> list = new ArrayList<ResourcePackRepository.Entry>();
        for (ResourcePackRepository.Entry e : repo.getRepositoryEntries()) if (!e.getResourcePackName().equals(fileName)) list.add(e);
        list.add(found);
        repo.setRepositories(list);
        mc.gameSettings.resourcePacks.clear();
        for (ResourcePackRepository.Entry e : list) mc.gameSettings.resourcePacks.add(e.getResourcePackName());
        mc.gameSettings.saveOptions();
        mc.refreshResources();
        return true;
    }
}
