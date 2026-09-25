plugins { `java-library` }

dependencies {
    api(project(":api"))
    testImplementation(platform("org.junit:junit-bom:5.13.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

repositories { mavenCentral() }

java { toolchain.languageVersion = JavaLanguageVersion.of(21) }
tasks.withType<JavaCompile>().configureEach {
    options.release = 8
    options.encoding = "UTF-8"
    options.compilerArgs.addAll(listOf("-Xlint:-options", "-Xlint:deprecation"))
}
tasks.test { useJUnitPlatform() }
