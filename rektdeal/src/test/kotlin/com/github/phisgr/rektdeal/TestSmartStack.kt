package com.github.phisgr.rektdeal

import com.github.phisgr.dds.Rank
import com.github.phisgr.dds.SUITS
import com.github.phisgr.logTimeMs
import com.github.phisgr.rektdeal.internal.flatten
import org.junit.jupiter.api.assertThrows
import kotlin.test.Test
import kotlin.test.assertEquals


class TestSmartStack {

    /**
     * https://github.com/anntzer/redeal/blob/main/examples/deal_gambling.py
     *
     * See also `examples/gambling.ipynb`
     */
    @Test
    fun testGambling() {
        val gamblingShape = Shape { s, h, d, c ->
            s <= 3 && h <= 3 && (d >= 7 && c <= 4 || d <= 4 && c >= 7)
        }
        val gambling = Evaluator { cards ->
            val longWithTopHonours = cards.size >= 7 &&
                cards[0] == Rank.A && cards[1] == Rank.K && cards[2] == Rank.Q
            val shortWithNoControls = cards.size <= 4 && (cards.isEmpty() || cards[0] < Rank.K)

            if (longWithTopHonours || shortWithNoControls) 1 else 0
        }

        val smartStack = SmartStack(gamblingShape, gambling, listOf(4)) // all satisfy
        smartStack.prepare(
            HoldingBySuit.parse("AK K52 98765 962")
        )
        val occurrences = IntArray(COMBINATIONS)
        repeat(100_000) { // test failure should be like sth like a 10-sigma event
            val north = smartStack().encoded
            val lengths = SUITS.map { suit -> (0xffffL shl (16 * suit.encoded)).and(north).countOneBits() }
            val (s, h, d, _) = lengths
            assertEquals(13, lengths.sum())
            occurrences[flatten(s, h, d)]++
        }

        val sorted = occurrences
            .mapIndexed { index, i ->
                Pair(toLengths(index), i)
            }
            .sortedByDescending {
                it.second
            }
        sorted.subList(0, 10).forEach { println(it) }
        assertEquals(Lengths(3, 2, 1, 7), sorted[0].first)
        assertEquals(
            setOf(
                Lengths(2, 2, 2, 7),
                Lengths(2, 3, 1, 7)
            ),
            setOf(sorted[1].first, sorted[2].first)
        )
        assertEquals(Lengths(3, 1, 2, 7), sorted[3].first)
        assertEquals(
            setOf(
                Lengths(1, 3, 2, 7),
                Lengths(3, 3, 0, 7)
            ),
            setOf(sorted[4].first, sorted[5].first)
        )

        val cumSum = cumSumField.get(smartStack) as LongArray
        assertEquals(77, cumSum.size)
        assertEquals(8810199L, cumSum.last())
    }

    @Test
    fun testEverything() {
        val shape = logTimeMs({ "The everything shape took $it" }) { Shape("xxxx") }
        val smartStack = SmartStack(shape, Evaluator.hcp, 0..37)
        // the prepare step took 3 minutes in Python redeal
        logTimeMs({ "Prepare and first hand took $it" }) { smartStack() }
        logTimeMs({ "Afterwards, a hand took $it" }) { smartStack() }
        val cumSum = cumSumField.get(smartStack) as LongArray

        fun hcpRange(l: Int) = when (l) {
            0 -> 0..0
            1 -> 0..4
            2 -> 0..7
            3 -> 0..9
            in 4..9 -> 0..10
            10 -> 1..10
            11 -> 3..10
            12 -> 6..10
            13 -> 10..10
            else -> throw IllegalArgumentException()
        }.let {
            it.last - it.first + 1
        }

        var sum = 0
        iterateShapes { s, h, d, c ->
            sum += listOf(s, h, d, c).map { hcpRange(it) }.reduce(Int::times)
        }
        assertEquals(1_379_488, sum)
        assertEquals(sum, cumSum.size)
        assertEquals(635_013_559_600, cumSum.last()) // 52C13
    }

    @Test
    fun testNothing() {
        val smartStack = SmartStack(Shape("5332"), Evaluator.hcp, 31..37)

        assertThrows<IllegalArgumentException> {
            smartStack.prepare(HoldingBySuit.parse("AKQJT98765432 - - -"))
        }.printStackTrace()
    }
}
