import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()       // to resolve kotlinpoet
    gradlePluginPortal() // to resolve plugins
}

dependencies {
    implementation("com.squareup:kotlinpoet:1.14.2")

    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:1.9.25")
    implementation("org.jetbrains.kotlinx:kover-gradle-plugin:0.7.5")
    implementation("org.jetbrains.dokka:dokka-gradle-plugin:1.9.20")
    implementation("me.champeau.jmh:jmh-gradle-plugin:0.7.2")
}

kotlin {
    compilerOptions.jvmTarget.set(JvmTarget.JVM_1_8)
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}
