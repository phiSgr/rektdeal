import com.github.phisgr.dds.GenerateTask
import com.github.phisgr.rektdeal.Publishing.configure
import io.github.krakowski.jextract.JextractPlugin
import io.github.krakowski.jextract.JextractTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("common.library-conventions")

    id("io.github.krakowski.jextract") version "0.4.1"
}

dependencies {
    testImplementation(kotlin("test"))
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "21"
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

val generatorTask by tasks.registering(GenerateTask::class) {
    dependsOn(tasks.withType<JextractTask>())
}


kotlin {
    sourceSets {
        main {
            kotlin {
                srcDir(generatorTask.map { it.outputDirectory })
            }
        }
    }
}

tasks.withType<JextractTask> {
    toolchain.set(
        project.properties["jextractPath"] as String?
            ?: "${System.getProperty("user.home")}/Downloads/jextract-21/"
    )

    header("${rootProject.projectDir}/dds/include/dll.h") {
        targetPackage.set("dds")
        className.set("Dds")
    }
}
plugins.withType<JextractPlugin>().configureEach {
    configurations {
        implementation {
            dependencies.removeIf {
                it is FileCollectionDependency
            }
        }
    }
}

publishing {
    configure(project, "DDS4J", "Wrapper around the Double Dummy Solver C++ Library.")
}

tasks.named<JavaExec>("runTestMainClass") {
    mainClass.set("example.Dds4j")
}

tasks.named<Jar>("sourcesJar") {
    // Both Kotlin and Java adds the java files to sources
    val regex = Regex("com/github/phisgr/dds/([A-Za-z]+)\\.java")
    val seen = mutableSetOf<String>()
    eachFile {
        val match = regex.matchEntire(path) ?: return@eachFile
        val isNew = seen.add(match.groupValues[1])
        if (!isNew) {
            duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        }
    }

    doLast {
        println("Files de-duplicated: $seen")
    }
}
