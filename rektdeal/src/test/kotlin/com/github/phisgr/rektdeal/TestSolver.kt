package com.github.phisgr.rektdeal

import com.github.phisgr.dds.*
import java.util.*
import kotlin.test.Test
import kotlin.test.assertEquals


class TestSolver {

    @Test
    fun testSingleThreadSolving() {
        val deal = Deal()

        repeat(3) { i ->
            deal.fromPbn(deals[i])

            val tricks = deal.ddTricks(trumps[i], declarer = firsts[i].next().next().next())
            assertEquals(13 - cardsScores[i][0], tricks)
        }

    }

    @Test
    fun testMultiThreadSolving() {
        multiThread(
            count = 3,
            // accept all deals, then mess with the deal.cards
            action = { index, deal ->
                val i = index - 1

                deal.fromPbn(deals[i])

                val expected = (0 until cardsSoln3[i]).associate {
                    Card(suit = cardsSuits[i][it], rank = cardsRanks[i][it]) to cardsScores[i][it]
                }
                val solved = deal.ddAllTricks(trumps[i], leader = firsts[i])
                println(solved)
                assertEquals(expected, solved)
            }
        )
    }

    private fun Deal.fromPbn(pbn: String) {
        val openingLead = Direction.fromChar(pbn.first())

        val cards = pbn.drop(2).split(' ')
        DIRECTIONS.toMutableList()
            .also {
                Collections.rotate(it, -openingLead.encoded)
            }
            .forEachIndexed { j, direction ->
                val directionOffset = direction.encoded * 13
                var cardIndex = directionOffset
                cards[j].split('.').forEachIndexed { suit, holding ->
                    holding.forEach { rank ->
                        this.cards[cardIndex] = Card(suit.toSuit(), Rank.fromChar(rank)).encoded
                        cardIndex++
                    }
                }
            }
    }
}
