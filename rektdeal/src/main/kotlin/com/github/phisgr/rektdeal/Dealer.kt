package com.github.phisgr.rektdeal

import com.github.phisgr.dds.*
import com.github.phisgr.rektdeal.internal.ONE_MIL
import java.util.function.Predicate

private fun handleDynamic(value: Any): PreDeal = when (value) {
    is String -> PreDealHand(value)
    is PreDeal -> value
    else -> throw IllegalArgumentException(
        "Accepted values are strings, ${PreDealHand::class.simpleName}, or ${SmartStack::class.simpleName}. Got $value."
    )
}

class Dealer(predeal: Map<Direction, PreDeal>? = null) {
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
     * [leftCards] - [smartStack]
     */
    private val cardsToDeal: ByteArray

    /**
     * [HoldingBySuit.WHOLE_DECK] - [PreDealHand]s
     */
    private val leftCards: HoldingBySuit

    private var smartStackDirection: Int = -1
    private var smartStack: SmartStack? = null

    private val presorted: Int
    private var deal: Deal

    /**
     * The encoded direction for each of the [preDeals].
     * If [smartStack] is used, an extra element of [smartStackDirection] is appended at the end.
     */
    private val preDealDirections: IntArray
    private val preDeals: LongArray

    init {
        var leftCards = HoldingBySuit.WHOLE_DECK

        val preDeals = mutableListOf<Long>()
        val preDealDirections = mutableListOf<Int>()

        (predeal ?: emptyMap()).forEach { (direction, preDeal) ->
            when (preDeal) {
                is PreDealHand -> {
                    preDeals.add(preDeal.holding.encoded)
                    preDealDirections.add(direction.encoded)

                    leftCards = leftCards.removeCards(preDeal.holding)
                    require(leftCards.size == SIZE - preDeals.size * 13) {
                        "Overlapping cards in pre-dealt hands."
                    }
                }

                is SmartStack -> {
                    require(smartStackDirection == -1) { "Only one SmartStack allowed." }
                    smartStackDirection = direction.encoded
                    smartStack = preDeal
                }
            }
        }

        this.leftCards = leftCards
        this.preDeals = preDeals.toLongArray()

        if (smartStackDirection != -1) preDealDirections.add(smartStackDirection)
        this.preDealDirections = preDealDirections.toIntArray()

        smartStack?.prepare(preDealt = HoldingBySuit.WHOLE_DECK.removeCards(leftCards))

        cardsToDeal = ByteArray(
            leftCards.size - (if (smartStack == null) 0 else 13)
        )

        if (smartStack == null) {
            setUpCards(leftCards)
        }
        presorted = this.preDealDirections.fold(0) { acc, direction -> acc or (1 shl direction) }
        deal = newDealObject()
    }

    private fun setUpCards(leftCards: HoldingBySuit) {
        leftCards.forEachIndexed { index, card ->
            cardsToDeal[index] = card
        }
    }

    private fun deal() {
        val smartStack = this.smartStack
        val dealCards = deal.cards
        if (smartStack != null) {
            val smartCards = smartStack()
            dealFromHolding(smartCards, smartStackDirection, dealCards, deal[smartStackDirection])
            setUpCards(leftCards.removeCards(smartCards))
        }
        cardsToDeal.shuffle()

        var count = 0
        repeat(4) { direction ->
            if (direction !in preDealDirections) {
                cardsToDeal.copyInto(
                    dealCards,
                    destinationOffset = direction * 13,
                    startIndex = count * 13,
                    endIndex = (count + 1) * 13
                )
                count++
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

    private fun dealFromHolding(cards: HoldingBySuit, direction: Int, dealCards: ByteArray, hand: Hand) {
        val offset = 13 * direction
        cards.forEachIndexed(startIndex = offset, afterEachSuit = { index, suit ->
            if (suit != 3) {
                hand[suit].end = index
                hand[suit + 1].start = index
            }
        }) { index, card ->
            dealCards[index] = card
        }
    }

    /**
     * Would be simply writing to the [deal] field,
     * but we need to prove that the field is initialized in constructor.
     */
    private fun newDealObject(): Deal {
        val newDeal = Deal(presorted)
        preDeals.forEachIndexed { index, preDeal ->
            val direction = preDealDirections[index]
            dealFromHolding(HoldingBySuit(preDeal), direction = direction, newDeal.cards, newDeal[direction])
        }
        return newDeal
    }
}
