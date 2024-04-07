package com.github.phisgr.rektdeal

import com.github.phisgr.dds.N
import com.github.phisgr.dds.Rank
import com.github.phisgr.dds.SOUTH
import com.github.phisgr.dds.threadCount
import org.junit.jupiter.api.assertThrows
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicIntegerArray
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertTrue


class TestMultiThreaded {

    /**
     * https://github.com/anntzer/redeal/blob/e2e81a477fd31ae548a340b5f0f380594d3d0ad6/examples/deal3.py
     */
    @Test
    fun testDeal3() {
        val i = AtomicInteger()
        val n = if (shortTest) 10 else 10000

        val bal1S = Shape("5(332)")

        multiThread(
            count = n,
            dealer = {
                Dealer(S = "764 J4 J753 AQJ2")
            },
            accept = { deal ->
                i.getAndIncrement()

                deal.west.hcp in 16..19 && deal.west.shape in bal1S &&
                    deal.east.hcp in 6..11 && deal.east.losers >= 3 &&
                    deal.east.hearts.size <= 4 && deal.east.spades.size <= 2
            },
            action = { _, deal ->
                println(deal.toString())
            }
        )
        println("Took $i tries to generate $n deals.")
    }

    /**
     * See `examples/gazzilli_weak_response.ipynb`
     */
    @Test
    fun testGazzilliWeakResponse() {
        val i = AtomicInteger()
        val n = if (shortTest) 10 else 10000
        val bal1S = Shape("5(332)")

        multiThread(
            count = n,
            accept = { deal ->
                i.incrementAndGet()

                deal.north.hcp in 18..19 && bal1S(deal.north) && (           // north strong bal
                    deal.south.spades.size == 3 && deal.south.hcp in 3..5 || // south very weak with support
                        deal.south.spades.size < 3 && deal.south.hcp in 5..7 // south weak no support
                    )
            },
            action = { _, deal ->
                println(deal.toString())
            }
        )

        println("Took $i tries to generate $n deals.")
    }

    /**
     * See [pavlicek_8z15.py](
     * https://github.com/anntzer/redeal/blob/e2e81a477fd31ae548a340b5f0f380594d3d0ad6/examples/pavlicek_8z15.py)
     */
    @Test
    fun test8z15() {
        val i = AtomicInteger()
        val n = 1000_000

        val counts = multiThread(
            count = n,
            dealer = { Dealer(N = "J763 J874 74 AQ5") },
            state = { IntArray(3) },
            accept = { deal ->
                i.incrementAndGet()

                deal.south.hcp in 15..17 && deal.south.freakness < 3 &&          // strong NT
                    (deal.south.spades.size < 5 && deal.south.hearts.size < 5 || // no 5cM
                        deal.south.all { it.size > 2 || it[0] >= Rank.J }) &&    // or 5cM with Jx or better doubleton

                    // Copying the logic from the Python file
                    //
                    // The description in the website is different:
                    // > East was constrained not to have any appropriate action (bid or double) over 1 NT.
                    // > No constraints were placed on West (no turn yet).
                    listOf(deal.west, deal.east).all { opp ->
                        opp.hcp < 15 &&
                            (opp.freakness < 6 && opp.l1 < 6 || opp.playingTricks < 6)
                    }
            },
            action = { _, deal, state ->
                state[deal.south.hcp - 15]++
            }
        )

        val table = (15..17).associateWith { hcp ->
            counts.sumOf { it[hcp - 15] }
        }

        println(table)
        println("Took $i tries to generate $n deals.")

        // Took half an hour to get these numbers from the Python script
        // Single-threaded, this test takes about 9 seconds
        // with 12 threads, about 2 seconds - when the work per deal is cheap, the shared counter memory access cost becomes significant
        val diff = mapOf(15 to 411502, 16 to 334853, 17 to 253645).mapValues { (hcp, count) ->
            (table[hcp]!! - count) / sdBinomial(n = n, np = count)
        }
        println(diff)
        diff.forEach { (_, d) -> assertTrue(abs(d) < 5) } // 5 sigma failure, at most 0.25% away
    }

    class Bleh : Throwable()

    @Test
    fun testExceptionHandling() {
        val count = AtomicInteger()
        assertThrows<Bleh> {
            multiThread(
                count = 1000,
                action = { index, deal ->
                    println(deal)
                    if (index == 10) {
                        throw Bleh()
                    }
                    count.getAndIncrement()
                }
            )
        }
        assertContains(9..500, count.get())
    }


    @Test
    fun testMtSolving() {
        val trickCounts = AtomicIntegerArray(14)
        val n = if (shortTest) 10 else 100
        multiThread(
            count = n,
            accept = { deal ->
                deal.south.hcp in 12..14 &&
                    Shape.balanced(deal.south)
            },
            action = { _, deal ->
                println(deal)
                trickCounts.getAndIncrement(
                    deal.ddTricks(strain = N, declarer = SOUTH)
                )
            }
        )


        println(trickCounts)
        print("If you open weak NT, the expected value of tricks is approximately ")
        println((0..13).sumOf { it * trickCounts[it] } / n.toDouble())
        print("The expected value of score when contract is 1N (non-vul) is ")
        println((0..13).sumOf {
            Contract("1N").score(it, vulnerable = false) * trickCounts[it]
        } / n.toDouble())
    }

}
