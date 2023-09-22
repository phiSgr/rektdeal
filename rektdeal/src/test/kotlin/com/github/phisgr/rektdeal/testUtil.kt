package com.github.phisgr.rektdeal

import java.lang.reflect.Field
import kotlin.math.sqrt

/**
 * lighter weight than setting up a [Dealer]
 */
fun Deal.shuffle() {
    cards.shuffle()
    reset(0)
}

fun newDeck(): Deal {
    val cards = Deal(0)
    repeat(SIZE) {
        cards.cards[it] = Card(suit = it / 13, rank = it % 13 + 2).encoded
    }
    return cards
}

fun sdBinomial(n: Int, np: Int): Double {
    val variance: Double = np * ((n - np).toDouble() / n)
    return sqrt(variance)
}

fun sdBinomial(n: Int, p: Double): Double {
    val variance: Double = n * p * (1 - p)
    return sqrt(variance)
}

val cumSumField: Field = SmartStack::class.java.getDeclaredField("cumSum").also {
    it.isAccessible = true
}

val shortTest = System.getenv("SHORT_TEST")?.toBoolean() ?: false
