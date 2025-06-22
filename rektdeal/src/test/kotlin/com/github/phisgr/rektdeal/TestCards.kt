package com.github.phisgr.rektdeal

import com.github.phisgr.dds.DIRECTIONS
import com.github.phisgr.dds.Deal
import com.github.phisgr.dds.SUITS
import java.lang.foreign.Arena
import kotlin.math.max
import kotlin.reflect.KProperty1
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TestCards {
    @Test
    fun testGetHandSorting() {
        repeat(4) {
            val cards = newDeck()
            cards.shuffle()

            Arena.ofConfined().use { arena ->
                val deal = Deal(arena)
                cards.setCards(deal.remainCards)

                println(deal.remainCards)
                println(cards.west)
                println(cards)

                // S H D C, descending
                val westRanksInDealtCards = cards
                    .toString()
                    .split(' ')[3]
                    .filter { it.isLetterOrDigit() }

                val westRankInCards = deal.remainCards
                    .toString()
                    .split("W: ")[1]
                    .dropLast(1)
                    .filter { it != '.' }

                assertEquals(westRankInCards, westRanksInDealtCards)
            }
            println()
        }
    }

    @Test
    fun testInit() {
        val cards = newDeck()
        repeat(16) {
            cards.shuffle()

            DIRECTIONS.forEach { direction ->
                val hand = cards.getHand(direction)
                val sizes = SUITS.map { suit ->
                    hand[suit].size
                }
                println(sizes.joinToString("-"))
                assertTrue(sizes.all { it >= 0 })
                assertEquals(13, sizes.sum())
            }
            println(cards)
        }
    }

    fun Boolean.toInt() = if (this) 1 else 0

    /**
     * [Redeal reference implementation](
     * https://github.com/anntzer/redeal/blob/e2e81a477fd31ae548a340b5f0f380594d3d0ad6/redeal/redeal.py#L481)
     */
    private fun losersRedeal(suitHolding: SuitHolding): Double {
        val cardSet = suitHolding.map { c -> c.encoded }.toSet()

        val (a, k, q, j, t) = (14 downTo 10).toList()

        var losers = 0.0
        if (suitHolding.size == 0) return losers
        if (a !in cardSet) losers += 1
        if (suitHolding.size >= 2 && k !in cardSet) losers += 1
        if (suitHolding.size >= 3) {
            if (q !in cardSet) {
                losers += 1
            } else if (losers == 2.0 && j !in cardSet && t !in cardSet) {
                losers += 0.5
            }
        }

//        losers += (suitHolding.size >= 1 && a !in cardSet).toInt()
//        losers += (suitHolding.size >= 2 && k !in cardSet).toInt()
//        losers += (suitHolding.size >= 3).toInt() *
//            (if (q !in cardSet) {
//                1.0
//            } else {
//                (losers == 2.0 && j !in cardSet && t !in cardSet).toInt() / 2.0
//            })

        return losers
    }

    /**
     * [Redeal reference implementation](
     * https://github.com/anntzer/redeal/blob/96c18fb2963b489d733a37237c5b72ac80689ad6/redeal/redeal.py#L532)
     */
    private fun newLtcRedeal(suitHolding: SuitHolding): Double {
        val cardSet = suitHolding.map { c -> c.encoded }.toSet()

        val (a, k, q) = (14 downTo 12).toList()

        return 1.5 * (suitHolding.size >= 1 && a !in cardSet).toInt() +
            1.0 * (suitHolding.size >= 2 && k !in cardSet).toInt() +
            0.5 * (suitHolding.size >= 3 && q !in cardSet).toInt()
    }


    /**
     * [Redeal reference implementation](
     * https://github.com/anntzer/redeal/blob/96c18fb2963b489d733a37237c5b72ac80689ad6/redeal/redeal.py#L544)
     */
    private fun playingTricksRedeal(suitHolding: SuitHolding): Double {
        val lenPt = max(suitHolding.size - 3, 0).toDouble()
        val cardSet = suitHolding.map { c -> c.encoded }.toSet()

        val (a, k, q, j, t) = (14 downTo 10).toList()

        fun containsAll(vararg rank: Int) = cardSet.containsAll(rank.toList())

        return when {
            containsAll(a, k, q) -> 3 + lenPt
            containsAll(a, k, j) || containsAll(a, q, j) -> 2.5 + lenPt
            containsAll(a, k) || containsAll(a, q, t) || containsAll(k, q, j) -> 2 + lenPt
            containsAll(a, q) || containsAll(k, j, t) -> 1.5 + lenPt
            containsAll(a, j) || containsAll(k, q) && suitHolding.size >= 3 -> 1.5 + lenPt
            containsAll(a) || containsAll(k, q) || containsAll(k, j) -> 1 + lenPt
            containsAll(q, j) && suitHolding.size >= 3 -> 1 + lenPt
            containsAll(k) && suitHolding.size >= 2 ||
                (containsAll(q) || containsAll(j, t)) && suitHolding.size >= 3 ->
                0.5 + lenPt

            else -> lenPt
        }
    }

    private fun <T> testRandom(
        referenceImpl: (SuitHolding) -> T,
        value: KProperty1<SuitHolding, T>,
    ) {
        val cards = newDeck()
        repeat(10_000) {
            cards.shuffle()
            DIRECTIONS.forEach { dir ->
                val hand = cards.getHand(dir)
                SUITS.forEach { suit ->
                    val holding = hand[suit]
                    try {
                        assertEquals(referenceImpl(holding), value.get(holding))
                    } catch (e: Throwable) {
                        println(holding)
                        throw e
                    }
                }
            }
        }
    }

    private fun <T> testHonours(
        referenceImpl: (SuitHolding) -> T,
        value: KProperty1<SuitHolding, T>,
    ) {

        val cards = newDeck()
        // using north.spades because of no extra offset/bits
        val spades = cards.north.spades

        fun check() {
            spades.reset()
            println("$spades ${value(spades)}")
            assertEquals(referenceImpl(spades), value(spades))
        }

        repeat(32) { honourBits ->
            var size = 0
            (14 downTo 10).forEach { card ->
                if (honourBits and 1.shl(card - 10) != 0) {
                    cards.cards[size] = card.toByte()
                    size++
                }
                spades.end = size
            }
            check()
            repeat(4) { extraLength ->
                cards.cards[size] = (5 - extraLength).toByte()
                size++
                spades.end = size
                check()
            }
        }
    }

    @Test
    fun testPlayingTricks() {
        testRandom(::playingTricksRedeal, SuitHolding::playingTricks)
        testHonours(::playingTricksRedeal, SuitHolding::playingTricks)
    }

    @Test
    fun testLosers() {
        testRandom(::losersRedeal, SuitHolding::losers)
        testHonours(::losersRedeal, SuitHolding::losers)
    }

    @Test
    fun testNewLtc() {
        testRandom(::newLtcRedeal, SuitHolding::newLtc)
        testHonours(::newLtcRedeal, SuitHolding::newLtc)
    }
}
