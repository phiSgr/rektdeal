package com.github.phisgr.dds

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FunSpec
import org.gradle.configurationcache.extensions.capitalized

val ws = "[\\n\\r\\s]+" //whitespace

val convertType = mapOf(
    "int" to Int::class,
    "boolean" to Boolean::class,
)

data class TypeConfig(
    val className: ClassName,
    val wrap: String = ".to${className.simpleName}()",
    val unwrap: String = ".encoded",
)

data class StructConfig(
    val name: String? = null,
    val getterOnly: Boolean = true,
    val typeOverride: Map<String, TypeConfig> = emptyMap(),
    val toStringOverride: (FunSpec.Builder.() -> Unit)? = null,
    val limitField: String? = null,
)

val strainType = TypeConfig(dds4jPackage("Strain"))
val directionType = TypeConfig(dds4jPackage("Direction"))
val suitType = TypeConfig(dds4jPackage("Suit"))
val rankType = TypeConfig(dds4jPackage("Rank"))
val holdingType = TypeConfig(dds4jPackage("Holding"))
val seatsType = TypeConfig(dds4jPackage("Seats"))
val denomType = TypeConfig(
    dds4jPackage("Strain"),
    wrap = ".toContractTypeDenom()",
    // does not exist, because there is no setter for it
    unwrap = ".asContractTypeDenom()",
)

val trumpAndFirst = mapOf(
    "trump" to strainType,
    "first" to directionType,
    "currentTrickSuit" to suitType,
    "currentTrickRank" to rankType,
)
val suitRank = mapOf(
    "suit" to suitType,
    "rank" to rankType,
)
val suitRankEquals = suitRank + mapOf("equals" to holdingType)

val structConfigs = mapOf(
    "ddTableResults" to StructConfig(toStringOverride = {
        // This struct has only one field, adding the `DdTableResults(...)` wrapping is ugly
        addStatement("return resTable.toString()")
    }),
    "DDSInfo" to StructConfig("DdsInfo"),
    "ddTableDealPBN" to StructConfig(getterOnly = false),
    "deal" to StructConfig(getterOnly = false, typeOverride = trumpAndFirst),
    "dealPBN" to StructConfig(getterOnly = false, typeOverride = trumpAndFirst),
    "playTracePBN" to StructConfig(getterOnly = false),
    "playTraceBin" to StructConfig(getterOnly = false, limitField = "number", typeOverride = suitRank),
    "futureTricks" to StructConfig(limitField = "cards", typeOverride = suitRankEquals),
    "ddTablesRes" to StructConfig(limitField = "noOfBoards"),
    "solvedBoards" to StructConfig(limitField = "noOfBoards"),
    "ddTableDeals" to StructConfig(getterOnly = false, limitField = "noOfTables"),
    "ddTableDealsPBN" to StructConfig(getterOnly = false, limitField = "noOfTables"),
    "solvedPlay" to StructConfig(limitField = "number"),
    "playTracesPBN" to StructConfig(getterOnly = false, limitField = "noOfBoards"),
    "playTracesBin" to StructConfig(getterOnly = false, limitField = "noOfBoards"),
    "solvedPlays" to StructConfig(limitField = "noOfBoards"),
    "boards" to StructConfig(getterOnly = false, limitField = "noOfBoards"),
    "boardsPBN" to StructConfig(getterOnly = false, limitField = "noOfBoards"),
    "parResultsMaster" to StructConfig(limitField = "number"),
    "parResultsDealer" to StructConfig(limitField = "number"),
    "contractType" to StructConfig(
        typeOverride = mapOf(
            "seats" to seatsType, "denom" to denomType
        )
    )
)

fun toKtName(jClassName: String): String = structConfigs[jClassName]?.name ?: jClassName.capitalized()

const val DDS4J_PACKAGE = "com.github.phisgr.dds"
fun dds4jPackage(className: String): ClassName = ClassName(DDS4J_PACKAGE, className)

val classCIntArray = dds4jPackage("CIntArray")
