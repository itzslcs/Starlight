package dev.starlight.core.perf;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Picks a starting {@link VideoPreset} from the graphics device the game reports, the CPU thread count and the memory
 * Minecraft may use. A heuristic, deliberately cautious: when in doubt it picks the faster preset.
 */
public final class HardwareTier {
    public static final class Suggestion {
        public final VideoPreset preset;
        public final String reason;

        Suggestion(VideoPreset preset, String reason) {
            this.preset = preset;
            this.reason = reason;
        }
    }

    private static final Pattern NVIDIA = Pattern.compile("(rtx|gtx|gt|mx) ?(\\d{3,4})");
    private static final Pattern RADEON = Pattern.compile("rx ?(\\d{3,4})");
    private static final Pattern ARC = Pattern.compile("arc(\\(tm\\))? ?([ab])(\\d{3})");
    /** Radeon 660M-890M: current laptop APUs, roughly entry-level card speed. */
    private static final Pattern STRONG_APU = Pattern.compile("\\b(66|68|76|78|86|88|89)0m\\b");
    /** Mesa names older APUs by codename. */
    private static final Pattern APU = Pattern.compile("renoir|cezanne|lucienne|barcelo|rembrandt|raven|picasso|mendocino|van gogh|dali|pollock");

    private HardwareTier() {}

    /**
     * @param gpu     renderer name (e.g. "NVIDIA GeForce RTX 3060/PCIe/SSE2", "Mesa Intel(R) UHD Graphics 620"); may be null
     * @param kind    "integrated", "discrete", "cpu", "virtual" where the game reports it (26.2+), else ""
     * @param threads CPU threads the JVM sees
     * @param heapMb  maximum heap in MiB
     */
    public static Suggestion suggest(String gpu, String kind, int threads, long heapMb) {
        String g = gpu == null ? "" : gpu.toLowerCase(Locale.ROOT);
        String k = kind == null ? "" : kind.toLowerCase(Locale.ROOT);
        VideoPreset p;
        String why;
        if (k.equals("cpu") || g.contains("llvmpipe") || g.contains("softpipe") || g.contains("swiftshader")
                || g.contains("basic render") || g.contains("gdi generic") || g.contains("software")) {
            p = VideoPreset.POTATO;
            why = "software rendering";
        } else if (g.contains("apple m")) {
            p = VideoPreset.MEDIUM;
            why = "Apple silicon";
        } else if (g.contains("mali") || g.contains("adreno") || g.contains("powervr") || g.contains("videocore")) {
            p = VideoPreset.POTATO;
            why = "mobile graphics";
        } else if (g.contains("intel") && !ARC.matcher(g).find()) {
            boolean xe = g.contains("xe") || g.contains("iris");
            p = xe ? VideoPreset.LOW : VideoPreset.POTATO;
            why = xe ? "Intel Iris/Xe integrated graphics" : "Intel integrated graphics";
        } else {
            Matcher arc = ARC.matcher(g), nv = NVIDIA.matcher(g), rx = RADEON.matcher(g);
            if (arc.find()) {
                int n = Integer.parseInt(arc.group(3));
                p = arc.group(2).equals("b") || n >= 700 ? VideoPreset.HIGH : n >= 500 ? VideoPreset.MEDIUM : VideoPreset.LOW;
                why = "Intel Arc graphics";
            } else if (nv.find()) {
                String series = nv.group(1);
                int n = Integer.parseInt(nv.group(2));
                if (series.equals("rtx")) p = VideoPreset.HIGH;
                else if (series.equals("gtx")) p = n >= 1000 ? VideoPreset.MEDIUM : VideoPreset.LOW;
                else p = VideoPreset.LOW; // GT and MX are entry level
                why = "NVIDIA " + series.toUpperCase(Locale.ROOT) + " " + n;
            } else if (rx.find()) {
                int n = Integer.parseInt(rx.group(1));
                p = n >= 6000 ? VideoPreset.HIGH : n >= 400 ? VideoPreset.MEDIUM : VideoPreset.LOW;
                why = "AMD Radeon RX " + n;
            } else if (g.contains("radeon") && STRONG_APU.matcher(g).find()) {
                p = VideoPreset.MEDIUM;
                why = "AMD Radeon 600M/700M/800M integrated graphics";
            } else if (g.contains("radeon") && (g.contains("graphics") || g.contains("vega") || APU.matcher(g).find())) {
                p = VideoPreset.LOW;
                why = "AMD integrated graphics";
            } else if (g.contains("radeon pro")) {
                p = VideoPreset.MEDIUM;
                why = "AMD Radeon Pro";
            } else if (k.equals("integrated")) {
                p = VideoPreset.LOW;
                why = "integrated graphics";
            } else if (k.equals("discrete")) {
                p = VideoPreset.MEDIUM;
                why = "a graphics card Starlight does not know";
            } else {
                p = VideoPreset.LOW;
                why = "unknown graphics";
            }
        }
        if (threads > 0 && threads <= 4 && p != VideoPreset.POTATO) {
            p = p.lower();
            why += ", " + threads + " CPU threads";
        }
        if (heapMb > 0 && heapMb < 2048 && p != VideoPreset.POTATO) {
            p = p.lower();
            why += ", " + heapMb + " MB for Minecraft";
        }
        return new Suggestion(p, why);
    }
}
