package dev.kestrel.core;

import dev.kestrel.core.plugin.PluginManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import static org.junit.jupiter.api.Assertions.*;

/** Loads real plugin jars (compiled on the fly) through a Kestrel instance backed by the test platform. */
class PluginManagerTest {
    @TempDir
    Path dir;

    private Path plugin(String id, String version, String deps, String body) throws Exception {
        Path src = dir.resolve("src-" + id);
        Path pkg = src.resolve("p" + id.replace("-", ""));
        Files.createDirectories(pkg);
        String cls = "Main";
        String code = "package p" + id.replace("-", "") + ";\n"
                + "public class Main implements dev.kestrel.api.Plugin {\n"
                + "  public void onEnable(dev.kestrel.api.PluginContext ctx) throws Exception { " + body + " }\n}\n";
        Files.write(pkg.resolve(cls + ".java"), code.getBytes(StandardCharsets.UTF_8));
        JavaCompiler javac = ToolProvider.getSystemJavaCompiler();
        Path out = dir.resolve("out-" + id);
        Files.createDirectories(out);
        int rc = javac.run(null, null, null, "-d", out.toString(), "-cp", System.getProperty("java.class.path"),
                pkg.resolve(cls + ".java").toString());
        assertEquals(0, rc);
        Path plugins = dir.resolve("game").resolve("Kestrel").resolve("plugins");
        Files.createDirectories(plugins);
        Path jar = plugins.resolve(id + ".jar");
        JarOutputStream j = new JarOutputStream(new FileOutputStream(jar.toFile()));
        try {
            j.putNextEntry(new JarEntry("plugin.json"));
            j.write(("{\"id\":\"" + id + "\",\"version\":\"" + version + "\",\"api\":\"1.0\",\"main\":\"p" + id.replace("-", "")
                    + ".Main\"" + deps + "}").getBytes(StandardCharsets.UTF_8));
            // every compiled class (anonymous classes compile to Main$1.class etc.)
            java.io.File[] classes = out.resolve("p" + id.replace("-", "")).toFile().listFiles();
            for (java.io.File c : classes) {
                j.putNextEntry(new JarEntry("p" + id.replace("-", "") + "/" + c.getName()));
                j.write(Files.readAllBytes(c.toPath()));
            }
        } finally {
            j.close();
        }
        return jar;
    }

    @Test
    void consentDependenciesFailuresAndCleanup() throws Exception {
        plugin("base", "1.0.0", "", "ctx.logger().info(\"base up\");");
        plugin("addon", "1.0.0", ",\"depends\":{\"base\":\">=1.0\"}",
                "ctx.registerModule(new dev.kestrel.api.module.Module(\"addon_mod\",\"Addon\",\"x\",dev.kestrel.api.module.Category.ADDON,"
                        + "dev.kestrel.api.module.Rule.ALLOWED,true){});");
        plugin("broken", "1.0.0", "", "throw new IllegalStateException(\"nope\");");
        plugin("orphan", "1.0.0", ",\"depends\":{\"missing\":\"*\"}", "");
        Files.write(dir.resolve("game/Kestrel/plugins/notajar.jar"), new byte[]{1, 2, 3});

        Kestrel k = TestPlatform.boot(dir.resolve("game"));
        PluginManager pm = k.plugins;
        assertEquals(PluginManager.State.NEEDS_CONSENT, pm.find("base").state, "never runs without consent");
        assertEquals(PluginManager.State.INCOMPATIBLE, pm.find("orphan").state);
        assertTrue(pm.find("orphan").error.contains("missing"));
        boolean garbageReported = false;
        for (PluginManager.Entry e : pm.entries()) garbageReported |= e.descriptor == null && e.state == PluginManager.State.INCOMPATIBLE;
        assertTrue(garbageReported, "unreadable jar is reported, not thrown");

        pm.grantConsent(pm.find("base"));
        assertEquals(PluginManager.State.ENABLED, pm.find("base").state);
        pm.grantConsent(pm.find("addon"));
        assertEquals(PluginManager.State.ENABLED, pm.find("addon").state);
        assertNotNull(k.modules.get("addon_mod"), "plugin module registered");
        pm.grantConsent(pm.find("broken"));
        assertEquals(PluginManager.State.FAILED, pm.find("broken").state);
        assertTrue(pm.find("broken").error.contains("nope"));

        pm.setEnabled(pm.find("addon"), false);
        assertNull(k.modules.get("addon_mod"), "modules removed on disable");
        pm.setEnabled(pm.find("addon"), true);
        assertNotNull(k.modules.get("addon_mod"));

        // consent persists for the same jar; a changed jar asks again
        k.config.flush();
        Kestrel k2 = TestPlatform.boot(dir.resolve("game"));
        assertEquals(PluginManager.State.ENABLED, k2.plugins.find("base").state);
        plugin("base", "1.0.1", "", "");
        Kestrel k3 = TestPlatform.boot(dir.resolve("game"));
        assertEquals(PluginManager.State.NEEDS_CONSENT, k3.plugins.find("base").state);
        assertEquals(PluginManager.State.FAILED, k3.plugins.find("addon").state, "dependency not enabled");
    }
}
