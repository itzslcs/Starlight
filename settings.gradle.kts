pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        maven("https://maven.fabricmc.net/") { name = "Fabric" }
        maven("https://maven.kikugie.dev/releases") { name = "KikuGie Releases" }
        maven("https://maven.kikugie.dev/snapshots") { name = "KikuGie Snapshots" }
    }
}

plugins {
    id("dev.kikugie.stonecutter") version "0.9.8"
    // Applies fabric-loom-remap (obfuscated, <=1.21.11) or fabric-loom (26.x) per version.
    id("dev.kikugie.loom-back-compat") version "0.4.2"
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "Kestrel"
include("api", "core", "addons:sample", "addons:tiertags")

// versions.json is the single source of truth for targets. -Pkestrel.fabricTargets=1.21.11,26.3 narrows it for dev.
@Suppress("UNCHECKED_CAST")
val allFabric = ((groovy.json.JsonSlurper().parse(file("versions.json")) as Map<String, Any>)["targets"] as List<Map<String, Any>>)
    .filter { it["loader"] == "fabric" }
    .map { it["mc"] as String }
val only = providers.gradleProperty("kestrel.fabricTargets").orNull
    ?.split(',')?.map(String::trim)?.filter(String::isNotEmpty).orEmpty()
val fabricTargets = if (only.isEmpty()) allFabric else allFabric.filter { it in only }

stonecutter {
    create(":fabric") {
        versions(fabricTargets)
        vcsVersion = if ("1.21.11" in fabricTargets) "1.21.11" else fabricTargets.first()
    }
}
