package dev.kestrel.core.chat;

/** One incoming chat line and what Kestrel wants done with it. Reused per platform; game thread only. */
public final class ChatLine {
    public String plain, formatted;
    /** Hide the line locally. */
    public boolean cancel;
    /** '§'-formatted text to put in front (timestamp), or null. */
    public String prefix;
    /** ARGB marker for mentions, 0 for none. */
    public int highlight;
    /** > 1: replace the previous identical line and show "(xN)". */
    public int repeat;
    /** Duplicate stacking is on: platforms that stack by line id (1.8.9) must print every line with an id. */
    public boolean track;

    public ChatLine reset(String plain, String formatted) {
        this.plain = plain == null ? "" : plain;
        this.formatted = formatted == null ? this.plain : formatted;
        cancel = false;
        prefix = null;
        highlight = 0;
        repeat = 0;
        track = false;
        return this;
    }

    public boolean modified() {
        return prefix != null || highlight != 0 || repeat > 1;
    }

    /** Suffix text for repeats, or null. */
    public String repeatSuffix() {
        return repeat > 1 ? " §7(x" + repeat + ")" : null;
    }
}
