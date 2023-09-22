package com.github.phisgr.rektdeal

import com.github.phisgr.dds.Rank
import java.util.*
import java.util.concurrent.ThreadLocalRandom

private data class LV(val length: Int, val value: Int)

private class Pattern(
    val lengths: IntArray,
    val values: IntArray,
)

private class HoldingCardList : RankList() {
    override var size: Int = 0
        private set

    private val cards: IntArray = IntArray(13)
    override fun get(index: Int): Rank {
        Objects.checkIndex(index, size)
        return Rank(cards[index])
    }

    fun setHolding(holding: Int) {
        size = holding.countOneBits()
        var index = 0
        for (i in 14 downTo 2) {
            if (holding.and(1 shl i) != 0) {
                cards[index] = i
                index++
            }
        }
    }

}

/**
 * Generates hands that has the [shape], and the sum of [evaluator]'s result of the four suits is `in` [values]
 */
class SmartStack(
    private val shape: Shape,
    private val evaluator: Evaluator,
    private val values: Iterable<Int>,
) : PreDeal() {
    private lateinit var holdings: Array<Map<LV, IntArray>>

    /**
     * Allows [prepare] to be called by multiple [Deal]s,
     * as long as they are the same value.
     */
    private var preDealt: Long = -1 // not a valid preDealt value

    private lateinit var cumSum: LongArray
    private lateinit var patterns: List<Pattern>


    @Synchronized
    internal fun prepare(preDealt: HoldingBySuit) {
        if (this.preDealt == preDealt.encoded) return
        check(this.preDealt == -1L)

        val holdings = Array(4) { mutableMapOf<LV, MutableList<Int>>() }

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
                    holdings[suit].getOrPut(LV(length, value)) { mutableListOf() }.add(holding)
                }
            }
        }

        var count = 0L
        var cumSum = LongArray(64)
        val patterns = arrayListOf<Pattern>()
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
                                    Pattern(
                                        intArrayOf(sLength, hLength, dLength, cLength),
                                        intArrayOf(sValue, hValue, dValue, cValue)
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

        this.cumSum = cumSum.copyOf(index)
        this.patterns = patterns
        this.preDealt = preDealt.encoded
        this.holdings = Array(4) {
            holdings[it].run {
                mapValuesTo(HashMap.newHashMap(size)) { (_, value) ->
                    value.toIntArray()
                }
            }
        }
    }

    operator fun invoke(): HoldingBySuit {
        if (this.preDealt == -1L) {
            prepare(HoldingBySuit(0))
        }

        val handIndex = ThreadLocalRandom.current().nextLong(cumSum.last())

        val index = cumSum.binarySearch(handIndex).let { index ->
            if (index < 0) (-index - 1) else (index + 1)
        }
        val pattern = patterns[index]

        // cumSum[index] - cumSum[index - 1] == spades.size * hearts.size * diamonds.size * clubs.size
        // remaining == spades.size * (hearts.size * (d.size * cIndex + dIndex) + hIndex) + sIndex
        var remaining = (handIndex - if (index == 0) 0 else cumSum[index - 1]).toInt()
        var cards = 0L
        repeat(4) { suit ->
            val holding = holdings[suit][LV(pattern.lengths[suit], pattern.values[suit])]!!
            val holdingSize = holding.size

            val holdingIndex = remaining % holdingSize
            remaining /= holdingSize

            cards += holding[holdingIndex].toLong().shl(16 * suit)
        }
        return HoldingBySuit(cards)
    }

    private fun valueInRange(v: Int) = if (values is IntRange) {
        v in values // member method comparing `start` and `endInclusive`
    } else {
        v in values // extension method iterating the members
    }

}
