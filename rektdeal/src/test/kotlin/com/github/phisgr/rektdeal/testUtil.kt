package com.github.phisgr.rektdeal

import java.lang.reflect.Field
import kotlin.math.abs
import kotlin.math.sqrt
import kotlin.test.assertTrue

/**
 * lighter weight than setting up a [Dealer]
 */
fun Deal.shuffle() {
    cards.shuffle()
    reset(0)
}

fun newDeck(): Deal {
    val cards = Deal()
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

fun sdNegativeBinomial(r: Int, p: Double): Double {
    val variance = r * (1 - p) / p / p
    return sqrt(variance)
}

val cumSumField: Field = PreparedSmartStack::class.java.getDeclaredField("cumSum").also {
    it.isAccessible = true
}

val shortTest: Boolean = System.getenv("SHORT_TEST")?.toBoolean() ?: false

fun assertNotTooDifferent(expected: Double, actual: Double, sd: Double) {
    println("Expected: $expected. Actual: $actual. S.d.: $sd.")
    val d = abs(expected - actual) / sd
    println("d: $d")

    // 5-sigma sounds like a lot of wiggle room
    // but for the N values we're working with here, the std err is tiny
    assertTrue(d < 5)
}
