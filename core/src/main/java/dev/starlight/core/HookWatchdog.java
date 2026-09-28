package dev.starlight.core;

/** Records which platform hooks have fired, so a silently failed mixin shows up in the UI and the smoke log. */
public final class HookWatchdog {
    public volatile boolean tick, hud, key, mouse, screenButton, chat, serverJoin;
    /** A vanilla screen drew the Starlight backdrop (only with Starlight game menus on; the smoke checks it on the pause menu). */
    public volatile boolean backdrop;
    /** A nametag was drawn with a Tier Tagger tag (the smoke checks it on the player's own nametag). */
    public volatile boolean nameTag;

    public String missing(boolean inWorld) {
        StringBuilder sb = new StringBuilder();
        if (!tick) sb.append("tick ");
        if (inWorld && !hud) sb.append("hud ");
        if (!screenButton) sb.append("menu-button ");
        return sb.toString().trim();
    }

    public String describe() {
        return "tick=" + tick + " hud=" + hud + " key=" + key + " mouse=" + mouse + " menuButton=" + screenButton
                + " chat=" + chat + " serverJoin=" + serverJoin + " backdrop=" + backdrop + " nameTag=" + nameTag;
    }
}
