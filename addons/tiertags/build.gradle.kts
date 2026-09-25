plugins { java }

dependencies { compileOnly(project(":api")) }

java { toolchain.languageVersion = JavaLanguageVersion.of(21) }
tasks.withType<JavaCompile>().configureEach {
    options.release = 8
    options.encoding = "UTF-8"
    options.compilerArgs.add("-Xlint:-options")
}
tasks.jar { archiveFileName = "Kestrel-addon-tiertags-${project.property("mod.version")}.jar" }
