package com.github.phisgr.rektdeal

import com.github.phisgr.dds.Direction
import com.github.phisgr.rektdeal.internal.ReadOnlyIntArray
import com.github.phisgr.rektdeal.internal.sumOf
import java.util.*
import java.util.function.IntBinaryOperator

/**
 * Run a simulation for the opening lead from [hand] for the opening lead by [leader].
 *
 * [scoring] can be [PayOff.matchPoint] or [PayOff.impFromTricks]`(contract, vulnerable)`.
 */
inline fun openingLead(
    count: Int,
    hand: PreDealHand,
    leader: Direction,
    extraPreDeals: Map<Direction, PreDeal> = emptyMap(),
    crossinline accept: (Deal) -> Boolean,
    crossinline progressLog: (Int) -> Unit = { if (it % 1000 == 0) log("$it deals analyzed.") },
    contract: Contract,
    scoring: IntBinaryOperator,
): PayOff<Card> {
    if (scoring === PayOff.impFromScores) {
        System.err.println("Possible bug detected. Use `scoring = PayOff.impFromTricks(contract, vulnerable) instead`.")
    }

    val cards: List<Card> = hand.validCards()

    return openingLead(
        count,
        hand,
        leader,
        extraPreDeals,
        state = { PayOff(entries = cards, scoring) },
        updateState = PayOff<Card>::addData,
        accept = accept,
        progressLog = progressLog,
        contract = contract,
        reducer = PayOff<Card>::combine
    )
}

data class OpeningLeadStat(
    val impNonVul: PayOff<Card>,
    val impVul: PayOff<Card>,
    val mp: PayOff<Card>,
    // mutated internally
    val trickFrequencies: Map<Card, ReadOnlyIntArray>,
) {
    init {
        require(impNonVul.entries == impVul.entries)
        require(impNonVul.entries == mp.entries)
        require(impNonVul.entries.toSet() == trickFrequencies.keys)
    }

    constructor(
        cards: List<Card>,
        impNonVulScoring: IntBinaryOperator,
        impVulScoring: IntBinaryOperator,
    ) : this(
        PayOff(entries = cards, impNonVulScoring),
        PayOff(entries = cards, impVulScoring),
        PayOff(entries = cards, PayOff.matchPoint),
        cards.associateWith { ReadOnlyIntArray(IntArray(14)) },
    )

    fun combine(that: OpeningLeadStat): OpeningLeadStat = OpeningLeadStat(
        this.impNonVul.combine(that.impNonVul),
        this.impVul.combine(that.impVul),
        this.mp.combine(that.mp),
        this.trickFrequencies.keys
            .also {
                require(it == that.trickFrequencies.keys)
            }
            .associateWith { key ->
                val thisTricks = this.trickFrequencies[key]!!
                val thatTricks = that.trickFrequencies[key]!!
                ReadOnlyIntArray(
                    IntArray(14) { thisTricks[it] + thatTricks[it] }
                )
            }
    )

    fun addData(tricks: Map<Card, Int>) {
        val cards = mp.entries
        val data = PayOff.convertMapToArray(cards, tricks)
        impNonVul.addData(data)
        impVul.addData(data)
        mp.addData(data)
        cards.forEachIndexed { index, card ->
            trickFrequencies[card]!!.wrapped[data[index]]++
        }
    }

    fun averageTricks(): Map<Card, Double> {
        val total = mp.count
        return trickFrequencies.mapValues { (_, trickFrequencies) ->
            sumOf(end = 14) { it * trickFrequencies[it] } / total.toDouble()
        }
    }

    override fun toString(): String =
        """Trick Frequencies
${trickFrequencies.toTable()}
averageTricks: ${averageTricks()}

IMP (not vulnerable)
$impNonVul
IMP (vulnerable)
$impVul
Matchpoint
$mp"""
}

/**
 * Run a simulation for the opening lead from [hand] for the opening lead by [leader].
 *
 * Calculates the IMPs (vulnerable or not), MPs, and trick counts of each card.
 */
inline fun openingLead(
    count: Int,
    hand: PreDealHand,
    leader: Direction,
    extraPreDeals: Map<Direction, PreDeal> = emptyMap(),
    crossinline accept: (Deal) -> Boolean,
    crossinline progressLog: (Int) -> Unit = { if (it % 1000 == 0) log("$it deals analyzed.") },
    contract: Contract,
): OpeningLeadStat {
    val impNonVulScoring = PayOff.impFromTricks(contract, vulnerable = false)
    val impVulScoring = PayOff.impFromTricks(contract, vulnerable = true)

    val cards = hand.validCards()

    return openingLead(
        count,
        hand,
        leader,
        extraPreDeals,
        state = {
            OpeningLeadStat(
                cards,
                impNonVulScoring,
                impVulScoring,
            )
        },
        updateState = OpeningLeadStat::addData,
        accept = accept,
        progressLog = progressLog,
        contract = contract,
        reducer = OpeningLeadStat::combine
    )
}

/**
 * Opening lead simulation generic over the [state] accumulated.
 * Use [reducer] to combine the state from different threads.
 */
inline fun <T> openingLead(
    count: Int,
    hand: PreDealHand,
    leader: Direction,
    extraPreDeals: Map<Direction, PreDeal> = emptyMap(),
    crossinline state: () -> T,
    crossinline updateState: (T, Map<Card, Int>) -> Unit,
    crossinline accept: (Deal) -> Boolean,
    crossinline progressLog: (Int) -> Unit,
    contract: Contract,
    crossinline reducer: (T, T) -> T,
): T {
    val preDeals: Map<Direction, PreDeal> = extraPreDeals.toMutableMap().also {
        val oldValue = it.put(leader, hand)
        require(oldValue == null) { "extraPreDeals should not contain leader." }
    }

    val strain = contract.strain
    return multiThread(
        count = count,
        dealer = { Dealer(preDeals) },
        state = state,
        accept = accept
    ) { index, deal, threadState ->
        val tricks: Map<Card, Int> = deal.ddAllTricks(strain = strain, leader = leader)
        updateState(threadState, tricks)
        progressLog(index)
    }.reduce(reducer)
}

private fun Map<Card, ReadOnlyIntArray>.toTable() =
    (0..13).joinToString(prefix = "   ", separator = "", postfix = "\n") { String.format("%7d", it) } +
        this.entries.joinToString(separator = "\n") { (card, tricks) ->
            (0..13).joinToString(prefix = String.format("%-3s", card), separator = "") {
                // %6d can hold the result from 1 mil total deals
                // good enough i guess
                String.format(" %6d", tricks[it])
            }
        }
