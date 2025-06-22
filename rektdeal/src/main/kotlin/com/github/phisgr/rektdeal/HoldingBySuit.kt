package com.github.phisgr.rektdeal

import com.github.phisgr.dds.Holding
import com.github.phisgr.dds.Rank
import com.github.phisgr.dds.Suit

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

        val WHOLE_DECK: HoldingBySuit = HoldingBySuit(0x7ffc7ffc7ffc7ffc)
    }

    override fun toString(): String = (0..3).joinToString(separator = " ") {
        val holding = getHolding(it).toString()
        if (holding == "") "-" else holding
    }

    fun removeCards(that: HoldingBySuit): HoldingBySuit = HoldingBySuit(this.encoded and that.encoded.inv())
    val size: Int get() = encoded.countOneBits()

    inline fun forEachIndexed(
        startIndex: Int = 0,
        action: (index: Int, card: Byte) -> Unit,
    ) {

        var bits = encoded
        var index = startIndex
        while (bits != 0L) {
            val pos = 63 - bits.countLeadingZeroBits()
            action(index, pos.toByte())
            bits = bits and (1L shl pos).inv()

            index++
        }
    }

    fun withCard(card: Byte): HoldingBySuit = HoldingBySuit(encoded or (1L shl card.toInt()))
    fun noOverlap(suit: Int, holding: Int): Boolean = (holding.toLong() shl (suit * 16)).and(encoded) == 0L

    fun withHolding(suit: Int, holding: Short): HoldingBySuit = HoldingBySuit(
        (holding.toLong() shl (suit * 16)).or(encoded)
    )

    fun getHolding(suit: Suit): Holding = getHolding(suit.encoded)
    private fun getHolding(suit: Int): Holding = Holding(
        // toShort to take last 16 bits
        // toInt to fit the representation inherited from DDS
        (encoded shr (suit * 16)).toShort().toInt()
    )

    internal fun countCardsOfOrBelowSuit(suit: Int): Int {
        val mask = (-1L).shl(16 * suit)
        return encoded.and(mask).countOneBits()
    }

    internal fun writeToArray(array: ByteArray, offset: Int = 0) {
        forEachIndexed(startIndex = offset) { index, card ->
            array[index] = card
        }
    }
}
