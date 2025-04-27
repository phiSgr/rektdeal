package com.github.phisgr.rektdeal

import com.github.phisgr.rektdeal.internal.ReadOnlyIntArray
import com.github.phisgr.rektdeal.internal.flatten
import com.github.phisgr.rektdeal.internal.nestingPermutations
import com.github.phisgr.rektdeal.internal.substituteWildCard
import java.util.*
import kotlin.math.max
import kotlin.math.min

internal const val COMBINATIONS = 560 // 16C3

typealias ShapeFun<T> = (s: Int, h: Int, d: Int, c: Int) -> T

/**
 * Iterates the [560][COMBINATIONS] possible shapes,
 * loop index will be the same as the [flatten]ed value.
 */
internal inline fun iterateShapes(action: ShapeFun<Unit>) {
    for (c in 13 downTo 0) {
        val firstThreeTotal = 13 - c
        for (d in firstThreeTotal downTo 0) {
            val majors = firstThreeTotal - d
            for (h in majors downTo 0) {
                val s = majors - h
                action(s, h, d, c)
            }
        }
    }
}

class Shape internal constructor(
    private val bitset: BitSet,
    internal val minLengths: IntArray,
    internal val maxLengths: IntArray,
) {
    companion object {

        operator fun invoke(cond: ShapeFun<Boolean>): Shape {
            val table = BitSet(COMBINATIONS)

            var i = 0
            val minLengths = IntArray(4) { 13 }
            val maxLengths = IntArray(4) { 0 }
            iterateShapes { s, h, d, c ->
                if (cond(s, h, d, c)) {
                    val lengths = intArrayOf(s, h, d, c)
                    repeat(4) {
                        minLengths[it] = min(minLengths[it], lengths[it])
                        maxLengths[it] = max(maxLengths[it], lengths[it])
                    }
                    table.set(i)
                }
                i++
            }
            return Shape(table, minLengths, maxLengths)
        }

        operator fun invoke(s: String): Shape {
            val matches = shapeGroupingRegex.findAll(s).map { it.groupValues }.toList()
            require(matches.joinToString(separator = "") { it[0] } == s)
            val fragments = matches.map { groupValues ->
                if (groupValues[2] != "") {
                    groupValues[2].map(::fromChar)
                } else {
                    listOf(fromChar(groupValues[3].single()))
                }
            }

            val lengths = fragments.flatten()
            require(lengths.size == 4) { "Need 4 numbers, got $s" }
            val totalLength = lengths.sumOf { it.coerceAtLeast(0) }
            val wildcardCount = lengths.count { it == -1 }
            if (wildcardCount == 0) {
                require(totalLength == 13) { "The sum of lengths does not equal 13." }
            } else {
                require(totalLength <= 13) { "The sum of specified lengths is larger than 13." }
            }

            val table = BitSet(COMBINATIONS)
            val minLengths = IntArray(4) { 13 }
            val maxLengths = IntArray(4) { 0 }
            fragments.nestingPermutations().forEach { withWildcard ->
                withWildcard.substituteWildCard(remaining = 13 - totalLength, wildcardCount = wildcardCount)
                    .forEach { lengths ->
                        repeat(4) {
                            minLengths[it] = min(minLengths[it], lengths[it])
                            maxLengths[it] = max(maxLengths[it], lengths[it])
                        }
                        table.set(flatten(lengths[0], lengths[1], lengths[2]))
                    }
            }

            return Shape(table, minLengths = minLengths, maxLengths = maxLengths)
        }

        private fun fromChar(c: Char): Int =
            conversionTable[c.lowercaseChar()] ?: throw IllegalArgumentException("Unknown length $c")

        private const val CHARS = "x0123456789TJQK"
        private val conversionTable: Map<Char, Int> = CHARS
            .withIndex()
            .associate { (index, c) ->
                c.lowercaseChar() to (index - 1)
            }
        private val shapeGroupingRegex =
            Regex("(\\(([$CHARS]+)\\)|([$CHARS]))", RegexOption.IGNORE_CASE)

        val balanced = Shape("(4333)") + Shape("(4432)") + Shape("(5332)")
        val semiBalanced = balanced + Shape("(5422)") + Shape("(6322)")
    }

    val count: Int get() = bitset.cardinality()

    fun contains(s: Int, h: Int, d: Int): Boolean = bitset[flatten(s = s, h = h, d = d)]

    /**
     * Allows the syntax [Hand.shape] `in shape`
     */
    operator fun contains(shape: ReadOnlyIntArray): Boolean = contains(shape[0], shape[1], shape[2])
    operator fun contains(shape: List<Int>): Boolean = contains(shape[0], shape[1], shape[2])
    operator fun invoke(hand: Hand): Boolean = bitset[hand.shapeFlattened]

    /**
     * Returns the union of the two sets.
     */
    operator fun plus(that: Shape): Shape {
        val res = bitset.clone() as BitSet
        res.or(that.bitset)
        val newMinLengths = IntArray(4)
        val newMaxLengths = IntArray(4)
        repeat(4) {
            newMinLengths[it] = min(this.minLengths[it], that.minLengths[it])
            newMaxLengths[it] = max(this.maxLengths[it], that.maxLengths[it])
        }
        return Shape(res, newMinLengths, newMaxLengths)
    }

    /**
     * Returns a [Shape] that contains those which are in `this` but not [that].
     */
    operator fun minus(that: Shape): Shape {
        val res = bitset.clone() as BitSet
        res.andNot(that.bitset)

        val newMinLengths = IntArray(4) { 13 }
        val newMaxLengths = IntArray(4) { 0 }
        lengthsFromBitSet(res, newMinLengths, newMaxLengths)
        return Shape(res, newMinLengths, newMaxLengths)
    }

    /**
     * returns a [Shape] that contains those which are in both `this` and [that]
     */
    fun intersect(that: Shape): Shape {
        val res = bitset.clone() as BitSet
        res.and(that.bitset)

        val newMinLengths = IntArray(4) { 13 }
        val newMaxLengths = IntArray(4) { 0 }
        lengthsFromBitSet(res, newMinLengths, newMaxLengths)
        return Shape(res, newMinLengths, newMaxLengths)
    }

    override fun toString(): String =
        "Shape(count=${count}, " +
            "minLengths=${minLengths.contentToString()}, " +
            "maxLengths=${maxLengths.contentToString()})"

    override fun equals(other: Any?): Boolean = other is Shape && this.bitset == other.bitset
    override fun hashCode(): Int = bitset.hashCode()
}

private fun lengthsFromBitSet(
    bitset: BitSet,
    newMinLengths: IntArray,
    newMaxLengths: IntArray,
) {
    var i = 0
    iterateShapes { s, h, d, c ->
        if (bitset[i]) {
            val lengths = intArrayOf(s, h, d, c)
            repeat(4) {
                newMinLengths[it] = min(newMinLengths[it], lengths[it])
                newMaxLengths[it] = max(newMaxLengths[it], lengths[it])
            }
        }
        i++
    }
}
