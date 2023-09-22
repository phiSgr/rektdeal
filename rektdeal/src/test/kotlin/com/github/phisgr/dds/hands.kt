package com.github.phisgr.dds

import java.util.*
import kotlin.test.assertEquals

// See dds/examples/hands.cpp

val trumps = listOf(S, N, S)
val firsts = listOf(NORTH, EAST, SOUTH)
val dealer = listOf(NORTH, EAST, NORTH)
val vul = listOf(Vulnerability.NONE, Vulnerability.NS, Vulnerability.NONE)
val deals = listOf(
    "N:QJ6.K652.J85.T98 873.J97.AT764.Q4 K5.T83.KQ9.A7652 AT942.AQ4.32.KJ3",
    "E:QJT5432.T.6.QJ82 .J97543.K7532.94 87.A62.QJT4.AT75 AK96.KQ8.A98.K63",
    "N:73.QJT.AQ54.T752 QT6.876.KJ9.AQ84 5.A95432.7632.K6 AKJ9842.K.T8.J93"
)
private val playedCardCounts = listOf(45, 52, 12)
val cardStrings = listOf(
    "CTC4CACJH8H4HKH9D5DAD9D2S7S5S2SQD8D4DQD3H3HAH6H7C3C8CQC2S3SKSAS6HQH5HJHTCKC9D6C5S4SJS8C6DJ",
    "SQD2S8SAHKHTH3H2HQS2H4H6H8D6HJHAS7SKS4C4D8C2DKD4H9C5S6S3H7C7C3S5H5CTD9STD3DQDAC8S9SJC9DTCQD5CAC6DJCKCJD7",
    "HAHKHQH7D7D8DAD9C5CAC6C3",
)
val cardsPlayed = cardStrings.mapIndexed { i, cards ->
    cards.chunked(2).map { card ->
        Suit.fromChar(card[0]) to Rank.fromChar(card[1])
    }.also {
        check(it.size == playedCardCounts[i])
    }
}
val trickCounts = listOf(
    listOf(
        8,
        8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8,
        8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8,
        8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8
    ),
    listOf(
        9,
        10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10,
        10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10,
        10, 10, 10, 10, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9
    ),
    listOf(
        10,
        10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10
    ),
)

fun Cards.fromPbn(boardNum: Int) {
    val pbn = deals[boardNum]
    val openingLead = Direction.fromChar(pbn.first())

    val cards = pbn.drop(2).split(' ')
    DIRECTIONS.toMutableList()
        .also {
            Collections.rotate(it, -openingLead.encoded)
        }
        .forEachIndexed { j, direction ->
            val hand = cards[j].split('.')
            SUITS.forEach { suit ->
                this[direction, suit] = Holding(hand[suit.encoded])
            }
        }
}

val cardsSoln2 = intArrayOf(6, 3, 4)
val cardsSoln3 = intArrayOf(9, 7, 8)
val cardsSuits = arrayOf(
    intArrayOf(2, 2, 2, 3, 0, 0, 1, 1, 1, 0, 0, 0, 0),
    intArrayOf(3, 3, 3, 1, 2, 0, 0, 0, 0, 0, 0, 0, 0),
    intArrayOf(1, 2, 2, 0, 1, 1, 3, 3, 0, 0, 0, 0, 0)
)
val cardsRanks = arrayOf(
    intArrayOf(5, 8, 11, 10, 6, 12, 2, 6, 13, 0, 0, 0, 0),
    intArrayOf(2, 8, 12, 10, 6, 12, 5, 0, 0, 0, 0, 0, 0),
    intArrayOf(14, 3, 7, 5, 5, 9, 6, 13, 0, 0, 0, 0, 0)
)
val cardsScores = arrayOf(
    intArrayOf(5, 5, 5, 5, 5, 5, 4, 4, 4, 0, 0, 0, 0),
    intArrayOf(4, 4, 4, 3, 3, 3, 2, 0, 0, 0, 0, 0, 0),
    intArrayOf(3, 3, 3, 3, 2, 2, 1, 1, 0, 0, 0, 0, 0)
);
val cardsEquals = arrayOf(
    intArrayOf(0, 0, 0, 768, 0, 2048, 0, 32, 0, 0, 0, 0, 0),
    intArrayOf(0, 0, 2048, 0, 0, 3072, 28, 0, 0, 0, 0, 0, 0),
    intArrayOf(0, 4, 64, 0, 28, 0, 0, 0, 0, 0, 0, 0, 0)
)

fun compareTricks(futureTricks: FutureTricks, handNum: Int) {
    (0..<futureTricks.cards).forEach {
        assertEquals(cardsSuits[handNum][it].toSuit(), futureTricks.suit[it])
        assertEquals(cardsRanks[handNum][it].toRank(), futureTricks.rank[it])
        assertEquals(cardsEquals[handNum][it].toHolding(), futureTricks.equals[it])
        assertEquals(cardsScores[handNum][it], futureTricks.score[it])
    }
}


val ddTableEquals = arrayOf(
    arrayOf(5, 8, 5, 8, 6, 6, 6, 6, 5, 7, 5, 7, 7, 5, 7, 5, 6, 6, 6, 6),
    arrayOf(4, 9, 4, 9, 10, 2, 10, 2, 8, 3, 8, 3, 6, 7, 6, 7, 9, 3, 9, 3),
    arrayOf(3, 10, 3, 10, 9, 4, 9, 4, 8, 4, 8, 4, 3, 9, 3, 9, 4, 8, 4, 8)
)

fun compareTable(ddTable: DdTable, handNum: Int) {
    val expected = ddTableEquals[handNum]
    var index = 0
    STRAINS.forEach { strain ->
        DIRECTIONS.forEach { direction ->
            assertEquals(expected[index], ddTable[strain, direction])
            index++
        }
    }
}
