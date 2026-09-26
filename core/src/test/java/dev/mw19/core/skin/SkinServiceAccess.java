package dev.mw19.core.skin;

import dev.mw19.core.net.HttpClient;

/** Test access to SkinService's endpoint-taking upload (the real one only talks to Mojang). */
public final class SkinServiceAccess {
    private SkinServiceAccess() {}

    public static void upload(HttpClient http, String endpoint, String token, byte[] png, boolean slim, SkinService.Result r) {
        SkinService.upload(http, endpoint, token, png, slim, r);
    }
}
