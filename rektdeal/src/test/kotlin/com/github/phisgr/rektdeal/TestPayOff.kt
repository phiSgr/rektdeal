package com.github.phisgr.rektdeal

import kotlin.test.Test
import kotlin.test.assertEquals

class TestPayOff {
    @Test
    fun testImp() {
        val scores = listOf(0, 10, 20, 30, 40, 50, 670, 740, 750, 3990, 4000, 4010).map {
            val res = PayOff.calculateImp(it)
            assertEquals(-res, PayOff.calculateImp(-it))
            res
        }
        assertEquals(
            listOf(0, 0, 1, 1, 1, 2, 12, 12, 13, 23, 24, 24),
            scores
        )
    }
}
