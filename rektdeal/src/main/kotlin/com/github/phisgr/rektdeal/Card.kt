package com.github.phisgr.rektdeal

import com.github.phisgr.dds.Rank
import com.github.phisgr.dds.Suit
import com.github.phisgr.dds.toSuit

@JvmInline
value class Card internal constructor(val encoded: Byte) {
    constructor(suit: Suit, rank: Rank) : this(suit = suit.encoded, rank = rank.encoded)
    internal constructor(suit: Int, rank: Int) : this((suit.shl(4) or rank).toByte())

    val suitEnc get() = encoded.toInt() shr 4
    val suit: Suit get() = suitEnc.toSuit()
    val rankEnc get() = encoded.toInt() and 0xf
    val rank: Rank get() = Rank(rankEnc)

    val hcp: Int get() = (rankEnc - 10).coerceAtLeast(0)
    val qp: Int get() = (rankEnc - 11).coerceAtLeast(0)
    val controls: Int get() = (rankEnc - 12).coerceAtLeast(0)

    override fun toString(): String = "$suit$rank"
}
