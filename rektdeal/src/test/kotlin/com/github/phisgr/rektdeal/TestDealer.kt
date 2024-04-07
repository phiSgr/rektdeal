package com.github.phisgr.rektdeal

import org.junit.jupiter.api.assertThrows
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue


class TestDealer {
    private fun IntArray.accumulateClubLength(l: Int) {
        val index = when (l) {
            0 -> 0
            1 -> 1
            4 -> 2
            5 -> 3
            else -> throw IllegalArgumentException()
        }
        this[index]++
    }

    /**
     * https://github.com/anntzer/redeal/blob/e2e81a477fd31ae548a340b5f0f380594d3d0ad6/examples/deal1.py
     */
    @Test
    fun testRoman() {
        val dealer = Dealer()
        var i = 0
        val n = 10000

        val lengths = IntArray(4)

        repeat(n) {
            val deal = dealer { deal ->
                i++
                deal.north.spades.size == 4 && deal.north.hearts.size == 4 &&
                    deal.north.diamonds.size !in (2..3) &&
                    deal.north.hcp in (11..15)
            }
            println(deal.toString())
            lengths.accumulateClubLength(deal.north.clubs.size)
        }
        println("Took $i tries to generate $n deals.")
        println(lengths.toList())
    }


    /**
     * https://github.com/anntzer/redeal/blob/e2e81a477fd31ae548a340b5f0f380594d3d0ad6/examples/deal1_stack.py
     */
    @Test
    fun testRomanSmart() {
        val roman = Shape("44(41)") + Shape("44(50)")
        val dealer = Dealer(N = SmartStack(shape = roman, Evaluator.hcp, 11..15))
        val n = 1_000_000
        val lengths = IntArray(4)
        repeat(n) {
            val deal = dealer()
            lengths.accumulateClubLength(deal.north.clubs.size)
        }

        run {
            val p = 229813740.0 / 3689996472.0
            val expectedValue = n * p
            val sd = sdBinomial(n, p = p)
            println("S.D. of count for (05) distribution $sd")

            assertTrue(abs((lengths[0] - expectedValue) / sd) < 5)
            assertTrue(abs((lengths[3] - expectedValue) / sd) < 5)
        }

        run {
            val p = 1615184496.0 / 3689996472.0
            val expectedValue = n * p
            val sd = sdBinomial(n, p = p)
            println("S.D. of count for (41) distribution $sd")

            // at most 0.57% off
            assertTrue(abs((lengths[1] - expectedValue) / sd) < 5)
            assertTrue(abs((lengths[2] - expectedValue) / sd) < 5)
        }

        println(lengths.toList())
    }


    /**
     * https://github.com/anntzer/redeal/blob/e2e81a477fd31ae548a340b5f0f380594d3d0ad6/examples/deal2.py
     */
    @Test
    fun testDeal2() {
        val dealer = Dealer(S = "Q86432 T2 932 83")
        var i = 0
        val n = 10000
        repeat(n) {
            val deal = dealer { deal ->
                i++
                assertEquals(deal.south.shape, listOf(6, 2, 3, 2))
                deal.east.hcp > 18 && (deal.east.hcp > 22 || deal.east.losers < 2)
            }
            println(deal.toString())
        }
        println("Took $i tries to generate $n deals.")
    }

    @Test
    fun testOverlappingCards() {
        assertThrows<IllegalArgumentException>("Overlapping cards in pre-dealt hands.") {
            Dealer(
                S = "Q86432 T2 932 83",
                N = "2 3456789JQK A K",
            )
        }
    }

}
