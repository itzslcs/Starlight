package dev.starlight.fabric;

import dev.starlight.core.Log;
import dev.starlight.core.Starlight;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VkApplicationInfo;
import org.lwjgl.vulkan.VkInstance;
import org.lwjgl.vulkan.VkInstanceCreateInfo;
import org.lwjgl.vulkan.VkPhysicalDevice;
import org.lwjgl.vulkan.VkPhysicalDeviceProperties;

import java.nio.IntBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * The GPU check behind "auto" (RendererSwitch): in an OpenGL session with the bundled VulkanMod kept off, a background
 * thread asks the Vulkan driver once whether a real graphics device offers Vulkan 1.2 (what VulkanMod needs) and writes
 * the answer to {@code Starlight/vulkan-probe.txt}. "ok" switches the next start to Vulkan. A marker file brackets the check,
 * so a driver that crashes the game here is remembered as "no" (RendererSwitch reads it at the next start).
 */
final class VulkanProbe {
    private static boolean started;

    private VulkanProbe() {}

    /** Once, after the game is up, when the answer is the only thing missing. */
    static void maybeRun() {
        if (started || !"opengl".equals(RendererSwitch.state) || !RendererSwitch.bundled) return;
        if ("opengl".equals(RendererSwitch.choice()) || !RendererSwitch.read(RendererSwitch.dir().resolve("vulkan-probe.txt")).isEmpty()) return;
        if (RendererSwitch.status().contains("is installed")) return; // another renderer mod: Vulkan stays off anyway
        started = true;
        Thread t = new Thread(VulkanProbe::run, "Starlight Vulkan check");
        t.setDaemon(true);
        t.start();
    }

    private static void run() {
        Path dir = RendererSwitch.dir(), running = dir.resolve("vulkan-probe-running");
        String result;
        try {
            Files.createDirectories(dir);
            Files.write(running, new byte[0]);
            result = check();
        } catch (Throwable t) {
            result = "no (" + t.getClass().getSimpleName() + (t.getMessage() == null ? "" : ": " + t.getMessage()) + ")";
        }
        try {
            Files.write(dir.resolve("vulkan-probe.txt"), result.getBytes(StandardCharsets.UTF_8));
            Files.deleteIfExists(running);
        } catch (java.io.IOException e) {
            Log.warn("Vulkan check: could not save the result: " + e);
        }
        RendererSwitch.probed(result);
        Log.info("Vulkan check: " + result);
        final Starlight k = Starlight.get();
        if (k != null && result.startsWith("ok") && RendererSwitch.vulkanNext()) {
            k.scheduler.runOnMain(() -> k.toast("Vulkan renderer ready",
                    "Your graphics card runs Vulkan, so Starlight starts with it (VulkanMod) next time: usually more FPS. Switch on the Performance page.",
                    k.theme.accent));
        }
    }

    /** "ok <device> (Vulkan x.y)" for the best hardware device with Vulkan 1.2+, else "no (why)". */
    static String check() {
        boolean software = Boolean.getBoolean("starlight.vulkan.allowSoftware"); // tests on a CPU-only Vulkan driver
        boolean ours = true;
        try {
            VK.create();
        } catch (IllegalStateException alreadyLoaded) { // GLFW's Vulkan support loads it too: share it, leave it loaded
            ours = false;
        }
        try (MemoryStack stack = MemoryStack.stackPush()) {
            int loader = VK.getInstanceVersionSupported();
            if (loader < VK10.VK_MAKE_API_VERSION(0, 1, 2, 0)) return "no (the Vulkan loader only offers " + api(loader) + ")";
            VkApplicationInfo app = VkApplicationInfo.calloc(stack).sType$Default()
                    .pApplicationName(stack.UTF8("Starlight Vulkan check")).apiVersion(VK10.VK_MAKE_API_VERSION(0, 1, 2, 0));
            VkInstanceCreateInfo info = VkInstanceCreateInfo.calloc(stack).sType$Default().pApplicationInfo(app);
            PointerBuffer p = stack.mallocPointer(1);
            int r = VK10.vkCreateInstance(info, null, p);
            if (r != VK10.VK_SUCCESS) return "no (no Vulkan driver: error " + r + ")";
            VkInstance instance = new VkInstance(p.get(0), info);
            try {
                IntBuffer n = stack.mallocInt(1);
                VK10.vkEnumeratePhysicalDevices(instance, n, null);
                if (n.get(0) == 0) return "no (no Vulkan device)";
                PointerBuffer devices = stack.mallocPointer(n.get(0));
                VK10.vkEnumeratePhysicalDevices(instance, n, devices);
                String best = null, seen = null;
                int bestRank = -1;
                for (int i = 0; i < n.get(0); i++) {
                    VkPhysicalDeviceProperties props = VkPhysicalDeviceProperties.malloc(stack);
                    VK10.vkGetPhysicalDeviceProperties(new VkPhysicalDevice(devices.get(i), instance), props);
                    int type = props.deviceType();
                    String name = props.deviceNameString() + " (Vulkan " + api(props.apiVersion()) + ")";
                    seen = name;
                    if (props.apiVersion() < VK10.VK_MAKE_API_VERSION(0, 1, 2, 0)) continue;
                    if (type == VK10.VK_PHYSICAL_DEVICE_TYPE_CPU && !software) continue; // software Vulkan: no gain
                    int rank = type == VK10.VK_PHYSICAL_DEVICE_TYPE_DISCRETE_GPU ? 3 : type == VK10.VK_PHYSICAL_DEVICE_TYPE_INTEGRATED_GPU ? 2 : 1;
                    if (rank > bestRank) {
                        bestRank = rank;
                        best = name;
                    }
                }
                return best != null ? "ok " + best : "no (" + (seen == null ? "no device" : seen + " is not enough") + ")";
            } finally {
                VK10.vkDestroyInstance(instance, null);
            }
        } finally {
            if (ours) VK.destroy();
        }
    }

    private static String api(int v) {
        return VK10.VK_API_VERSION_MAJOR(v) + "." + VK10.VK_API_VERSION_MINOR(v);
    }
}
