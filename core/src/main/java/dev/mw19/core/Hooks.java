package dev.mw19.core;

/**
 * Values written by modules on the game thread and read by platform mixins every frame. Plain fields on purpose:
 * a mixin's per-frame cost is one field read, and nothing here can throw. Defaults = vanilla behaviour.
 */
public final class Hooks {
    private Hooks() {}

    /** FOV multiplier source (zoom), or null for vanilla. */
    public interface FovSource {
        float multiplier(long nowMs);
    }

    public static FovSource zoom;

    /** Called by the FOV mixin every frame. */
    public static float fov() {
        FovSource z = zoom;
        return z == null ? 1f : z.multiplier(System.currentTimeMillis());
    }
    /** Fullbright: gamma to use instead of the option, or NaN for vanilla. */
    public static double gamma = Double.NaN;
    /** Custom crosshair active: hide the vanilla one. */
    public static boolean hideCrosshair;
    /** Entity Culling module: the platform's entity pass asks {@code Mw19.get().occlusion} before drawing. */
    public static boolean entityCulling;

    /** Entity Culling's block entity part: chests, signs, banners, heads... fully hidden behind blocks are not drawn. */
    public static boolean blockEntityCulling;
    /** Fast Chests: the chest models are in (the platform sets it); their block entity renderer is skipped. */
    public static boolean fastChests;
    /** Clear Weather module: the client level reports no rain or thunder. */
    public static boolean clearWeather;
    /** Marlow's Crystal Optimizer (built in): a crystal a hit breaks is hidden at once. Volatile: the network thread
     *  reads it when it decodes a server's message. Keep render: still drawn until the server removes it. */
    public static volatile boolean crystalOptimizer;
    public static boolean crystalKeepRender;
    /** Hero's Anchor Optimizer (built in): a charged anchor that will explode becomes a ghost block at once. */
    public static boolean anchorOptimizer;

    /** Exploit Protection: resolve sign/anvil text as an unmodded client would; refuse local resource pack URLs. */
    public static volatile boolean guardText, guardPackUrls;
    /** Render your own nametag in third person. */
    public static boolean ownNametag;
    /** Extra copies of crit / enchanted-hit particles (0 = vanilla). */
    public static int extraHitParticles;
    /** Fire overlay: move down by this many screen units (0 = vanilla) and draw at this opacity (1 = vanilla). */
    public static float fireOffset, fireOpacity = 1f;
    /** Shield overlay: lower a raised shield by this amount (0 = vanilla). */
    public static float shieldOffset;
    /** Hurt overlay colour (ARGB), 0 = vanilla red. */
    public static int hitColor;
    /** Damage tilt multiplier (1 = vanilla). */
    public static float damageTilt = 1f;
    /** 1.8-style visuals: keep the held item steady after attacking (no cooldown dip). */
    public static boolean noCooldownDip;
    /** Freelook: when active the camera uses these angles and mouse movement turns the camera, not the player. */
    public static boolean freelook, freelookInvert;
    public static float freelookYaw, freelookPitch;

    /**
     * Mouse turn while freelook is active: the platform calls this with the same deltas it would have passed to
     * Entity.turn (1.21+/26.x) or Entity.setAngles (1.8.9) and skips turning the player.
     */
    public static void freelookTurn(double dYaw, double dPitch) {
        freelookYaw += (float) dYaw * 0.15f;
        float dp = (float) dPitch * 0.15f * (freelookInvert ? -1 : 1);
        freelookPitch = Math.max(-90f, Math.min(90f, freelookPitch + dp));
    }
}
