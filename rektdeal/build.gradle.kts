import com.github.phisgr.rektdeal.Publishing.configure
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("common.library-conventions")

    id("me.champeau.jmh")
}

dependencies {
    compileOnly(project(":api8"))
    testCompileOnly(project(":api8"))

    // for use in Jupyter notebooks
    compileOnly("org.jetbrains.kotlinx:dataframe:0.13.1")

    api(project(":dds4j"))
    testImplementation(kotlin("test"))
}

tasks.test {
    dependsOn(":dds4j:jar")
    classpath += files("${rootProject.projectDir}/dds4j/build/libs/dds4j-${project(":dds4j").version}.jar")
    jvmArgs("--enable-native-access=ALL-UNNAMED")
}

kotlin {
    compilerOptions.jvmTarget.set(JvmTarget.JVM_1_8)
}
java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

jmh {
    iterations.set(3)
    warmupIterations.set(3)
    fork.set(10)
    threads.set(1)
    profilers.set(listOf("gc"))

    jmhVersion.set("1.37")
}

repositories {
    mavenLocal()
}

publishing {
    configure(
        project,
        "ReKtDeal",
        "A bridge deal generator for Kotlin.",
        "https://github.com/phiSgr/rektdeal"
    )
}

tasks.named<JavaExec>("runTestMainClass") {
    dependsOn(":dds4j:jar")
    classpath += files("${rootProject.projectDir}/dds4j/build/libs/dds4j-${project(":dds4j").version}.jar")
}
