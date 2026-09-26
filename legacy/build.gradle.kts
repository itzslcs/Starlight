import java.util.Properties

// Forge 1.8.9 target (the only 1.8.9 loader Dawn/Feather ships; DECISIONS D-002).
plugins {
    java
    id("gg.essential.loom") version "1.15.50"
    id("dev.architectury.architectury-pack200") version "0.1.3"
}

// Shared identity lives in the main build's gradle.properties.
val shared = Properties().apply { rootDir.resolve("../gradle.properties").reader().use(::load) }
val modId: String = shared.getProperty("mod.id")
val modName: String = shared.getProperty("mod.name")
val modVersion: String = shared.getProperty("mod.version")
version = "$modVersion+mc1.8.9"
base.archivesName = modName

// core/api are compiled ONCE by the main build (--release 8) and consumed as jars, so all 18 jars carry identical bytes.
val coreJars = files("../core/build/libs/core.jar", "../api/build/libs/api.jar")

// -Pkestrel.smoke=<seconds> makes the dev client drive itself (see core Smoke + scripts/smoke.sh).
val smokeSeconds: String? = providers.gradleProperty("kestrel.smoke").orNull
val smokeClicks = providers.gradleProperty("kestrel.smokeClicks").isPresent

loom {
    runConfigs {
        named("client") {
            runDir = "../run/1.8.9"
            property("mixin.debug", "true")
            property("mixin.debug.countInjections", "true")
            programArgs("--tweakClass", "org.spongepowered.asm.launch.MixinTweaker")
            if (smokeSeconds != null) {
                property("kestrel.smoke", "1")
                property("kestrel.smoke.seconds", smokeSeconds)
                if (smokeClicks) property("kestrel.smoke.clicks", "true")
            }
        }
        remove(getByName("server"))
    }
    forge {
        pack200Provider.set(dev.architectury.pack200.java.Pack200Adapter())
        mixinConfig("mixins.$modId.json")
    }
}

sourceSets.main {
    // FML dev runtime expects resources next to classes.
    output.setResourcesDir(sourceSets.main.flatMap { it.java.classesDirectory })
}

repositories {
    mavenCentral()
    maven("https://repo.spongepowered.org/maven/")
}

val shade: Configuration = configurations.create("shade")
configurations.implementation { extendsFrom(shade) }

dependencies {
    minecraft("com.mojang:minecraft:1.8.9")
    mappings("de.oceanlabs.mcp:mcp_stable:22-1.8.9")
    forge("net.minecraftforge:forge:1.8.9-11.15.1.2318-1.8.9")
    // Plain Forge needs Mixin bundled; on Dawn its own Mixin 0.8.7-legacy is first on the classpath and wins.
    shade("org.spongepowered:mixin:0.7.11-SNAPSHOT") { isTransitive = false }
    // Compile-time mixin annotations only; Loom remaps annotation targets (MCP -> SRG) in remapJar, so no refmap.
    compileOnly("org.spongepowered:mixin:0.7.11-SNAPSHOT") { isTransitive = false }
    implementation(coreJars)
}

java { toolchain.languageVersion = JavaLanguageVersion.of(8) }
tasks.withType<JavaCompile>().configureEach { options.encoding = "UTF-8" }

tasks.processResources {
    val props = mapOf("id" to modId, "name" to modName, "version" to version.toString())
    inputs.properties(props)
    filesMatching(listOf("mcmod.info", "mixins.$modId.json")) { expand(props) }
}

tasks.named<Jar>("jar") {
    doFirst {
        coreJars.forEach { check(it.exists()) { "$it missing: run ./gradlew :core:jar in the repo root first (buildAll does this)" } }
    }
    from({ (coreJars + shade).map { zipTree(it) } }) {
        exclude("META-INF/MANIFEST.MF", "META-INF/*.SF", "META-INF/*.RSA", "META-INF/*.DSA", "LICENSE.txt",
            // Mixin's annotation processor / obfuscation tooling is build-time only (and its service file would make
            // javac run the processor for anyone compiling against this jar).
            "org/spongepowered/tools/**", "META-INF/services/javax.annotation.processing.Processor",
            "META-INF/services/org.spongepowered.tools.obfuscation.service.IObfuscationService")
    }
    // MIT notices travel with the code: ours, and Mixin's for the shaded copy (docs/THIRD_PARTY.md).
    from(rootDir.resolve("../LICENSE")) { rename { "LICENSE_kestrel" } }
    from({ shade.map { zipTree(it) } }) {
        include("LICENSE.txt")
        rename { "LICENSE_mixin" }
    }
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    manifest.attributes(
        "FMLCorePluginContainsFMLMod" to "true",
        "ForceLoadAsMod" to "true",
        "TweakClass" to "org.spongepowered.asm.launch.MixinTweaker",
        "MixinConfigs" to "mixins.$modId.json",
    )
}

tasks.register<Copy>("collectJar") {
    group = "build"
    from(tasks.named("remapJar")) // RemapJarTask is not a Jar subclass in this Loom fork; copy its outputs
    into(rootDir.resolve("../dist"))
}
