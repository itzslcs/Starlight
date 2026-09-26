package dev.mw19.api;

/** Plugin API version. Major = breaking, minor = additive. */
public final class Mw19Api {
    /** 2.0: the package moved from dev.kestrel to dev.mw19 with the rename (DECISIONS D-019), which breaks 1.x plugins. */
    public static final int MAJOR = 2;
    public static final int MINOR = 0;
    public static final String VERSION = MAJOR + "." + MINOR;

    private Mw19Api() {}
}
