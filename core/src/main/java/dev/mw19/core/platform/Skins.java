package dev.mw19.core.platform;

/** Textures from PNG bytes and the signed-in account's skin (game thread). */
public interface Skins {
    /** Decodes a PNG into a texture. Returns a handle > 0, or 0 when the bytes are not a readable PNG. */
    int loadImage(byte[] png);

    void releaseImage(int handle);

    /** Whether the signed-in account's own skin uses the slim (3 px arm) model. */
    boolean ownSkinSlim();

    /**
     * The session's access token, only for the official skin endpoint (api.minecraftservices.com) when the player asks
     * to change their skin. Null when there is none (offline or demo). Never log, store or pass it on.
     */
    String accessToken();

    /** Answer of {@link #refreshOwnSkin}, on the game thread. */
    interface Refreshed {
        void done(boolean shown);
    }

    /**
     * After a skin change: the game still holds the profile it fetched at start, so the player would keep seeing the
     * old skin until a restart. This fetches the profile again until Mojang serves the skin with {@code texture} (null:
     * any skin other than the current one), then shows it on the player in menus and in the world. Default: cannot.
     */
    default void refreshOwnSkin(String texture, Refreshed done) {
        done.done(false);
    }
}
