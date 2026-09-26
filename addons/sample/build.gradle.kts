plugins { java }

dependencies { compileOnly(project(":api")) }

java { toolchain.languageVersion = JavaLanguageVersion.of(21) }
tasks.withType<JavaCompile>().configureEach {
    options.release = 8
    options.encoding = "UTF-8"
    options.compilerArgs.add("-Xlint:-options")
}
tasks.jar { archiveFileName = "MW19-addon-sample-${project.property("mod.version")}.jar" }

tasks.processResources {
    val v = project.property("mod.version").toString()
    inputs.property("version", v)
    filesMatching("plugin.json") { expand("version" to v) }
}
