pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        maven("https://maven.architectury.dev/")
        maven("https://maven.fabricmc.net/")
        maven("https://maven.minecraftforge.net/")
        maven("https://repo.spongepowered.org/maven/")
        maven("https://repo.essential.gg/repository/maven-public/")
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

// Separate build on purpose (DECISIONS D-003): Essential's architectury-loom fork and Fabric Loom share
// net.fabricmc.loom.* class names and must not meet in one Gradle build.
rootProject.name = "Starlight-legacy"
