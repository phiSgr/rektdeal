package com.github.phisgr.rektdeal

import com.github.phisgr.dds.Rank
import org.agrona.collections.Long2ObjectHashMap
import java.util.*
import java.util.concurrent.ThreadLocalRandom

private data class LV(val length: Int, val value: Int)

/**
 * A utility class to convert a `Holding` encoded int into a List.
 * This makes getting nth card in a suit easier.
 */
private class HoldingCardList : RankList() {
    override var size: Int = 0
        private set

    private val cards = ByteArray(13)
    override fun get(index: Int): Rank {
        Objects.checkIndex(index, size)
        return Rank(cards[index].toInt())
    }

    fun setHolding(holding: Int) {
        size = holding.countOneBits()
        var bits = holding
        var index = 0
        while (bits != 0) {
            val pos = 31 - bits.countLeadingZeroBits()
            cards[index] = pos.toByte()
            bits = bits and (1 shl pos).inv()

            index++
        }
    }

}

/**
 * A [SmartStack] accelerates deal generation via precomputation.
 * Instead of only filtering from random deals, the rare hand in the deal is directly generated.
 *
 * Only one [SmartStack] is allowed per [Dealer] - applying the algorithm twice naively skews probabilities.
 * However, you *can* still use an `accept` filter alongside a [SmartStack] without issue.
 *
 * Generates hands with the given [shape] and [evaluator] sum within [values].
 *
 * Thread-safe.
 */
class SmartStack(
    private val shape: Shape,
    private val evaluator: Evaluator,
    private val values: Iterable<Int>,
) : PreDeal() {
    private val prepared = HashMap<Long, PreparedSmartStack>()

    @Synchronized
    internal fun prepare(preDealt: HoldingBySuit): PreparedSmartStack =
        prepared.computeIfAbsent(preDealt.encoded) { createPrepared(preDealt) }

    private fun createPrepared(preDealt: HoldingBySuit): PreparedSmartStack {
        val holdings = Array(4) { mutableMapOf<LV, MutableList<Short>>() }

        val cardList = HoldingCardList()
        repeat(1 shl 13) {
            val holding = it shl 2
            val length = it.countOneBits()

            cardList.setHolding(holding)
            val value = evaluator.evaluate(cardList)

            repeat(4) { suit ->
                if (shape.minLengths[suit] <= length &&
                    shape.maxLengths[suit] >= length &&
                    preDealt.noOverlap(suit, holding)
                ) {
                    holdings[suit].getOrPut(LV(length, value)) { mutableListOf() }.add(holding.toShort())
                }
            }
        }

        var count = 0L
        var cumSum = LongArray(64)
        val patterns = arrayListOf<IntArray>()
        var index = 0

        holdings[0].forEach { (spadesLv, spades) ->
            holdings[1].forEach { (heartsLv, hearts) ->
                holdings[2].forEach { (diamondsLv, diamonds) ->
                    val (sLength, sValue) = spadesLv
                    val (hLength, hValue) = heartsLv
                    val (dLength, dValue) = diamondsLv
                    if (sLength + hLength + dLength <= 13 && shape.contains(sLength, hLength, dLength)) {
                        holdings[3].forEach { (clubsLv, clubs) ->
                            val (cLength, cValue) = clubsLv
                            if (cLength == 13 - sLength - hLength - dLength &&
                                valueInRange(sValue + hValue + dValue + cValue)
                            ) {
                                count += spades.size * hearts.size * diamonds.size * clubs.size

                                if (index >= cumSum.size) { // A poor man's ArrayList<long>
                                    val newSize = cumSum.size * 2
                                    cumSum = cumSum.copyOf(newSize)
                                }

                                cumSum[index] = count
                                patterns.add(
                                    intArrayOf(
                                        sValue,
                                        hValue,
                                        dValue,
                                        cValue,
                                        sLength +
                                            (hLength +
                                                (dLength +
                                                    cLength.shl(4)).shl(4)).shl(4)
                                    )
                                )
                                index++
                            }
                        }
                    }
                }
            }
        }

        require(index > 0) {
            "No hand can satisfy the conditions."
        }

        return PreparedSmartStack(
            Array(4) { suit ->
                val holdingsMap = holdings[suit]
                val fastMap = Long2ObjectHashMap<ShortArray>(holdingsMap.size, 0.65f)
                holdingsMap.forEach { (key, value) ->
                    fastMap.put(key.length.toLong().shl(32) + key.value, value.toShortArray())
                }
                fastMap
            },
            cumSum.copyOf(index),
            IntArray(5 * patterns.size) { patterns[it / 5][it % 5] },
        )
    }

    private fun valueInRange(v: Int) = when (val vs = values) {
        is IntRange -> v in vs  // member method comparing `start` and `endInclusive`
        else -> v in vs         // extension method iterating the members
    }

}

class PreparedSmartStack internal constructor(
    private val holdings: Array<Long2ObjectHashMap<ShortArray>>,
    private val cumSum: LongArray,
    private val patterns: IntArray,
) {

    operator fun invoke(): HoldingBySuit {
        val handIndex = ThreadLocalRandom.current().nextLong(cumSum.last())

        val index = cumSum.binarySearch(handIndex).let { index ->
            if (index < 0) (-index - 1) else (index + 1)
        }
        val patternOffset = index * 5

        // cumSum[index] - cumSum[index - 1] == spades.size * hearts.size * diamonds.size * clubs.size
        // remaining == spades.size * (hearts.size * (d.size * cIndex + dIndex) + hIndex) + sIndex
        var remaining = (handIndex - if (index == 0) 0 else cumSum[index - 1]).toInt()
        var cards = HoldingBySuit(0L)
        repeat(4) { suit ->
            val length = patterns[patternOffset + 4].shr(4 * suit) and 0xf
            val value = patterns[patternOffset + suit]

            val holding = holdings[suit][length.toLong().shl(32) + value]!!
            val holdingSize = holding.size

            val holdingIndex = remaining % holdingSize
            remaining /= holdingSize

            cards = cards.withHolding(suit, holding[holdingIndex])
        }
        return cards
    }
}

