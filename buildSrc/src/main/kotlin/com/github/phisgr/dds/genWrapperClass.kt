package com.github.phisgr.dds

import com.squareup.kotlinpoet.*
import java.io.File
import java.lang.foreign.MemorySegment
import java.lang.foreign.SegmentAllocator


private val needArray = mutableSetOf("parResultsDealer", "parResultsMaster")  // these two are used in sidesPar(Bin)

private fun typeOverrideOrNull(jClassName: String, fieldName: String): TypeConfig? =
    structConfigs[jClassName]?.typeOverride?.get(fieldName)

private fun TypeSpec.Builder.addProp(
    fieldName: String,
    type: String,
    jClassName: String,
    getterOnly: Boolean,
) {
    val typeOverride = typeOverrideOrNull(jClassName, fieldName)
    val t = typeOverride?.className ?: convertType[type]?.asTypeName()
    ?: throw IllegalArgumentException("Unknown type $type")

    val prop = PropertySpec.builder(fieldName, t)
    prop.getter(
        FunSpec.getterBuilder()
            .addStatement("return $jClassName.$fieldName(memory)${typeOverride?.wrap ?: ""}")
            .build()
    )

    if (!getterOnly) {
        prop.mutable().setter(
            FunSpec.setterBuilder()
                .addParameter("value", t)
                .addStatement("$jClassName.$fieldName(memory, value${typeOverride?.unwrap ?: ""})")
                .build()
        )
    }
    addProperty(prop.build())
}

private fun isStringField(fieldName: String, javaText: String): Boolean =
    // The Javadoc includes the original C++ code
    Regex("char$ws$fieldName\\[\\d+\\];").containsMatchIn(javaText)

private fun TypeSpec.Builder.addStringProp(fieldName: String, jClassName: String, getterOnly: Boolean) {
    val prop = PropertySpec.builder(fieldName, String::class)
    prop.getter(
        FunSpec.getterBuilder()
            .addStatement("return $jClassName.$fieldName(memory).getString(0)")
            .build()
    )

    if (!getterOnly) {
        prop.mutable().setter(
            FunSpec.setterBuilder()
                .addParameter("value", String::class)
                .addStatement("$jClassName.$fieldName(memory).setString(0, value)")
                .build()
        )
    }
    addProperty(prop.build())
}

private fun stringArrayRegex(fieldName: String) = Regex("char$ws$fieldName\\[\\d+\\]\\[(\\d+)\\];")
private fun TypeSpec.Builder.addStringArray(fieldName: String, jClassName: String, maxLength: Int) {
    addProperty(
        PropertySpec
            .builder(fieldName, dds4jPackage("CStringArray"))
            .getter(
                FunSpec.getterBuilder()
                    .addStatement("return CStringArray($jClassName.$fieldName(memory), maxLength = $maxLength)")
                    .build()
            )
            .build()
    )
}

private fun isIntArray(fieldName: String, javaText: String): Boolean =
    Regex("int$ws$fieldName\\[\\d+\\];").containsMatchIn(javaText)

private fun TypeSpec.Builder.addIntArray(fieldName: String, jClassName: String) {
    val t = typeOverrideOrNull(jClassName, fieldName)?.className?.nestedClass("Array")
        ?: classCIntArray
    val prop = PropertySpec.builder(fieldName, t)
    prop.getter(
        FunSpec.getterBuilder()
            .addStatement("return %T($jClassName.$fieldName(memory))", t)
            .build()
    )
    addProperty(prop.build())
}

private fun isCards(fieldName: String, javaText: String) =
    Regex("cards$", RegexOption.IGNORE_CASE).containsMatchIn(fieldName) &&
        Regex("int$ws$fieldName\\[4\\]\\[4\\]").containsMatchIn(javaText)

private fun TypeSpec.Builder.addCards(fieldName: String, jClassName: String, javaText: String) {
    val slice = if (jClassName == "ddTableDeal") {
        check(fieldName == "cards")
        check(zeroOffsetCheck("cards").containsMatchIn(javaText))
        "memory"
    } else {
        "$jClassName.$fieldName(memory)"
    }
    val prop = PropertySpec.builder(fieldName, dds4jPackage("Cards"))
    prop.getter(
        FunSpec.getterBuilder()
            .addStatement("return Cards($slice)")
            .build()
    )
    addProperty(prop.build())
}


private fun structArrayRegex(fieldName: String) = Regex("struct$ws(\\w+)$ws$fieldName\\[\\d+\\];")
private fun TypeSpec.Builder.addStructArray(
    fieldName: String,
    jClassName: String,
    jStructName: String,
) {
    val kStructName = toKtName(jStructName)
    needArray += jStructName
    addProperty(
        PropertySpec.builder(
            fieldName,
            dds4jPackage(kStructName).nestedClass("Array")
        ).getter(
            FunSpec.getterBuilder()
                .addStatement("return $kStructName.Array($jClassName.$fieldName(memory))")
                .build()
        ).build()
    )
}

private fun memoryArray(javaClassName: String): TypeSpec {
    val ktClassName = toKtName(javaClassName)
    return memoryWrapperClass("Array")
        .addFunction(
            FunSpec.constructorBuilder()
                .addParameter("allocator", SegmentAllocator::class)
                .addParameter("size", Int::class)
                .callThisConstructor("$javaClassName.allocateArray(size.toLong(), allocator)")
                .build()
        )
        .addFunction(
            FunSpec.builder("get")
                .addModifiers(KModifier.OPERATOR)
                .addParameter("index", Int::class)
                .returns(dds4jPackage(ktClassName))
                .addStatement("return $ktClassName(memory.asSlice($javaClassName.sizeof() * index, $javaClassName.layout()))")
                .build()
        )
        .addFunction(
            FunSpec.builder("size")
                .returns(Int::class)
                .addStatement("return Math.toIntExact(memory.byteSize() / $javaClassName.sizeof())")
                .build()
        )
        .addFunction(
            FunSpec.builder("toString")
                .addParameter("count", Int::class)
                .returns(String::class)
                .addStatement("""return (0..<count).joinToString(", ", "[", "]") { this[it].toString() }""")
                .build()
        )
        .addFunction(
            FunSpec.builder("toString")
                .addModifiers(KModifier.OVERRIDE)
                .returns(String::class)
                .addStatement("""return toString(size())""")
                .build()
        )
        .build()
}

// to make sure no other fields go before it, i.e. offset is always 0
private fun zeroOffsetCheck(fieldName: String) = Regex("private static final long $fieldName\\\$OFFSET = 0;")

private fun TypeSpec.Builder.addDdTable() {
    addProperty(
        PropertySpec.builder(
            "resTable",
            dds4jPackage("DdTable")
        ).getter(
            FunSpec.getterBuilder()
                // with the offset check, we can save the `resTable` getter call
                .addStatement("return DdTable(memory)")
                .build()
        ).build()
    )
}

private fun memoryWrapperClass(name: String) = TypeSpec
    .classBuilder(name)
    .primaryConstructor(
        FunSpec.constructorBuilder()
            .addParameter("memory", MemorySegment::class)
            .build()
    )
    .addProperty(
        PropertySpec.builder("memory", MemorySegment::class)
            .initializer("memory")
            .build()
    )

private val fieldRegex = Regex("public static (\\w+)$ws(\\w+)\\(MemorySegment struct\\)")
fun toWrapperClass(
    javaFile: File,
    jClassName: String,
    getterOnly: Boolean,
): TypeSpec.Builder {
    val kClassName = toKtName(jClassName)
    val javaText = javaFile.readText()
    val fields = fieldRegex.findAll(javaText)

    val spec = memoryWrapperClass(kClassName)
        .addFunction(
            FunSpec.constructorBuilder()
                .addParameter("allocator", SegmentAllocator::class)
                .callThisConstructor("$jClassName.allocate(allocator)")
                .build()
        )
    val isArray = mutableSetOf<String>()

    fields.forEach { field ->
        val name = field.groupValues[2]
        val t = field.groupValues[1]

        if (t != MemorySegment::class.simpleName) {
            spec.addProp(fieldName = name, type = t, jClassName = jClassName, getterOnly)
        } else {
            if (isStringField(fieldName = name, javaText = javaText)) {
                spec.addStringProp(fieldName = name, jClassName = jClassName, getterOnly = getterOnly)
            } else if (isIntArray(fieldName = name, javaText = javaText)) {
                isArray += name
                spec.addIntArray(fieldName = name, jClassName = jClassName)
            } else {
                val structArrayMatch = structArrayRegex(name).findAll(javaText).toList()
                if (structArrayMatch.isNotEmpty()) {
                    isArray += name
                    val match = structArrayMatch.single()
                    spec.addStructArray(
                        name,
                        jClassName,
                        jStructName = match.groupValues[1],
                    )
                } else {
                    val stringArrayMatch = stringArrayRegex(name).findAll(javaText).toList()
                    if (stringArrayMatch.isNotEmpty()) {
                        isArray += name
                        val match = stringArrayMatch.single()
                        spec.addStringArray(
                            name,
                            jClassName,
                            match.groupValues[1].toInt(),
                        )
                    } else if (isCards(name, javaText)) {
                        spec.addCards(name, jClassName, javaText)
                    } else {
                        check(jClassName == "ddTableResults")
                        check(name == "resTable")
                        check(javaText.contains("int resTable[5][4];"))
                        check(zeroOffsetCheck("resTable").containsMatchIn(javaText))
                        spec.addDdTable()
                    }
                }
            }
        }
    }

    val toStringFun = FunSpec.builder("toString")
        .addModifiers(KModifier.OVERRIDE)
        .returns(String::class)

    when (val toStringOverride = structConfigs[jClassName]?.toStringOverride) {
        null -> {
            val limitField = structConfigs[jClassName]?.limitField

            limitField?.let { toStringFun.addStatement("val _limit = $it") }
            toStringFun.addStatement(
                "return %P",
                fields
                    .joinToString(", ", prefix = "$kClassName(", postfix = ")") {
                        val fieldName = it.groupValues[2]
                        if (limitField != null && fieldName in isArray) {
                            "${fieldName}=\${${fieldName}.toString(_limit)}"
                        } else {
                            "${fieldName}=\$${fieldName}"
                        }
                    }
            )
        }

        else -> toStringFun.toStringOverride()
    }
    spec.addFunction(
        toStringFun.build()
    )
    return spec
}

fun genTypes(sources: List<File>): FileSpec {
    val file = FileSpec.builder(DDS4J_PACKAGE, "genTypes").indent("    ")
    val types = sources.associate {
        val javaClassName = it.name.removeSuffix(".java")
        javaClassName to toWrapperClass(
            javaFile = it,
            jClassName = javaClassName,
            getterOnly = structConfigs[javaClassName]?.getterOnly ?: true
        )
    }

    types
        .forEach { (javaClassName, t) ->
            file.addImport("dds", javaClassName)
            if (javaClassName in needArray) {
                t.addType(memoryArray(javaClassName))
            }

            file.addType(t.build())
        }
    return file.build()
}
