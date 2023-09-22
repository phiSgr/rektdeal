plugins {
    java
    kotlin("jvm")

    id("org.jetbrains.kotlinx.kover")

    id("org.jetbrains.dokka")

    id("maven-publish")
    id("signing")
}

repositories {
    mavenCentral()
}

val taskIsKover = gradle.startParameter.taskRequests.any {
    it.args.any { arg -> arg.startsWith("kover") }
}

kover {
    if (!taskIsKover) {
        disable() // Enabling coverage makes the tests run a lot longer
    }
}
tasks.test {
    useJUnitPlatform()

    // During development, the dll is put in the repository root
    workingDir = rootProject.projectDir

    if (taskIsKover) {
        environment("SHORT_TEST", true)
    }
    environment("DDS4J_LOAD", "LOAD_LIBRARY")
}

group = "com.github.phisgr"
version = when (name) {
    "dds4j" -> "0.0.0"
    "rektdeal" -> "0.0.0"
    else -> throw IllegalStateException("unknown project $name")
}

java {
    withJavadocJar()
    withSourcesJar()
}
val javadocJar = tasks.named<Jar>("javadocJar") {
    from(tasks.named("dokkaHtml"))
}

signing {
    useGpgCmd()
    sign(publishing.publications)
}

tasks.withType<Javadoc>().all { enabled = false }

tasks.register("printVersion") {
    doLast {
        println("${project.name}_version=${project.version}")
    }
}

task<JavaExec>("runTestMainClass") { // test equivalent for application
    dependsOn("testClasses")
    classpath = sourceSets["test"].runtimeClasspath
    (project.properties["testMainClass"] as String?)?.let {
        mainClass.set(it)
    }

    jvmArgs = listOf(
        "--enable-native-access=ALL-UNNAMED",
        "--enable-preview"
    )
}
