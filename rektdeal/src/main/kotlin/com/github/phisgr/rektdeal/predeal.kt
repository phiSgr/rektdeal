package com.github.phisgr.rektdeal

import com.github.phisgr.rektdeal.internal.requireWholeHand

/**
 * [PreDealCards], or [SmartStack]
 */
sealed class PreDeal

/**
 * PreDealCards used to be PreDealHand.
 * Previously we did not allow pre-deals to be partially specified.
 * I.e., it had to be a full hand with 13 cards.
 * With the constraint dropped, it does not make sense to call them a `hand` anymore.
 * This function is added for source compatibility.
 */
@Suppress("FunctionName")
fun PreDealHand(s: String) = PreDealCards(s).apply { requireWholeHand() }

/**
 * The input string is the suits separated by spaces.
 * Voids can be represented with a `-`.
 * The honours are upper case characters `AKQJT`.
 */
class PreDealCards(s: String) : PreDeal() {
    val holding = HoldingBySuit.parse(s).also {
        require(it.size <= 13)
    }

    /**
     * Returns a list of non-equivalent cards.
     */
    fun validCards(): List<Card> = buildList {
        var prevCard: Byte = 0
        var suitStart = 0

        // forEachIndexed goes CA,...C2,DA,...H2,SA,...S2
        // but we want the ordering here to go SA,...S2,HA,...D2,CA,...C2
        // so we reverse after each suit is done
        // then reverse the whole list

        // rather ugly and slow, but not in the hot path, so whatev
        holding.forEachIndexed { _, card ->
            if (Card(prevCard).suitEncoded != Card(card).suitEncoded) {
                subList(suitStart, size).reverse()
                suitStart = size
            }
            if (prevCard != card.inc()) {
                add(Card(card))
            }
            prevCard = card
        }
        subList(suitStart, size).reverse()
        reverse()
    }
}
