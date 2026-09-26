package dev.mw19.core.skin;

import dev.mw19.api.net.Http;
import dev.mw19.api.util.Json;
import dev.mw19.core.net.HttpClient;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Mojang's official skin endpoints: changing the signed-in account's skin, and copying another player's current skin
 * into the library. Only runs when the player clicks the matching button (README "Network use").
 */
public final class SkinService {
    static final String UPLOAD = "https://api.minecraftservices.com/minecraft/profile/skins";
    private static final Pattern NAME = Pattern.compile("[A-Za-z0-9_]{3,16}");

    public interface Result {
        void done(boolean ok, String message);
    }

    public interface Fetched {
        void done(byte[] png, boolean slim, String error);
    }

    private SkinService() {}

    /** Sets the account's skin. {@code token} is the session token (null → a message explains why it cannot). */
    public static void upload(HttpClient http, String token, byte[] png, boolean slim, Result result) {
        upload(http, UPLOAD, token, png, slim, result);
    }

    static void upload(HttpClient http, String endpoint, String token, byte[] png, boolean slim, final Result result) {
        if (token == null || token.isEmpty()) {
            result.done(false, "Sign in with a Microsoft account to change your skin.");
            return;
        }
        if (!SkinLibrary.isSkin(png)) {
            result.done(false, "That file is not a 64×64 PNG skin.");
            return;
        }
        String boundary = "MW19" + Long.toHexString(System.nanoTime());
        Map<String, String> headers = new HashMap<String, String>();
        headers.put("Authorization", "Bearer " + token);
        headers.put("Content-Type", "multipart/form-data; boundary=" + boundary);
        headers.put("Accept", "application/json");
        http.request("POST", endpoint, headers, multipart(boundary, slim ? "slim" : "classic", png), 1 << 16, new HttpClient.BytesCallback() {
            @Override
            public void done(int status, byte[] body, String error) {
                if (status == 200) result.done(true, "Skin changed. Other players see it after you rejoin a server.");
                else if (status == 401 || status == 403) result.done(false, "Your login has expired. Restart the game from your launcher and try again.");
                else if (status == 400) result.done(false, "Mojang did not accept this skin.");
                else if (status == 429) result.done(false, "Too many skin changes. Wait a minute and try again.");
                else if (status == 0) result.done(false, "Could not reach Mojang (" + error + ").");
                else result.done(false, "Mojang answered HTTP " + status + ".");
            }
        });
    }

    static byte[] multipart(String boundary, String variant, byte[] png) {
        String head = "--" + boundary + "\r\nContent-Disposition: form-data; name=\"variant\"\r\n\r\n" + variant + "\r\n"
                + "--" + boundary + "\r\nContent-Disposition: form-data; name=\"file\"; filename=\"skin.png\"\r\nContent-Type: image/png\r\n\r\n";
        byte[] h = head.getBytes(StandardCharsets.UTF_8), t = ("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8);
        byte[] out = new byte[h.length + png.length + t.length];
        System.arraycopy(h, 0, out, 0, h.length);
        System.arraycopy(png, 0, out, h.length, png.length);
        System.arraycopy(t, 0, out, h.length + png.length, t.length);
        return out;
    }

    public static boolean validName(String name) {
        return name != null && NAME.matcher(name).matches();
    }

    /** Downloads the current skin of the player called {@code name} (Mojang profile lookup, then the texture). */
    public static void fetch(final HttpClient http, String name, final Fetched out) {
        if (!validName(name)) {
            out.done(null, false, "Player names are 3-16 letters, digits or _");
            return;
        }
        http.getJson("https://api.mojang.com/users/profiles/minecraft/" + name, 60_000, new Http.Callback() {
            @Override
            public void done(int status, Object json, String error) {
                String id = Json.str(Json.obj(json), "id", null);
                if (status == 404 || status == 204 || (status == 200 && id == null)) {
                    out.done(null, false, "No player with that name");
                    return;
                }
                if (id == null || !id.matches("[0-9a-fA-F]{32}")) {
                    out.done(null, false, error == null ? "Mojang answered HTTP " + status : error);
                    return;
                }
                profile(http, id, out);
            }
        });
    }

    private static void profile(final HttpClient http, String id, final Fetched out) {
        http.getJson("https://sessionserver.mojang.com/session/minecraft/profile/" + id, 60_000, new Http.Callback() {
            @Override
            public void done(int status, Object json, String error) {
                String url = null;
                boolean slim = false;
                for (Object o : Json.arr(Json.obj(json).get("properties"))) {
                    Map<String, Object> p = Json.obj(o);
                    if (!"textures".equals(Json.str(p, "name", ""))) continue;
                    try {
                        String decoded = new String(java.util.Base64.getDecoder().decode(Json.str(p, "value", "")), StandardCharsets.UTF_8);
                        Map<String, Object> skin = Json.obj(Json.obj(Json.obj(Json.parse(decoded)).get("textures")).get("SKIN"));
                        url = Json.str(skin, "url", null);
                        slim = "slim".equals(Json.str(Json.obj(skin.get("metadata")), "model", ""));
                    } catch (IllegalArgumentException e) {
                        url = null;
                    }
                }
                if (url == null) {
                    out.done(null, false, error != null ? error : "That player uses the default skin");
                    return;
                }
                if (url.startsWith("http://textures.minecraft.net/")) url = "https://" + url.substring(7);
                if (!url.startsWith("https://textures.minecraft.net/")) {
                    out.done(null, false, "Unexpected texture address");
                    return;
                }
                final boolean isSlim = slim;
                http.request("GET", url, null, null, SkinLibrary.MAX_BYTES, new HttpClient.BytesCallback() {
                    @Override
                    public void done(int status, byte[] body, String error) {
                        if (status == 200 && SkinLibrary.isSkin(body)) out.done(body, isSlim, null);
                        else out.done(null, false, error != null ? error : "The skin file could not be read");
                    }
                });
            }
        });
    }
}
