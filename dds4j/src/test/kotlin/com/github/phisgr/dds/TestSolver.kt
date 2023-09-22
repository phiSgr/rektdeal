package com.github.phisgr.dds

import com.github.phisgr.logTimeMs
import java.lang.foreign.Arena
import kotlin.test.Test
import kotlin.test.assertEquals


private typealias Solver<T> = (T, target: Int, solutions: Int, mode: Int, FutureTricks, thrId: Int) -> Unit

class TestSolver {
    private inline fun <T> solveAndCompare(
        solver: Solver<T>,
        deal: T,
        futureTricks: FutureTricks,
        boardNum: Int,
        solutions: Int,
        cards: IntArray,
    ) {
        logTimeMs({ "Solving board $boardNum took $it." }) {
            solver(deal, -1, solutions, 0, futureTricks, 0)
        }

        println(futureTricks)
        assertEquals(cards[boardNum], futureTricks.cards)
        compareTricks(futureTricks, handNum = boardNum)
    }

    @Test
    fun testSolveBoard() {
        println("Solving boards 0,0,1,1,2,2")
        Arena.ofConfined().use { arena ->
            val deal = Deal(arena)
            val futureTricks = FutureTricks(arena)

            repeat(3) { i ->

                deal.trump = trumps[i]
                deal.first = firsts[i]
                deal.currentTrickRank.clear()
                deal.currentTrickSuit.clear()
                deal.remainCards.fromPbn(i)

                println(deal)
                listOf(2 to cardsSoln2, 3 to cardsSoln3).forEach { (solutions, cards) ->
                    // solving the same board again takes very little time
                    solveAndCompare(
                        ::solveBoard,
                        deal,
                        futureTricks,
                        boardNum = i,
                        solutions = solutions,
                        cards = cards
                    )
                }
            }

        }
    }

    @Test
    fun testSolveBoardPbn() {
        println("Solving boards 0,1,2,0,1,2")
        listOf(2 to cardsSoln2, 3 to cardsSoln3).forEach { (solutions, cards) ->
            repeat(3) { i ->
                Arena.ofConfined().use { arena -> // does not reuse memory this time
                    val deal = DealPBN(arena)
                    val futureTricks = FutureTricks(arena)
                    deal.trump = trumps[i]
                    deal.first = firsts[i]
                    deal.currentTrickRank.clear()
                    deal.currentTrickSuit.clear()

                    deal.remainCards = deals[i]

                    println(deal)

                    solveAndCompare(
                        ::solveBoardPBN,
                        deal,
                        futureTricks,
                        boardNum = i,
                        solutions = solutions,
                        cards = cards
                    )
                }
            }
        }
    }
}
