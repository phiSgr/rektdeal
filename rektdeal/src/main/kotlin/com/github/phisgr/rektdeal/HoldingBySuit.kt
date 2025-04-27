package com.github.phisgr.rektdeal

import com.github.phisgr.dds.Holding
import com.github.phisgr.dds.Rank
import com.github.phisgr.dds.Suit

private const val MASK = 1.shl(15) - 4

/**
 * The [Holding] encoding repeated 4 times, spades in the least significant bits.
 */
@JvmInline
value class HoldingBySuit(val encoded: Long) {
    companion object {
        fun parse(s: String): HoldingBySuit {
            var encoded = 0L
            s.split(' ').forEachIndexed { suit, cards ->
                if (cards != "-") {
                    val suitOffset = suit * 16
                    cards.forEach { c ->
                        encoded = encoded.or(1L shl (suitOffset + Rank.fromChar(c).encoded))
                    }
                }
            }
            return HoldingBySuit(encoded)
        }

        // MASK repeated 4 times
        val WHOLE_DECK: HoldingBySuit = HoldingBySuit(0x7ffc7ffc7ffc7ffc)
    }

    fun noOverlap(suit: Int, holding: Int): Boolean = (holding.toLong() shl (suit * 16)).and(encoded) == 0L

    override fun toString(): String = (0..3).joinToString(separator = " ") {
        val holding = Holding(encoded.shr(16 * it).toInt()).toString()
        if (holding == "") "-" else holding
    }

    fun removeCards(that: HoldingBySuit): HoldingBySuit = HoldingBySuit(this.encoded and that.encoded.inv())
    val size: Int get() = encoded.countOneBits()

    inline fun forEachIndexed(
        startIndex: Int = 0,
        afterEachSuit: (index: Int, suit: Int) -> Unit = { _, _ -> },
        action: (index: Int, card: Byte) -> Unit,
    ) {
        var i = startIndex
        repeat(4) { suit ->
            val prefix = suit * 16
            var cardBit = 1L shl (14 + prefix)
            for (rank in 14 downTo 2) {
                if (encoded and cardBit != 0L) {
                    action(i, (prefix + rank).toByte())
                    i++
                }
                cardBit = cardBit shr 1
            }
            afterEachSuit(i, suit)
        }
    }

    fun withCard(card: Byte): HoldingBySuit = HoldingBySuit(encoded or (1L shl card.toInt()))

    fun getHolding(suit: Suit): Holding = Holding(
        (encoded shr (suit.encoded * 16)).toInt() and MASK
    )

    internal fun writeToArray(array: ByteArray, offset: Int = 0) {
        forEachIndexed(startIndex = offset) { index, card ->
            array[index] = card
        }
    }
}
