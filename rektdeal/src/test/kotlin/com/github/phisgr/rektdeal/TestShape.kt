package com.github.phisgr.rektdeal

import com.github.phisgr.logTimeMs
import org.junit.jupiter.api.assertThrows
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue


class TestShape {
    @Test
    fun testGambling() {
        val cond: ShapeFun<Boolean> = { s, h, d, c ->
            s <= 3 && h <= 3 && (d >= 7 && c <= 4 || d <= 4 && c >= 7)
        }
        val shape = logTimeMs { Shape(cond) }
        println(shape)

        assertTrue(Lengths(3, 3, 7, 0) in shape)
        assertTrue(Lengths(4, 1, 7, 1) !in shape)
        assertTrue(Lengths(1, 0, 5, 7) !in shape)
        assertTrue(Lengths(1, 3, 3, 6) !in shape)

        logTimeMs {
            (0..13).forEach { s ->
                (0..(13 - s)).forEach { h ->
                    (0..(13 - s - h)).forEach { d ->
                        val lengths = Lengths(s, h, d, 13 - s - h - d)
                        assertEquals(
                            cond(s, h, d, lengths.c),
                            lengths in shape
                        )
                    }
                }
            }
        }
    }

    @Test
    fun test5332() {
        logTimeMs { Shape("(5332)") }.let { shape ->
            println(shape)
            assertEquals(12, shape.count)
            assertTrue(Lengths(3, 3, 2, 5) in shape)
            assertTrue(Lengths(3, 5, 2, 3) in shape)
        }

        logTimeMs { Shape("5(332)") }.let { shape ->
            println(shape)
            assertEquals(Shape("5332") + Shape("5323") + Shape("5233"), shape)
            assertEquals(3, shape.count)
            assertTrue(Lengths(3, 3, 2, 5) !in shape)
            assertTrue(Lengths(5, 2, 3, 3) in shape)
            assertTrue(Lengths(5, 3, 3, 2) in shape)
        }

        logTimeMs { Shape("(53)(32)") + Shape("(52)33") }.let { shape ->
            println(shape)
            assertEquals(6, shape.count)
            assertTrue(Lengths(3, 3, 2, 5) !in shape)
            assertTrue(Lengths(2, 5, 3, 3) in shape)
            assertTrue(Lengths(5, 3, 3, 2) in shape)
        }
    }

    @Test
    fun testVariousShapes() {
        logTimeMs { Shape("(31)(54)") }.let { shape ->
            println(shape)

            assertEquals(4, shape.count)
            assertTrue(Lengths(3, 1, 4, 5) in shape)
            assertTrue(Lengths(2, 2, 5, 4) !in shape)
            assertTrue(Lengths(1, 3, 5, 4) in shape)
        }
        logTimeMs { Shape("(6x)xx") }.let { shape ->
            println(shape)
            // 7 cards remain, 66xx
            // 9C2 * 2       - 2
            assertEquals(70, shape.count)

            assertTrue(Lengths(6, 7, 0, 0) in shape)
            assertTrue(Lengths(6, 6, 1, 0) in shape)
            assertTrue(Lengths(1, 3, 5, 4) !in shape)
        }
        logTimeMs { Shape("(5x)xx") }.let { shape ->
            println(shape)
            // 8 cards remain, 55xx
            // 10C2 * 2      - 4
            assertEquals(86, shape.count)

            assertTrue(Lengths(5, 5, 0, 3) in shape)
            assertTrue(Lengths(5, 0, 8, 0) in shape)
            assertTrue(Lengths(2, 3, 5, 3) !in shape)
        }
    }

    @Test
    fun testEverything() {
        // This is an absurd corner-case.
        // Equivalent to "xxxx"
        // but the permutation code does not handle repetitions
        // so repeated 24 times
        // Python redeal took 5 seconds
        val everything = logTimeMs { Shape("(xxxx)") }.also { shape ->
            println(shape)
            assertEquals(560, shape.count)
        }

        logTimeMs {
            Shape { _, _, _, _ -> true }
        }.let { shape ->
            println(shape)
            assertEquals(560, shape.count)
            assertEquals(everything, shape)
        }
    }

    @Test
    fun testIllegal() {
        assertThrows<IllegalArgumentException> { Shape("5(332") }
        assertThrows<IllegalArgumentException> { Shape("5678") }
        assertThrows<IllegalArgumentException> { Shape("555x") }
        assertThrows<IllegalArgumentException> { Shape("33331") }
    }

    @Test
    fun testMinus() {
        val semiBalNo5cM = Shape.semiBalanced - Shape { s, h, _, _ -> s >= 5 || h >= 5 }
        assertEquals(34, semiBalNo5cM.count)
        assertContentEquals(intArrayOf(4, 4, 6, 6), semiBalNo5cM.maxLengths)
        assertContentEquals(intArrayOf(2, 2, 2, 2), semiBalNo5cM.minLengths)
        assertEquals(
            Shape { s, h, d, c ->
                intArrayOf(s, h, d, c).all { it in 2..6 } && s <= 4 && h <= 4
            },
            semiBalNo5cM
        )
        println(semiBalNo5cM)
    }

    @Test
    fun testIntersect() {
        val balWith5cM = Shape.balanced.intersect(Shape { s, h, _, _ -> s >= 5 || h >= 5 })
        assertEquals(
            Shape { s, h, d, _ ->
                Shape.balanced.contains(s, h, d) && (s == 5 || h == 5)
            },
            balWith5cM
        )
        assertEquals(6, balWith5cM.count)
        assertContentEquals(intArrayOf(5, 5, 3, 3), balWith5cM.maxLengths)
        assertContentEquals(intArrayOf(2, 2, 2, 2), balWith5cM.minLengths)
        println(balWith5cM)
    }
}
