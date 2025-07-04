package com.github.phisgr.rektdeal

import com.github.phisgr.dds.*
import com.github.phisgr.rektdeal.internal.ONE_MIL
import com.github.phisgr.rektdeal.internal.shuffleWithoutPermuteHead
import java.util.function.Predicate

private fun handleDynamic(value: Any): PreDeal = when (value) {
    is String -> PreDealCards(value)
    is PreDeal -> value
    else -> throw IllegalArgumentException(
        "Accepted values are strings, ${PreDealCards::class.simpleName}, or ${SmartStack::class.simpleName}. Got $value."
    )
}

class Dealer(preDeals: Map<Direction, PreDeal>? = null) {
    /**
     * The parameters can either be a string that will be parsed as [PreDealCards],
     * or a [PreDeal] value (i.e. [PreDeal] or [PreDealCards].
     */
    @Suppress("LocalVariableName")
    constructor(N: Any? = null, E: Any? = null, S: Any? = null, W: Any? = null) : this(
        listOf(
            NORTH to N,
            EAST to E,
            SOUTH to S,
            WEST to W,
        ).filter { (_, value) ->
            value != null
        }.associate { (key, value) ->
            key to handleDynamic(value!!)
        }
    )

    /**
     * [leftCards] - [preparedSmartStack]
     */
    private val cardsToDeal: ByteArray

    /**
     * [HoldingBySuit.WHOLE_DECK] - [preDeals]
     */
    private val leftCards: HoldingBySuit

    private val smartStackDirection: Int
    private val preparedSmartStack: PreparedSmartStack?

    private var deal: Deal

    private val preDeals: LongArray = LongArray(4)

    init {
        var leftCards = HoldingBySuit.WHOLE_DECK

        var smartStack: Pair<Direction, SmartStack>? = null
        (preDeals ?: emptyMap()).forEach { (direction, preDeal) ->
            when (preDeal) {
                is PreDealCards -> {
                    leftCards = leftCards.removeCards(preDeal.holding)
                    this.preDeals[direction.encoded] = preDeal.holding.encoded
                    require(leftCards.size == SIZE - this.preDeals.sumOf { it.countOneBits() }) {
                        "Overlapping cards in pre-dealt hands."
                    }
                }

                is SmartStack -> {
                    require(smartStack == null) { "Only one SmartStack allowed." }
                    smartStack = Pair(direction, preDeal)
                }
            }
        }

        this.leftCards = leftCards

        when (val pair = smartStack) {
            null -> {
                smartStackDirection = -1
                preparedSmartStack = null
            }
            else -> {
                val (direction, ss) = pair
                smartStackDirection = direction.encoded
                preparedSmartStack = ss.prepare(HoldingBySuit.WHOLE_DECK.removeCards(leftCards))
            }
        }

        cardsToDeal = ByteArray(
            leftCards.size - (if (smartStack == null) 0 else 13)
        )

        if (smartStack == null) {
            leftCards.writeToArray(cardsToDeal)
        }
        deal = newDealObject()
    }

    private fun deal() {
        val smartStack = this.preparedSmartStack
        val dealCards = deal.cards
        if (smartStack != null) {
            val smartCards = smartStack()
            deal.hands[smartStackDirection].fromBitVector(smartCards)
            leftCards.removeCards(smartCards).writeToArray(cardsToDeal)
        }

        var dealt = 0
        var presorted = 0
        repeat(4) { direction ->
            val preDealtCount = if (direction == smartStackDirection) 13 else preDeals[direction].countOneBits()
            if (preDealtCount == 13) {
                // cards already written during newDealObject or deal.hands[smartStackDirection].fromBitVector
                presorted = presorted or (1 shl direction)
            } else {
                if (dealt == 0) {
                    cardsToDeal.shuffleWithoutPermuteHead(13 - preDealtCount)
                }

                val offset = direction * 13
                if (preDealtCount > 0) {
                    // the pre-dealt cards might be shuffled away
                    HoldingBySuit(preDeals[direction]).writeToArray(dealCards, offset)
                }

                val dealTo = dealt + 13 - preDealtCount

                cardsToDeal.copyInto(
                    dealCards,
                    destinationOffset = offset + preDealtCount,
                    startIndex = dealt,
                    endIndex = dealTo
                )
                dealt = dealTo
            }
        }
        deal.reset(presorted)
    }

    /**
     * The [Deal] object might be reused if [accept] criterion returned `false`.
     */
    operator fun invoke(accept: Predicate<Deal> = Predicate { true }): Deal {
        var i = 0
        while (true) {
            i++
            if (i % ONE_MIL == 0 && Thread.interrupted()) {
                throw InterruptedException()
            }
            val dealt = tryDeal(accept)
            if (dealt != null) return dealt
        }
    }

    operator fun invoke(maxTry: Int, accept: Predicate<Deal> = Predicate { true }): Deal? {
        repeat(maxTry) {
            val dealt = tryDeal(accept)
            if (dealt != null) return dealt
        }
        return null
    }

    private fun tryDeal(accept: Predicate<Deal>): Deal? {
        deal()
        val accepted = accept.test(deal)
        return if (accepted) {
            val oldDeal = deal
            deal = newDealObject()
            oldDeal
        } else {
            null
        }
    }

    /**
     * Would be simply writing to the [deal] field,
     * but we need to prove that the field is initialized in constructor.
     */
    private fun newDealObject(): Deal {
        val newDeal = Deal()
        var preSorted = if (smartStackDirection == -1) 0 else 1 shl smartStackDirection
        preDeals.forEachIndexed { direction, preDeal ->
            val holdingBySuit = HoldingBySuit(preDeal)
            if (holdingBySuit.size == 13) {
                preSorted = preSorted or (1 shl direction)
                newDeal.hands[direction].fromBitVector(holdingBySuit)
            }
        }
        newDeal.reset(preSorted)
        return newDeal
    }
}
