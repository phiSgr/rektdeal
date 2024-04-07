package com.github.phisgr.dds

import java.lang.foreign.MemorySegment

open class Seats

/**
 * Removing the internal visibility so that rektdeal code can
 * call the constructor, instead of using [com.github.phisgr.dds.internal.toRank] which has the range check.
 */
@JvmInline
value class Rank /*internal constructor*/(val encoded: Int) {
    class Array(
        val memory: MemorySegment,
    ) {
        @JvmName("get")
        operator fun get(index: Int): Rank = STUB()

        @JvmName("set")
        operator fun set(index: Int, value: Rank) {
            STUB()
        }

        fun clear() {
            STUB()
        }
    }

    companion object {
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

        val A = Rank(14)
        val K = Rank(13)
        val Q = Rank(12)
        val J = Rank(11)
        val T = Rank(10)
    }

    fun toChar(): Char = STUB()

    operator fun compareTo(that: Rank): Int = STUB()
}

class CIntArray(val memory: MemorySegment) {
    operator fun get(index: Int): Int = STUB()
    operator fun set(index: Int, value: Int) {
        STUB()
    }
}

@JvmInline
value class Holding(val encoded: Int) {
    companion object {
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
        operator fun get(index: Int): Holding = STUB()

        @JvmName("set")
        operator fun set(index: Int, value: Holding) {
            STUB()
        }
    }

    fun withCard(rank: Rank) = Holding(encoded.or(1 shl rank.encoded))
}

class Cards(val memory: MemorySegment) {

    @JvmName("get")
    operator fun get(hand: Direction, suit: Suit): Holding = STUB()

    @JvmName("set")
    operator fun set(hand: Direction, suit: Suit, value: Holding) {
        STUB()
    }
}

class DdTable(val memory: MemorySegment) {
    operator fun get(strain: Strain, hand: Direction): Int = STUB()
}

enum class Vulnerability {
    NONE, BOTH, NS, EW;

    val encoded: Int = ordinal
}
