package com.github.phisgr.dds

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.jvm.jvmName
import java.lang.foreign.MemorySegment
import java.util.*

private val funRegex = Regex("public static int ([A-Z][a-z]\\w+)\\([^)]+\\)")
private fun cRegex(name: String) = Regex("\\* \\{@snippet :$ws\\* int $name\\((.*)\\);")

// capture groups:              1       2          3         4       5     6   7
private val paramRegex = Regex("(struct (\\w+)\\*?|(char\\*)|(\\w+)) (\\w+)(\\[(\\d+)\\])?")

private fun structNameToClassName(structName: String) = dds4jPackage(toKtName(structName))

private val useAllThreads = setOf(
    "CalcPar", "CalcParPBN",
    "CalcDDtable", "CalcDDtablePBN",
    "CalcAllTables", "CalcAllTablesPBN",
    "SolveAllBoards", "SolveAllBoardsBin",
    "SolveAllChunks", "SolveAllChunksBin", "SolveAllChunksPBN",
    "AnalyseAllPlaysBin", "AnalyseAllPlaysPBN"
)

val typeOverride = mapOf(
    "vulnerable" to TypeConfig(dds4jPackage("Vulnerability")),
    "dealer" to directionType
)

fun genFun(javaFile: String): FileSpec {
    val file = FileSpec.builder(DDS4J_PACKAGE, "genFun")
        .jvmName("Dds")
        .indent("    ")

    funRegex.findAll(javaFile).forEach { match ->
        val cName = match.groupValues[1]
        val ktName = cName
            .replaceFirstChar { it.lowercase(Locale.getDefault()) }
            .replace("DDtable", "DdTable")
        if (ktName == "setThreading") return@forEach

        val funSpecBuilder = FunSpec.builder(ktName)

        val paramUsage = mutableListOf<String>()
        paramRegex.findAll(
            cRegex(cName).findAll(javaFile).single().groupValues[1]
        ).forEach { param ->
            val paramName = param.groupValues[5]
            val typeName = if (paramName == "pres" && cName == "ConvertToSidesTextFormat") {
                // the signature is a pointer, but that is an array of two elements
                paramUsage += "$paramName.memory"
                val structName = param.groupValues[2]
                structNameToClassName(structName).nestedClass("Array")
            } else if (param.groupValues[6] != "") {
                paramUsage += "$paramName.memory"
                if (param.groupValues[4] == "int") {
                    classCIntArray
                } else {
                    val structName = param.groupValues[2]
                    structNameToClassName(structName).nestedClass("Array")
                }
            } else if (param.groupValues[3] != "") {
                check(param.groupValues[3] == "char*")
                paramUsage += paramName
                MemorySegment::class.asTypeName()
            } else if (param.groupValues[4] != "") {
                val (usage, typeName) = when (val t = typeOverride[paramName]) {
                    null -> paramName to convertType[param.groupValues[4]]!!.asTypeName()
                    else -> "$paramName${t.unwrap}" to t.className
                }
                paramUsage += usage
                typeName
            } else {
                val structName = param.groupValues[2]
                paramUsage += "$paramName.memory"
                structNameToClassName(structName)
            }
            funSpecBuilder.addParameter(
                ParameterSpec.builder(paramName, typeName).build()
            )
        }

        val statement = "%T.$cName(${paramUsage.joinToString()}).checkErrorCode()"

        val threadParam = funSpecBuilder.parameters.firstOrNull { it.name in setOf("threadIndex", "thrId") }
        val withLock = when {
            threadParam != null -> "withThread(${threadParam.name}) {\n"
            cName in useAllThreads -> "withAllThreads {\n"
            else -> {
                check("Par" in cName && "Calc" !in cName || cName.matches(Regex("ConvertTo.*TextFormat")))
                null
            }
        }

        val builder = CodeBlock.builder()
        if (withLock != null) builder.add(withLock).indent()
        builder.add(statement, ClassName("dds", "Dds"))
        if (withLock != null) builder.unindent().add("\n}")

        funSpecBuilder.addCode(builder.build())
        file.addFunction(funSpecBuilder.build())
    }
    return file.build()
}
