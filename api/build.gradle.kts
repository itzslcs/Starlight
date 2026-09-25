plugins { `java-library` }

// Same bytes in every jar: compiled once for Java 8 (1.8.9 runs on Java 8; 26.x on 25).
java { toolchain.languageVersion = JavaLanguageVersion.of(21) }
tasks.withType<JavaCompile>().configureEach {
    options.release = 8
    options.encoding = "UTF-8"
    options.compilerArgs.addAll(listOf("-Xlint:-options", "-Xlint:deprecation"))
}
