package com.github.phisgr.rektdeal

import com.github.phisgr.rektdeal.internal.requireWholeHand

/**
 * [PreDealCards], or [SmartStack]
 */
sealed class PreDeal

// PreDealCards used to be PreDealHand.
// Previously we did not allow pre-deals to be partially specified.
// I.e., it had to be a full hand with 13 cards.
// With the constraint dropped, it does not make sense to call them a `hand` anymore.
// This function is added for source compatibility.
@Suppress("FunctionName")
fun PreDealHand(s: String) = PreDealCards(s).apply { requireWholeHand() }

class PreDealCards(s: String) : PreDeal() {
    val holding = HoldingBySuit.parse(s).also {
        require(it.size <= 13)
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
