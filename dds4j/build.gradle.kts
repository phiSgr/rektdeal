import com.github.phisgr.dds.GenerateTask
import com.github.phisgr.rektdeal.Publishing.configure
import io.github.krakowski.jextract.JextractTask
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("common.library-conventions")

    id("io.github.krakowski.jextract") version "0.5.0"
}

dependencies {
    testImplementation(kotlin("test"))
}

kotlin {
    compilerOptions.jvmTarget.set(JvmTarget.JVM_21)
}

java {
    // not ideal that we're creating class files version 65 (Java 21)
    // when they depend on classes in Java 22
    // but before Kotlin supports Java 22 that'll have to do
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

tasks.dokkaHtml.configure {
    dokkaSourceSets {
        configureEach {
            // include generated functions (e.g. solveBoard) and classes (e.g. DdTableDeal)
            suppressGeneratedFiles.set(false)

            // but hide the low level code from jextract
            suppressedFiles.from(tasks.withType<JextractTask>().map { it.outputDir })
        }
    }
}

tasks.withType<JextractTask> {
    toolchain.set(
        project.properties["jextractPath"] as String?
            ?: "${System.getProperty("user.home")}/Downloads/jextract-22/"
    )

    header("${rootProject.projectDir}/dds/include/dll.h") {
        targetPackage.set("dds")
        className.set("Dds")

        argFile.set("$projectDir/includes.txt")
    }

}

publishing {
    configure(
        project,
        "DDS4J",
        "Java FFM binding for Bo Haglund's Double Dummy Solver",
        "https://github.com/phiSgr/rektdeal/tree/main/dds4j",
        licenseSpec = {
            name.set("Apache-2.0")
            url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
        }
    )
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
