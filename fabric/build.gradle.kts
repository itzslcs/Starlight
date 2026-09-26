// Shared build script for every Fabric target (Stonecutter node fabric/versions/<mc>).
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

dependencies {
    minecraft("com.mojang:minecraft:$mc")
    loomx.applyMojangMappings()
    modImplementation("net.fabricmc:fabric-loader:${property("deps.fabric_loader")}")
    implementation(project(":core"))
    bundle(project(":core"))
}

// -Pkestrel.smoke=<seconds> makes the dev client drive itself (see core Smoke + scripts/smoke.sh).
val smokeSeconds: String? = providers.gradleProperty("kestrel.smoke").orNull
// -Pkestrel.smokeClicks=1: smoke.sh's xdotool helper will click the menu for real (Smoke.requestClick).
val smokeClicks = providers.gradleProperty("kestrel.smokeClicks").isPresent

loom {
    runConfigs.all {
        runDirectory = rootProject.file("run/$mc")
        generateRunConfig = false
        jvmArguments.add("-Dmixin.debug.export=true")
        // Logs an error for any injector that matched fewer targets than expected, even with require = 0,
        // so a silently skipped optional mixin fails the smoke test.
        jvmArguments.add("-Dmixin.debug.countInjections=true")
        if (smokeSeconds != null) {
            jvmArguments.add("-Dkestrel.smoke=1")
            jvmArguments.add("-Dkestrel.smoke.seconds=$smokeSeconds")
            if (smokeClicks) jvmArguments.add("-Dkestrel.smoke.clicks=true")
        }
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
    dependsOn(bundle)
    from({ bundle.map { zipTree(it) } }) { exclude("META-INF/MANIFEST.MF") }
    from(rootProject.file("LICENSE")) { rename { "LICENSE_kestrel" } }
}

// Copies the final (remapped on <=1.21.11) jar to <root>/dist.
tasks.register<Copy>("collectJar") {
    group = "build"
    from(loomx.modJar.flatMap { it.archiveFile })
    into(rootProject.layout.projectDirectory.dir("dist"))
}
