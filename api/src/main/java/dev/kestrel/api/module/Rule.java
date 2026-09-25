package dev.kestrel.api.module;

/** Verdict against the Hypixel Allowed Modifications policy (docs/RULES_MATRIX.md). */
public enum Rule {
    /** Clearly inside a permitted category. */
    ALLOWED,
    /** Not clearly inside a category: default-off, disabled by competitive-safe. */
    GRAY,
    /** Outside the policy on some servers: default-off, force-disabled there by serverrules.json. */
    DISALLOWED_ON_SOME_SERVERS
}
