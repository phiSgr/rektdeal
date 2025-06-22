package com.github.phisgr.rektdeal

import java.util.concurrent.ThreadLocalRandom
import kotlin.test.Test
import kotlin.test.assertEquals

class TestHoldingBySuit {
    @Test
    fun testToStringParse() {
        val random = ThreadLocalRandom.current()
        repeat(100) {
            val bits = random.nextLong() and HoldingBySuit.WHOLE_DECK.encoded
            try {
                val s = HoldingBySuit(bits).toString()
                println(s)
                assertEquals(bits, HoldingBySuit.parse(s).encoded)
            } catch (e: Throwable) {
                println(bits.toString(2))
                throw e
            }
        }
    }
}
