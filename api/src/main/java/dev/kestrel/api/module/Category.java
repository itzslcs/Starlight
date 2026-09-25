package dev.kestrel.api.module;

public enum Category {
    HUD("HUD"),
    VISUAL("Visual"),
    UTILITY("Utility"),
    CHAT("Chat"),
    HYPIXEL("Hypixel"),
    PERFORMANCE("Performance"),
    ADDON("Addons");

    private final String label;

    Category(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
