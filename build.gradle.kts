import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

plugins { base }

val dist = layout.projectDirectory.dir("dist")
val isWindows = System.getProperty("os.name").lowercase().contains("win")
val fabricLoader = providers.gradleProperty("deps.fabric_loader").get()

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

tasks.register("buildAll") {
    group = "build"
    description = "Builds every target jar into dist/ plus SHA256SUMS."
    dependsOn(cleanDist, ":core:test", buildLegacy)
    dependsOn(project(":fabric").subprojects.map { "${it.path}:collectJar" })
    doLast {
        val root = dist.asFile
        writePrismInstances(root, fabricLoader)
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

/**
 * Importable Prism Launcher instances (DECISIONS D-017, docs/PRISM.md): dist/prism/MW19-<mc>.zip holds instance.cfg,
 * mmc-pack.json (component uids/versions as in Prism's own meta) and the mod jar. Fixed entry times keep the zips
 * byte-identical across builds.
 */
fun writePrismInstances(root: File, fabricLoader: String) {
    val out = root.resolve("prism").apply { mkdirs() }
    val jars = root.listFiles { f -> f.name.matches(Regex("MW19-.+\\+mc.+\\.jar")) }.orEmpty().sortedBy { it.name }
    for (jar in jars) {
        val mc = Regex("\\+mc(.+)\\.jar$").find(jar.name)!!.groupValues[1]
        val components = if (mc == "1.8.9") {
            listOf("""{"uid": "net.minecraft", "version": "1.8.9", "important": true}""",
                """{"uid": "net.minecraftforge", "version": "11.15.1.2318"}""")
        } else {
            listOf("""{"uid": "net.minecraft", "version": "$mc", "important": true}""",
                """{"uid": "net.fabricmc.intermediary", "version": "$mc", "dependencyOnly": true}""",
                """{"uid": "net.fabricmc.fabric-loader", "version": "$fabricLoader"}""")
        }
        val pack = "{\n    \"formatVersion\": 1,\n    \"components\": [\n        " + components.joinToString(",\n        ") + "\n    ]\n}\n"
        val cfg = "[General]\nInstanceType=OneSix\nname=MW19 $mc\niconKey=default\n"
        ZipOutputStream(out.resolve("MW19-$mc.zip").outputStream()).use { zip ->
            fun put(name: String, bytes: ByteArray) {
                zip.putNextEntry(ZipEntry(name).apply { time = 315532800000L }) // 1980-01-01
                zip.write(bytes)
                zip.closeEntry()
            }
            put("instance.cfg", cfg.toByteArray())
            put("mmc-pack.json", pack.toByteArray())
            put("minecraft/mods/${jar.name}", jar.readBytes())
        }
    }
    logger.lifecycle("dist/prism/: ${jars.size} Prism instance zips")
}
