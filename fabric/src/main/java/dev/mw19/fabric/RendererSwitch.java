package dev.mw19.fabric;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.LanguageAdapter;
import net.fabricmc.loader.api.LanguageAdapterException;
import net.fabricmc.loader.api.ModContainer;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collections;

/**
 * Decides, before any mod code or mixin runs, whether the bundled VulkanMod renders this session (DECISIONS D-024).
 * Fabric creates every mod's language adapter after it has picked the mods and before it reads their entrypoints and
 * mixin configs, so an OpenGL session simply takes VulkanMod's entrypoints and mixin configs off its metadata: the jar
 * stays loaded but nothing of it runs. No Minecraft class may be touched here (none is transformable yet).
 *
 * <p>Vulkan runs when a GPU check passed (see {@link VulkanProbe}, it runs in an OpenGL session and takes effect at
 * the next start) or the player chose Vulkan, unless the player chose OpenGL, another renderer mod is installed, or the
 * last Vulkan start never reached the menu. That last case is how a failing driver is caught: the marker file
 * {@code MW19/vulkan-starting} is written before a Vulkan start and removed once the game is up (FabricPlatform), so if
 * it is still there the start crashed or hung, and MW19 stays on OpenGL until the player asks for Vulkan again.
 */
public final class RendererSwitch implements LanguageAdapter {
    /** "vulkan", "opengl" or "none" (no VulkanMod in this install). */
    public static volatile String state = "none";
    /** Why this session is on OpenGL although VulkanMod is present (empty on Vulkan). */
    public static volatile String reason = "";
    /** VulkanMod's version, when present. */
    public static volatile String version = "";
    /** VulkanMod came inside MW19's jar (a separately installed one is the player's own choice and left alone). */
    public static volatile boolean bundled;
    /** Cached at launch and kept current by {@link #choose} and the GPU check (the Performance page reads them per frame). */
    private static volatile String choice = "auto", probe = "";
    private static volatile boolean failed, conflict;
    /** Renderer mods VulkanMod cannot run beside (both replace the world renderer). */
    static final String[] CONFLICTS = {"sodium", "iris", "embeddium", "nvidium", "canvas"};

    public RendererSwitch() {
        try {
            decide();
        } catch (Throwable t) {
            System.err.println("[MW19] renderer switch failed, leaving VulkanMod as Fabric loaded it: " + t);
        }
    }

    @Override
    public <T> T create(ModContainer mod, String value, Class<T> type) throws LanguageAdapterException {
        throw new LanguageAdapterException("MW19's renderer switch creates no entrypoints");
    }

    static Path dir() {
        return FabricLoader.getInstance().getGameDir().resolve("MW19");
    }

    /** The player's choice: "auto" (default), "vulkan" or "opengl" (Performance page, applies at the next start). */
    public static String choice() {
        return choice;
    }

    private static String readChoice() {
        String c = read(dir().resolve("renderer.txt"));
        return "vulkan".equals(c) || "opengl".equals(c) ? c : "auto";
    }

    private static void decide() throws IOException {
        FabricLoader fl = FabricLoader.getInstance();
        ModContainer vk = fl.getModContainer("vulkanmod").orElse(null);
        if (vk == null) return;
        version = vk.getMetadata().getVersion().getFriendlyString();
        // A dev run's VulkanMod is the test harness's (smoke.sh WITH_MODS): treated as bundled so the switch is tested.
        bundled = fl.isDevelopmentEnvironment() || vk.getContainingMod().map(m -> "mw19".equals(m.getMetadata().getId())).orElse(false);
        if (!bundled) {
            state = "vulkan";
            System.out.println("[MW19] renderer: VulkanMod " + version + " (installed separately, left as it is)");
            return;
        }
        Path dir = dir(), starting = dir.resolve("vulkan-starting"), failedFile = dir.resolve("vulkan-failed");
        if (Files.exists(starting)) Files.move(starting, failedFile, StandardCopyOption.REPLACE_EXISTING);
        if (Files.exists(dir.resolve("vulkan-probe-running"))) { // the GPU check itself took the game down
            Files.write(dir.resolve("vulkan-probe.txt"), "no (the Vulkan check crashed)".getBytes(StandardCharsets.UTF_8));
            Files.deleteIfExists(dir.resolve("vulkan-probe-running"));
        }
        choice = readChoice();
        probe = read(dir.resolve("vulkan-probe.txt"));
        failed = Files.exists(failedFile);
        String why = null;
        for (String id : CONFLICTS) if (why == null && fl.isModLoaded(id)) why = fl.getModContainer(id).get().getMetadata().getName() + " is installed";
        conflict = why != null;
        if (why == null && "opengl".equals(choice)) why = "OpenGL chosen on the Performance page";
        if (why == null && failed) why = "Vulkan did not start last time";
        if (why == null && !"vulkan".equals(choice) && !probe.startsWith("ok")) {
            why = probe.isEmpty() ? "checking this PC's graphics for Vulkan" : "no Vulkan graphics: " + probe.replaceFirst("^no ", "");
        }
        if (why == null) {
            Files.createDirectories(dir);
            Files.write(starting, ("VulkanMod " + version + " starting").getBytes(StandardCharsets.UTF_8));
            state = "vulkan";
            System.out.println("[MW19] renderer: Vulkan (VulkanMod " + version + ")");
            return;
        }
        Object meta = vk.getMetadata(); // Fabric's V1ModMetadata: entrypoints and mixin configs are read after us
        try {
            set(meta, "entrypoints", Collections.emptyMap());
            set(meta, "mixins", Collections.emptyList());
            // It also says it brings a Fabric renderer, which makes Fabric API's own (Indigo) stand down: with VulkanMod
            // off, that would leave mods using the Fabric Rendering API without one.
            java.util.Map<String, net.fabricmc.loader.api.metadata.CustomValue> custom = new java.util.HashMap<>(vk.getMetadata().getCustomValues());
            if (custom.remove("fabric-renderer-api-v1:contains_renderer") != null) set(meta, "customValues", Collections.unmodifiableMap(custom));
        } catch (ReflectiveOperationException | RuntimeException e) { // it will run after all: watch that start
            Files.createDirectories(dir);
            Files.write(starting, ("VulkanMod " + version + " starting").getBytes(StandardCharsets.UTF_8));
            state = "vulkan";
            System.err.println("[MW19] could not keep VulkanMod off (" + why + "): " + e);
            return;
        }
        state = "opengl";
        reason = why;
        System.out.println("[MW19] renderer: OpenGL, VulkanMod " + version + " kept off (" + why + ")");
    }

    /** VulkanMod is running and the game got through its first frames: this start did not fail. */
    public static void started() {
        try {
            Files.deleteIfExists(dir().resolve("vulkan-starting"));
        } catch (IOException ignored) {
        }
    }

    /** Performance page: remembered for the next start. Choosing Vulkan again clears an earlier failed start. */
    public static void choose(String c) {
        choice = c;
        if ("vulkan".equals(c)) failed = false;
        try {
            Files.createDirectories(dir());
            Files.write(dir().resolve("renderer.txt"), c.getBytes(StandardCharsets.UTF_8));
            if ("vulkan".equals(c)) Files.deleteIfExists(dir().resolve("vulkan-failed"));
        } catch (IOException e) {
            System.err.println("[MW19] could not save the renderer choice: " + e);
        }
    }

    /** Whether the next start runs VulkanMod, by the same rules as {@link #decide} (Performance page toggle). */
    public static boolean vulkanNext() {
        if ("none".equals(state)) return false;
        if (!bundled) return true;
        if (conflict || "opengl".equals(choice) || failed) return false;
        return "vulkan".equals(choice) || probe.startsWith("ok");
    }

    /** The GPU check's answer (VulkanProbe). */
    static void probed(String result) {
        probe = result;
    }

    /** One line for the Performance page: what renders now and what the next start does. */
    public static String status() {
        if ("none".equals(state)) return "";
        if ("vulkan".equals(state)) return "Vulkan now (VulkanMod " + version + (bundled ? ")" : ", installed separately)")
                + (vulkanNext() ? "" : " · OpenGL from the next start");
        return "OpenGL now: " + reason + (vulkanNext() ? " · Vulkan from the next start" : "");
    }

    private static void set(Object o, String name, Object value) throws ReflectiveOperationException {
        for (Class<?> c = o.getClass(); c != null; c = c.getSuperclass()) {
            try {
                Field f = c.getDeclaredField(name);
                f.setAccessible(true);
                f.set(o, value);
                return;
            } catch (NoSuchFieldException ignored) {
            }
        }
        throw new NoSuchFieldException(o.getClass().getName() + "." + name);
    }

    static String read(Path p) {
        try {
            return Files.exists(p) ? new String(Files.readAllBytes(p), StandardCharsets.UTF_8).trim() : "";
        } catch (IOException e) {
            return "";
        }
    }
}
