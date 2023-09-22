package com.github.phisgr.rektdeal

/**
 * [PreDealHand] or [SmartStack]
 */
sealed class PreDeal

class PreDealHand(s: String) : PreDeal() {
    val holding = HoldingBySuit.parse(s).also {
        require(13 == it.encoded.countOneBits())
    }

    /**
     * Returns a list of non-equivalent cards.
     */
    fun validCards(): List<Card> = buildList {
        var prevCard: Byte = 0
        holding.forEachIndexed { _, card ->
            if (prevCard != card.inc()) {
                add(Card(card))
            }
            prevCard = card
        }
    }
}
