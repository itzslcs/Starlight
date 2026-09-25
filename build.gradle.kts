import java.security.MessageDigest

plugins { base }

val dist = layout.projectDirectory.dir("dist")
val isWindows = System.getProperty("os.name").lowercase().contains("win")

val cleanDist by tasks.registering(Delete::class) {
    group = "build"
    description = "Removes previous release artifacts from dist/."
    delete(dist)
}

// 1.8.9 is a separate Gradle build (DECISIONS D-003); it consumes core/api jars built here.
val buildLegacy by tasks.registering(Exec::class) {
    group = "build"
    description = "Builds the Forge 1.8.9 jar through legacy/'s own wrapper."
    dependsOn(":api:jar", ":core:jar", cleanDist)
    workingDir = file("legacy")
    commandLine(if (isWindows) listOf("cmd", "/c", "gradlew.bat") else listOf("./gradlew"))
    args("remapJar", "collectJar", "--console=plain", "-q")
}

val collectAddons by tasks.registering(Copy::class) {
    group = "build"
    dependsOn(cleanDist)
    from(project(":addons").subprojects.map { it.tasks.named("jar") })
    into(dist.dir("plugins"))
}

tasks.register("buildAll") {
    group = "build"
    description = "Builds every target jar into dist/ plus SHA256SUMS."
    dependsOn(cleanDist, ":core:test", buildLegacy, collectAddons)
    dependsOn(project(":fabric").subprojects.map { "${it.path}:collectJar" })
    doLast {
        val root = dist.asFile
        val files = root.walkTopDown().filter { it.isFile && it.name != "SHA256SUMS" }.sortedBy { it.path }.toList()
        val sums = files.joinToString("") { f ->
            val md = MessageDigest.getInstance("SHA-256")
            f.inputStream().use { input ->
                val buf = ByteArray(1 shl 16)
                while (true) {
                    val n = input.read(buf)
                    if (n < 0) break
                    md.update(buf, 0, n)
                }
            }
            md.digest().joinToString("") { "%02x".format(it) } + "  " + f.relativeTo(root).invariantSeparatorsPath + "\n"
        }
        root.resolve("SHA256SUMS").writeText(sums)
        logger.lifecycle("dist/: ${files.size} files\n$sums")
    }
}

// Every collectJar must run after the dist/ cleanup.
project(":fabric").subprojects {
    tasks.matching { it.name == "collectJar" }.configureEach { mustRunAfter(cleanDist) }
}
