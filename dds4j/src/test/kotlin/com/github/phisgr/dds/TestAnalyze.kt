package com.github.phisgr.dds

import com.github.phisgr.logTimeMs
import java.lang.foreign.Arena
import java.util.concurrent.CompletableFuture
import kotlin.test.Test
import kotlin.test.assertEquals

class TestAnalyze {

    @Test
    fun testAnalyzeBin() {
        logTimeMs({ "Analyzing took $it." }) {
            List(3) { i ->
                CompletableFuture.runAsync { // parallelism!
                    Arena.ofConfined().use { arena ->
                        val deal = Deal(arena)
                        val trace = PlayTraceBin(arena)

                        deal.trump = trumps[i]
                        deal.first = firsts[i]

                        deal.currentTrickRank.clear()
                        deal.currentTrickSuit.clear()

                        deal.remainCards.fromPbn(i)
                        trace.number = cardsPlayed[i].size
                        repeat(trace.number) { j ->
                            val (suit, rank) = cardsPlayed[i][j]
                            trace.suit[j] = suit
                            trace.rank[j] = rank
                        }

                        val res = SolvedPlay(arena).also {
                            logTimeMs({ "Analyzing board $i took $it." }) {
                                analysePlayBin(deal, trace, it, thrId = i % threadCount)
                            }
                        }
                        println("${Thread.currentThread().name} $res")

                        checkTrace(res, i)
                    }
                }
            }.forEach {
                it.get()
            }
        }
    }

    @Test
    fun testAnalyzePBN() {
        Arena.ofConfined().use { arena ->
            repeat(3) { i ->
                val deal = DealPBN(arena)
                val trace = PlayTracePBN(arena)

                deal.trump = trumps[i]
                deal.first = firsts[i]

                deal.currentTrickRank.clear()
                deal.currentTrickSuit.clear()

                deal.remainCards = deals[i]
                trace.number = cardStrings[i].length / 2
                trace.cards = cardStrings[i]

                val res = SolvedPlay(arena).also {
                    analysePlayPBN(deal, trace, it, thrId = 0)
                }
                println(res)
                checkTrace(res, i)
            }
        }
    }

    private fun checkTrace(res: SolvedPlay, i: Int) {
        assertEquals(
            // res.tricks[0] is the tricks of the deal before any card played
            (cardsPlayed[i].size + 1)
                // nothing new happens in last four cards, i.e. last trick
                .coerceAtMost(49),
            res.number
        )
        assertEquals(
            res.tricks.toString(res.number),
            trickCounts[i].toString()
        )
    }

}
