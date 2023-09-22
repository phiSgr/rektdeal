package com.github.phisgr.dds

import java.lang.foreign.MemorySegment
import java.lang.foreign.ValueLayout

@JvmInline
value class Rank internal constructor(val encoded: Int) : Comparable<Rank> {

    class Array(
        val memory: MemorySegment,
    ) {
        @JvmName("get")
        operator fun get(index: Int): Rank = Rank(memory.getInt(index))

        @JvmName("set")
        operator fun set(index: Int, value: Rank) {
            memory.setInt(index, value.encoded)
        }

        fun size(): Int = memory.intSize

        fun toString(count: Int): String = (0..<count).joinToString(", ", "[", "]") { index ->
            when (val encoded = memory.getInt(index)) {
                0 -> "null"
                else -> Rank(encoded).toString()
            }
        }

        override fun toString(): String = toString(size())

        fun clear() {
            memory.fill(0.toByte())
        }
    }

    companion object {
        val strings = (2..14).map { Rank(it).toChar().toString() }

        @JvmStatic
        @JvmName("fromChar")
        fun fromChar(c: Char) = Rank(when (c) {
            'A' -> 14
            'K' -> 13
            'Q' -> 12
            'J' -> 11
            'T' -> 10
            else -> (c - '0').also { require(it in 2..9) }
        })

        @JvmStatic
        fun toChar(rank: Int) = Rank(rank).toChar()

        @JvmStatic
        fun toString(rank: Int) = Rank(rank).toString()

        val A = Rank(14)
        val K = Rank(13)
        val Q = Rank(12)
        val J = Rank(11)
        val T = Rank(10)
    }

    override fun toString(): String = strings[encoded - 2]

    fun toChar(): Char = when (encoded) {
        14 -> 'A'
        13 -> 'K'
        12 -> 'Q'
        11 -> 'J'
        10 -> 'T'
        else -> ('0' + encoded)
    }

    override operator fun compareTo(other: Rank): Int = encoded.compareTo(other.encoded)
}

class CIntArray(val memory: MemorySegment) {
    operator fun get(index: Int): Int = memory.getInt(index)
    operator fun set(index: Int, value: Int) {
        memory.setInt(index, value)
    }

    fun size(): Int = memory.intSize

    override fun toString(): String = toString(size())

    fun toString(count: Int): String = (0..<count).joinToString(separator = ", ", prefix = "[", postfix = "]") {
        this[it].toString()
    }
}

/**
 * Wraps around a [MemorySegment] with the layout `char[size][maxLength]`.
 */
data class CStringArray(val memory: MemorySegment, val maxLength: Long) {
    operator fun get(index: Int): String = memory.getUtf8String(index * maxLength)
    operator fun set(index: Int, value: String) {
        memory.asSlice(index * maxLength, maxLength).setUtf8String(0, value)
    }

    fun size(): Int = Math.toIntExact(memory.byteSize() / maxLength)
    override fun toString(): String = toString(size())
    fun toString(count: Int): String = (0..<count).joinToString(separator = ", ", prefix = "[", postfix = "]") {
        this[it]
    }
}

/**
 * Bit vector encoding the cards in one suit.
 *
 * E.g. A986 is encoded as `0b100001101000000` (17216).
 * ```
 * AKQJT98765432--
 * 100001101000000
 * ```
 */
@JvmInline
value class Holding(val encoded: Int) {
    companion object {
        @JvmStatic
        fun withCard(holding: Int, newCard: Int): Int = Holding(holding).withCard(Rank(newCard)).encoded

        @JvmStatic
        fun toString(holding: Int): String = Holding(holding).toString()

        @JvmStatic
        @JvmName("parse")
        operator fun invoke(s: String): Holding = s.fold(Holding(0)) { holding, char ->
            holding.withCard(Rank.fromChar(char))
        }
    }

    class Array(
        val memory: MemorySegment,
    ) {
        @JvmName("get")
        operator fun get(index: Int): Holding = memory.getInt(index).toHolding()

        @JvmName("set")
        operator fun set(index: Int, value: Holding) {
            memory.setInt(index, value.encoded)
        }

        fun size(): Int = memory.intSize

        fun toString(count: Int): String = (0..<count).joinToString(", ", "[", "]") {
            this[it].toString()
        }

        override fun toString(): String = toString(size())
    }

    override fun toString(): String {
        val size = Integer.bitCount(encoded)
        return StringBuilder(size).apply {
            for (it in 14 downTo 2) {
                if (encoded and (1 shl it) != 0) append(Rank(it).toChar())
            }
        }.toString()
    }

    fun withCard(rank: Rank) = Holding(encoded.or(1 shl rank.encoded))
}

/**
 * Wrapper around the field `unsigned int cards[DDS_HANDS][DDS_SUITS]`
 *
 * Holds 16 [Holding] encoded ints.
 */
class Cards(val memory: MemorySegment) {

    @JvmName("get")
    operator fun get(hand: Direction, suit: Suit): Holding =
        Holding(memory.get(ValueLayout.JAVA_INT, (hand.encoded * 4 + suit.encoded) * 4L))

    @JvmName("set")
    operator fun set(hand: Direction, suit: Suit, value: Holding) {
        memory.set(ValueLayout.JAVA_INT, (hand.encoded * 4 + suit.encoded) * 4L, value.encoded)
    }

    override fun toString(): String =
        DIRECTIONS.joinToString(separator = ", ", prefix = "{", postfix = "}") { hand ->
            val pbn = SUITS.joinToString(separator = ".") { suit -> this[hand, suit].toString() }
            "$hand: $pbn"
        }
}

class DdTable(val memory: MemorySegment) {
    companion object {
        private val strainsAscending = SUITS.reversed() + N

        val formatString = """
               C  D  H  S  N
            N%3d%3d%3d%3d%3d
            S%3d%3d%3d%3d%3d
            E%3d%3d%3d%3d%3d
            W%3d%3d%3d%3d%3d
        """.trimIndent()
    }

    operator fun get(strain: Strain, hand: Direction): Int =
        memory.get(ValueLayout.JAVA_INT, (strain.encoded * 4 + hand.encoded) * 4L)

    override fun toString(): String {
        return formatString.format(
            *listOf(NORTH, SOUTH, EAST, WEST).flatMap { direction ->
                strainsAscending.map { strain ->
                    this[strain, direction]
                }
            }.toTypedArray()
        )
    }
}

enum class Vulnerability {
    NONE, BOTH, NS, EW;

    val encoded: Int = ordinal
}
