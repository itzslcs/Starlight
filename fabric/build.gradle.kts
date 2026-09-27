// Shared build script for every Fabric target (Stonecutter node fabric/versions/<mc>).
import java.net.URI
import java.security.MessageDigest

plugins {
    id("dev.kikugie.loom-back-compat")
}

val mc: String = sc.current.version
val unobf = sc.current.parsed >= "26.1"
val javaVersion = if (unobf) 25 else 21

val modId = property("mod.id") as String
val modName = property("mod.name") as String
version = "${property("mod.version")}+mc$mc"
base.archivesName = modName

// core + api are compiled once for Java 8 and merged into the mod jar (identical bytes in every target).
val bundle: Configuration = configurations.create("bundle") { isTransitive = true }

// VulkanMod (LGPL-3.0-only) nested as jar-in-jar where fabric/bundled.json pins a build for this Minecraft version
// (scripts/vulkanmod-pin.py: only builds whose source commit is public). RendererSwitch decides at launch whether it
// runs; its source zip goes to dist/sources/ and the LGPL/GPL texts into the jar (DECISIONS D-024, THIRD_PARTY.md).
@Suppress("UNCHECKED_CAST")
val vulkanmod: Map<String, String>? = ((groovy.json.JsonSlurper().parse(rootProject.file("fabric/bundled.json")) as Map<String, Any>)
    ["vulkanmod"] as Map<String, Map<String, String>>)[mc]

// The same file as a resolvable configuration, for the checksum below ("include" itself cannot be resolved).
val vulkanJar: Configuration = configurations.create("vulkanJar") { isTransitive = false }

repositories {
    exclusiveContent {
        forRepository { maven("https://api.modrinth.com/maven") { name = "Modrinth" } }
        filter { includeGroup("maven.modrinth") }
    }
    mavenCentral()
}

dependencies {
    minecraft("com.mojang:minecraft:$mc")
    loomx.applyMojangMappings()
    modImplementation("net.fabricmc:fabric-loader:${property("deps.fabric_loader")}")
    implementation(project(":core"))
    bundle(project(":core"))
    // The Vulkan probe (RendererSwitch) runs against the LWJGL Vulkan binding VulkanMod nests.
    compileOnly("org.lwjgl:lwjgl-vulkan:3.3.3")
    if (vulkanmod != null) {
        "include"("maven.modrinth:vulkanmod:${vulkanmod["modrinth"]}")
        vulkanJar("maven.modrinth:vulkanmod:${vulkanmod["modrinth"]}")
    }
}

// The nested VulkanMod must be the pinned file, byte for byte.
val verifyBundled = tasks.register("verifyBundled") {
    val pinned = vulkanmod
    val files = vulkanJar
    onlyIf { pinned != null }
    doLast {
        val jar = files.resolve().firstOrNull { it.name.startsWith("vulkanmod") }
            ?: throw GradleException("VulkanMod ${pinned!!["version"]} was not resolved")
        val md = MessageDigest.getInstance("SHA-512")
        jar.inputStream().use { input -> val buf = ByteArray(1 shl 16); while (true) { val n = input.read(buf); if (n < 0) break; md.update(buf, 0, n) } }
        val sha = md.digest().joinToString("") { "%02x".format(it) }
        if (sha != pinned!!["sha512"]) throw GradleException("VulkanMod ${jar.name}: SHA-512 $sha does not match fabric/bundled.json")
    }
}

// What the jar carries besides MW19: a notice naming the bundled VulkanMod, its source and the licenses.
val thirdPartyNotice = tasks.register("thirdPartyNotice") {
    val out = layout.buildDirectory.file("generated/notice/THIRD_PARTY_NOTICES.txt")
    val pinned = vulkanmod
    inputs.property("vulkanmod", pinned?.toString() ?: "none")
    outputs.file(out)
    doLast {
        val text = if (pinned == null) "This build of MW19 bundles no third-party code.\n" else """
            This jar contains VulkanMod ${pinned["version"]} (https://github.com/xCollateral/VulkanMod) by Collateral,
            unmodified, as the nested jar META-INF/jars/vulkanmod-${pinned["modrinth"]}.jar (from
            https://modrinth.com/mod/vulkanmod/version/${pinned["modrinth"]}).

            VulkanMod is free software under the GNU Lesser General Public License, version 3 only (LGPL-3.0-only).
            Copies of the LGPL and of the GNU GPL it builds on are in META-INF/licenses/ in this jar. MW19 itself is
            MIT-licensed (LICENSE_mw19) and only chooses at launch whether VulkanMod runs.

            Source code of this exact VulkanMod version: ${pinned["source"]}
            (as a zip: ${pinned["sourceZip"]}; also distributed next to MW19's jars as sources/).
            To use a different or modified VulkanMod, put its jar in your mods folder: Fabric then loads the newer
            version instead of this one.
        """.trimIndent() + "\n"
        out.get().asFile.apply { parentFile.mkdirs() }.writeText(text)
    }
}

// -Pmw19.withMods=<dir>: every jar in <dir> joins the dev run as a mod, remapped by Loom at build time (smoke.sh
// WITH_MODS compatibility runs). Dropping them into run/<mc>/mods instead left Fabric Loader's runtime remapping to it,
// and Sodium 0.8.14's own mixins then failed to find their targets (debug-log 2026-09-26).
// Only for the requested target: Stonecutter also configures its active version, which must not get another version's mods.
val requested = providers.gradleProperty("mw19.fabricTargets").orNull?.split(",")?.map { it.trim() }
val extraMods = requested == null || mc in requested
val withMods: String? = providers.gradleProperty("mw19.withMods").orNull
if (withMods != null && extraMods) dependencies {
    "modLocalRuntime"(fileTree(withMods) { include("*.jar") })
    // plain libraries those mods nest (e.g. VulkanMod's LWJGL Vulkan), unpacked by scripts/testmods.py
    "runtimeOnly"(fileTree(File(withMods).parentFile.resolve("compat-libs")) { include("*.jar") })
}
// -Pmw19.fabricApi=<version>: Fabric API from Fabric's Maven (its modules are nested jars that a plain file dependency skips).
val withFabricApi: String? = providers.gradleProperty("mw19.fabricApi").orNull
if (withFabricApi != null && extraMods) dependencies { "modLocalRuntime"("net.fabricmc.fabric-api:fabric-api:$withFabricApi") }

// -Pmw19.smoke=<seconds> makes the dev client drive itself (see core Smoke + scripts/smoke.sh).
val smokeSeconds: String? = providers.gradleProperty("mw19.smoke").orNull
// -Pmw19.smokeClicks=1: smoke.sh's xdotool helper will click the menu for real (Smoke.requestClick).
val smokeClicks = providers.gradleProperty("mw19.smokeClicks").isPresent
// -Pmw19.bench=1: the dev client runs the benchmark scene and quits (core Bench + scripts/bench.sh).
val bench = providers.gradleProperty("mw19.bench").isPresent
val benchScene: String? = providers.gradleProperty("mw19.benchScene").orNull

loom {
    // Classes and resources as one mod in dev runs (fabric.classPathGroups): Fabric then exposes the classes before
    // mods start, which the renderer switch (a language adapter, created that early) needs.
    mods.register(modId) { sourceSet(sourceSets.main.get()) }
    runConfigs.all {
        runDirectory = rootProject.file("run/$mc")
        generateRunConfig = false
        jvmArguments.add("-Dmixin.debug.export=true")
        // Logs an error for any injector that matched fewer targets than expected, even with require = 0,
        // so a silently skipped optional mixin fails the smoke test. Not with other mods present: their optional
        // injectors would trip it too (MW19's own wiring is still checked by scripts/mixin-audit.py).
        if (withMods == null) jvmArguments.add("-Dmixin.debug.countInjections=true")
        if (smokeSeconds != null) {
            jvmArguments.add("-Dmw19.smoke=1")
            jvmArguments.add("-Dmw19.smoke.seconds=$smokeSeconds")
            if (smokeClicks) jvmArguments.add("-Dmw19.smoke.clicks=true")
        }
        if (bench) jvmArguments.add("-Dmw19.bench=1")
        if (benchScene != null) jvmArguments.add("-Dmw19.bench.scene=$benchScene")
    }
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(javaVersion)
    sourceCompatibility = JavaVersion.toVersion(javaVersion)
    targetCompatibility = JavaVersion.toVersion(javaVersion)
}

tasks.withType<JavaCompile>().configureEach {
    options.release = javaVersion
    options.encoding = "UTF-8"
}

tasks.processResources {
    val props = mapOf(
        "id" to modId,
        "name" to modName,
        "version" to version.toString(),
        "minecraft" to mc,
        "java" to javaVersion,
        // 26.x needs a loader that understands unobfuscated Minecraft.
        "loader" to if (unobf) ">=0.18.0" else ">=0.16.0",
    )
    inputs.properties(props)
    filesMatching("fabric.mod.json") { expand(props) }
    filesMatching("*.mixins.json") { expand("java" to "JAVA_$javaVersion") }
}

tasks.named<Jar>("jar") {
    dependsOn(bundle, verifyBundled, thirdPartyNotice)
    from({ bundle.map { zipTree(it) } }) { exclude("META-INF/MANIFEST.MF") }
    from(rootProject.file("LICENSE")) { rename { "LICENSE_mw19" } }
    from(thirdPartyNotice)
    if (vulkanmod != null) from(rootProject.file("docs/licenses")) { into("META-INF/licenses") }
}

// The bundled VulkanMod's source, offered next to the jars (LGPL-3.0 section 4 / GPL-3.0 section 6).
val vulkanSource = tasks.register("vulkanSource") {
    val pinned = vulkanmod
    val out = rootProject.layout.projectDirectory.file("dist/sources/VulkanMod-${pinned?.get("version")}-src.zip")
    onlyIf { pinned != null }
    outputs.file(out)
    doLast {
        val f = out.asFile.apply { parentFile.mkdirs() }
        if (!f.exists()) {
            val conn = URI(pinned!!["sourceZip"]!!).toURL().openConnection()
            conn.setRequestProperty("User-Agent", "itzslcs/mw19-build")
            conn.getInputStream().use { input -> f.outputStream().use { output -> input.copyTo(output) } }
        }
    }
}

// Copies the final (remapped on <=1.21.11) jar to <root>/dist.
tasks.register<Copy>("collectJar") {
    group = "build"
    dependsOn(vulkanSource)
    from(loomx.modJar.flatMap { it.archiveFile })
    into(rootProject.layout.projectDirectory.dir("dist"))
}
