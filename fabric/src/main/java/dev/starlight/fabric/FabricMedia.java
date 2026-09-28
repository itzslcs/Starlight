package dev.starlight.fabric;

import com.mojang.blaze3d.platform.NativeImage;
import dev.starlight.core.platform.Packs;
import dev.starlight.core.platform.Skins;
import dev.starlight.fabric.mixin.PlayerSkinWidgetAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.PlayerSkinWidget;
//? if >=26.1 {
/*import net.minecraft.client.gui.GuiGraphicsExtractor;
*///?} else {
import net.minecraft.client.gui.GuiGraphics;
//?}
import net.minecraft.client.renderer.texture.DynamicTexture;
//? if >=1.21.11 {
import net.minecraft.resources.Identifier;
//?} else {
/*import net.minecraft.resources.ResourceLocation;
*///?}
//? if >=1.21.9 {
import net.minecraft.core.ClientAsset;
import net.minecraft.world.entity.player.PlayerModelType;
import net.minecraft.world.entity.player.PlayerSkin;
//?} else {
/*import net.minecraft.client.resources.PlayerSkin;
*///?}
import net.minecraft.server.packs.repository.PackRepository;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Textures from PNG bytes, the player model (vanilla's PlayerSkinWidget, as on the skin report screen) and resource
 * packs, for core. Game thread only.
 */
public final class FabricMedia implements Skins, Packs {
    public static final FabricMedia INSTANCE = new FabricMedia();

    private final Minecraft mc = Minecraft.getInstance();
    private final Map<Integer, Tex> textures = new HashMap<>();
    private int next = 1;
    private PlayerSkinWidget widget;
    private PlayerSkin shown;
    private Supplier<PlayerSkin> own;
    private long ownAt;

    private static final class Tex {
        //? if >=1.21.11 {
        final Identifier id;
        //?} else {
        /*final ResourceLocation id;
        *///?}
        final int w, h;
        PlayerSkin wide, slim;

        //? if >=1.21.11 {
        Tex(Identifier id, int w, int h) {
        //?} else {
        /*Tex(ResourceLocation id, int w, int h) {
        *///?}
            this.id = id;
            this.w = w;
            this.h = h;
        }
    }

    private FabricMedia() {}

    // ------------------------------------------------------------------ textures

    @Override
    public int loadImage(byte[] png) {
        NativeImage img = null;
        try {
            img = NativeImage.read(png);
            int handle = next++;
            String path = "dynamic/" + handle;
            //? if >=1.21.11 {
            Identifier id = Identifier.fromNamespaceAndPath("starlight", path);
            //?} else {
            /*ResourceLocation id = ResourceLocation.fromNamespaceAndPath("starlight", path);
            *///?}
            int w = img.getWidth(), h = img.getHeight();
            //? if >=1.21.5 {
            DynamicTexture tex = new DynamicTexture(() -> "starlight " + path, img);
            //?} else {
            /*DynamicTexture tex = new DynamicTexture(img);
            *///?}
            img = null; // owned by the texture now
            mc.getTextureManager().register(id, tex);
            textures.put(handle, new Tex(id, w, h));
            return handle;
        } catch (Exception e) {
            if (img != null) img.close();
            return 0;
        }
    }

    @Override
    public void releaseImage(int handle) {
        Tex t = textures.remove(handle);
        if (t != null) mc.getTextureManager().release(t.id);
    }

    //? if >=26.1 {
    /*void blit(GuiGraphicsExtractor g, int handle, float x1, float y1, float x2, float y2, float u0, float v0, float u1, float v1, float alpha) {
    *///?} else {
    void blit(GuiGraphics g, int handle, float x1, float y1, float x2, float y2, float u0, float v0, float u1, float v1, float alpha) {
    //?}
        Tex t = textures.get(handle);
        if (t == null) return;
        int x = Math.round(x1), y = Math.round(y1), w = Math.round(x2) - x, h = Math.round(y2) - y;
        if (w <= 0 || h <= 0) return;
        float u = u0 * t.w, v = v0 * t.h;
        int uw = Math.round((u1 - u0) * t.w), vh = Math.round((v1 - v0) * t.h);
        //? if >=1.21.6 {
        int color = Math.round(Math.max(0, Math.min(1, alpha)) * 255) << 24 | 0xFFFFFF;
        g.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, t.id, x, y, u, v, w, h, uw, vh, t.w, t.h, color);
        //?} elif >=1.21.2 {
        /*int color = Math.round(Math.max(0, Math.min(1, alpha)) * 255) << 24 | 0xFFFFFF;
        g.blit(net.minecraft.client.renderer.RenderType::guiTextured, t.id, x, y, u, v, w, h, uw, vh, t.w, t.h, color);
        *///?} else {
        /*com.mojang.blaze3d.systems.RenderSystem.enableBlend();
        com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1f, 1f, 1f, alpha);
        g.blit(t.id, x, y, w, h, u, v, uw, vh, t.w, t.h);
        com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        *///?}
    }

    // ------------------------------------------------------------------ skins

    private PlayerSkin ownSkin() {
        long now = System.currentTimeMillis();
        if (own == null || now - ownAt > 1000) { // the profile (with textures) arrives from Mojang a moment after start
            ownAt = now;
            //? if >=1.21.9 {
            own = mc.getSkinManager().createLookup(mc.getGameProfile(), false);
            //?} else {
            /*own = mc.getSkinManager().lookupInsecure(mc.getGameProfile());
            *///?}
        }
        return own.get();
    }

    private PlayerSkin custom(int handle, boolean slim) {
        Tex t = textures.get(handle);
        if (t == null) return null;
        if (slim ? t.slim == null : t.wide == null) {
            //? if >=1.21.9 {
            PlayerSkin s = PlayerSkin.insecure(new ClientAsset.ResourceTexture(t.id, t.id), null, null, slim ? PlayerModelType.SLIM : PlayerModelType.WIDE);
            //?} else {
            /*PlayerSkin s = new PlayerSkin(t.id, null, null, null, slim ? PlayerSkin.Model.SLIM : PlayerSkin.Model.WIDE, false);
            *///?}
            if (slim) t.slim = s;
            else t.wide = s;
        }
        return slim ? t.slim : t.wide;
    }

    @Override
    public boolean ownSkinSlim() {
        try {
            PlayerSkin s = ownSkin();
            //? if >=1.21.9 {
            return s != null && s.model() == PlayerModelType.SLIM;
            //?} else {
            /*return s != null && s.model() == PlayerSkin.Model.SLIM;
            *///?}
        } catch (RuntimeException e) {
            return false;
        }
    }

    /** The signed-in player's face and hat at (0, 0), 8 units wide (the backend scales it), as the tab list draws it. */
    //? if >=26.1 {
    /*void face(GuiGraphicsExtractor g) {
    *///?} else {
    void face(GuiGraphics g) {
    //?}
        PlayerSkin skin = ownSkin();
        if (skin == null) return;
        //? if >=26.1 {
        /*net.minecraft.client.gui.components.PlayerFaceExtractor.extractRenderState(g, skin, 0, 0, 8);
        *///?} else {
        net.minecraft.client.gui.components.PlayerFaceRenderer.draw(g, skin, 0, 0, 8);
        //?}
    }

    /** The model in the rect (vanilla's widget fits it to the height), turned by yaw/pitch. */
    //? if >=26.1 {
    /*void player(GuiGraphicsExtractor g, float x1, float y1, float x2, float y2, int handle, boolean slim, float yaw, float pitch) {
    *///?} else {
    void player(GuiGraphics g, float x1, float y1, float x2, float y2, int handle, boolean slim, float yaw, float pitch) {
    //?}
        PlayerSkin skin = handle == 0 ? ownSkin() : custom(handle, slim);
        if (skin == null) return;
        shown = skin;
        int x = Math.round(x1), y = Math.round(y1), w = Math.round(x2) - x, h = Math.round(y2) - y;
        if (w <= 0 || h <= 0) return;
        if (widget == null) widget = new PlayerSkinWidget(w, h, mc.getEntityModels(), () -> shown);
        widget.setX(x);
        widget.setY(y);
        widget.setWidth(w);
        widget.setHeight(h);
        PlayerSkinWidgetAccessor a = (PlayerSkinWidgetAccessor) widget;
        a.starlight$setRotationX(pitch);
        a.starlight$setRotationY(yaw);
        //? if >=26.1 {
        /*widget.extractRenderState(g, -1, -1, 0f);
        *///?} else {
        widget.render(g, -1, -1, 0f);
        //?}
    }

    // ------------------------------------------------------------------ own skin after a change

    @Override
    public void refreshOwnSkin(final String texture, final Refreshed done) {
        final java.util.UUID id = mc.getUser().getProfileId();
        final String before = skinTexture(mc.getGameProfile());
        if (texture != null && texture.equals(before)) { // the skin it already had: nothing to load
            done.done(true);
            return;
        }
        Thread t = new Thread(() -> {
            // Mojang's session server takes a moment to serve a new skin. The signed fetch skips authlib's cache.
            for (int i = 0; i < 12; i++) {
                try {
                    Thread.sleep(i == 0 ? 1000 : 5000);
                    Object result = fetchProfile(id);
                    String now = result == null ? null : skinTexture(profileOf(result));
                    if (now != null && (texture != null ? texture.equals(now) : !now.equals(before))) {
                        mc.execute(() -> done.done(useProfile(result)));
                        return;
                    }
                } catch (InterruptedException e) {
                    break;
                } catch (RuntimeException e) {
                    dev.starlight.core.Log.warn("skin refresh: " + e); // offline for a moment: try again
                }
            }
            mc.execute(() -> done.done(false));
        }, "Starlight skin refresh");
        t.setDaemon(true);
        t.start();
    }

    private Object fetchProfile(java.util.UUID id) {
        //? if >=1.21.9 {
        return mc.services().sessionService().fetchProfile(id, true);
        //?} else {
        /*return mc.getMinecraftSessionService().fetchProfile(id, true);
        *///?}
    }

    private static com.mojang.authlib.GameProfile profileOf(Object result) {
        //? if >=26.3 {
        /*return ((com.mojang.authlib.services.ProfileResult) result).profile();
        *///?} else {
        return ((com.mojang.authlib.yggdrasil.ProfileResult) result).profile();
        //?}
    }

    private static String skinTexture(com.mojang.authlib.GameProfile p) {
        if (p == null) return null;
        //? if >=1.21.9 {
        for (com.mojang.authlib.properties.Property prop : p.properties().get("textures")) return dev.starlight.core.skin.SkinService.skinTexture(prop.value());
        //?} else {
        /*for (com.mojang.authlib.properties.Property prop : p.getProperties().get("textures")) return dev.starlight.core.skin.SkinService.skinTexture(prop.value());
        *///?}
        return null;
    }

    /** Game thread: the fetched profile becomes the game's own (Starlight's previews, the next world) and the player's list
     *  entry looks its skin up again (the player in this world). False when an accessor did not apply. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private boolean useProfile(Object result) {
        try {
            ((java.util.concurrent.CompletableFuture) ((dev.starlight.fabric.mixin.MinecraftProfileAccessor) mc).starlight$profileFuture()).obtrudeValue(result);
            own = null;
            net.minecraft.client.multiplayer.ClientPacketListener c = mc.getConnection();
            net.minecraft.client.multiplayer.PlayerInfo info = c == null ? null : c.getPlayerInfo(mc.getUser().getProfileId());
            if (info != null) {
                ((dev.starlight.fabric.mixin.PlayerInfoAccessor) info).starlight$setSkinLookup(dev.starlight.fabric.mixin.PlayerInfoAccessor.starlight$createSkinLookup(profileOf(result)));
            }
            return true;
        } catch (RuntimeException e) {
            dev.starlight.core.Log.error("skin refresh: could not put the new profile in", e);
            return false;
        }
    }

    /**
     * Smoke: a fresh profile goes in (the game's profile and the player's list entry change), then the start state goes
     * back. The start value can be null: an offline test account has no profile, and the game then makes one per call.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    String selfTest() {
        try {
            java.util.concurrent.CompletableFuture future = ((dev.starlight.fabric.mixin.MinecraftProfileAccessor) mc).starlight$profileFuture();
            Object start = future.getNow(null);
            com.mojang.authlib.GameProfile copy = new com.mojang.authlib.GameProfile(mc.getUser().getProfileId(), mc.getUser().getName());
            //? if >=26.3 {
            /*Object fresh = new com.mojang.authlib.services.ProfileResult(copy);
            *///?} else {
            Object fresh = new com.mojang.authlib.yggdrasil.ProfileResult(copy);
            //?}
            net.minecraft.client.multiplayer.PlayerInfo info = mc.getConnection() == null ? null : mc.getConnection().getPlayerInfo(mc.getUser().getProfileId());
            if (info == null) return "FAIL (no player list entry for the player)";
            dev.starlight.fabric.mixin.PlayerInfoAccessor entry = (dev.starlight.fabric.mixin.PlayerInfoAccessor) info;
            java.util.function.Supplier lookup = entry.starlight$skinLookup();
            boolean in = useProfile(fresh) && mc.getGameProfile() == copy && entry.starlight$skinLookup() != lookup;
            future.obtrudeValue(start);
            entry.starlight$setSkinLookup(lookup);
            boolean back = future.getNow(null) == start && entry.starlight$skinLookup() == lookup;
            return in && back ? "ok (profile and player list entry swapped and restored)" : "FAIL (in=" + in + " back=" + back + ")";
        } catch (RuntimeException e) {
            return "FAIL (" + e + ")";
        }
    }

    /**
     * The session token for the official skin endpoint. Only a Microsoft-account token (a JWT) is returned, so offline
     * and development sessions get a clear message instead of a failed upload.
     */
    @Override
    public String accessToken() {
        String t = mc.getUser().getAccessToken();
        return t != null && t.startsWith("eyJ") && t.chars().filter(c -> c == '.').count() == 2 ? t : null;
    }

    // ------------------------------------------------------------------ resource packs

    @Override
    public Path folder() {
        return mc.getResourcePackDirectory();
    }

    @Override
    public List<String> enabled() {
        List<String> out = new ArrayList<>();
        for (String id : mc.getResourcePackRepository().getSelectedIds()) if (id.startsWith("file/")) out.add(0, id.substring(5));
        return out;
    }

    @Override
    public boolean enable(String fileName) {
        PackRepository repo = mc.getResourcePackRepository();
        repo.reload();
        String id = "file/" + fileName;
        if (!repo.isAvailable(id)) return false;
        repo.removePack(id); // re-adding puts it on top
        repo.addPack(id);
        mc.options.updateResourcePacks(repo);
        return true;
    }
}
