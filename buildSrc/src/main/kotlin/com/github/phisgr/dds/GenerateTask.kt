package com.github.phisgr.dds

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.*
import java.io.File

@CacheableTask
open class GenerateTask : DefaultTask() {
    @get:InputDirectory
    @PathSensitive(PathSensitivity.RELATIVE)
    val inputDirectory: File = project.file("build/generated/sources/jextract/main/java")

    @get:OutputDirectories
    val outputDirectory: File = project.file("build/generated/sources/dds/main/kotlin")

    @TaskAction
    fun generate() {
        val sources = File(inputDirectory, "dds/")
            .listFiles()!!
            .filterNot {
                it.name.contains("constants$") || it.name == "RuntimeHelper.java" || it.name == "Dds.java"
            }
        genFun(File(inputDirectory, "dds/Dds.java").readText())
            .writeTo(outputDirectory)
        genTypes(sources)
            .writeTo(outputDirectory)
    }
}
