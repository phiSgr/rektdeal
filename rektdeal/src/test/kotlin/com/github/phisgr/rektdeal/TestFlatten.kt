package com.github.phisgr.rektdeal

import com.github.phisgr.rektdeal.internal.flatten
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue


// [0, 1, 4, 10, 20, 35, 56, 84, 120, 165, 220, 286, 364, 455]
val c3 = IntArray(14).also { c3 ->
    c3[1] = 1
    (1 until 13).forEach { i ->
        c3[i + 1] = c3[i] * (i + 3) / i
    }
}

// [0, 1, 3, 6, 10, 15, 21, 28, 36, 45, 55, 66, 78, 91]
val c2 = IntArray(14).also { c2 ->
    c2[1] = 1
    (1 until 13).forEach { i ->
        c2[i + 1] = c2[i] * (i + 2) / i
    }
}

/**
 * Opposite of [Lengths.flatten]
 */
@Suppress("LocalVariableName")
fun toLengths(flattened: Int): Lengths {
    val `third - 2` = c3.indexOfLast { it <= flattened }
    val secondSum = flattened - c3[`third - 2`]
    val `second - 1` = c2.indexOfLast { it <= secondSum }
    val first = secondSum - c2[`second - 1`]

    val h = `second - 1` - first
    val d = `third - 2` - `second - 1`

    check(first >= 0)
    check(h >= 0)
    check(d >= 0)

    return Lengths(s = first, h, d, c = 13 - `third - 2`)
}

class TestFlatten {

    @Test
    fun testFlattenShape() {
        println(c3.toList())
        println(c2.toList())
        val set = mutableSetOf<Lengths>()
        repeat(560) { flattened ->
            val lengths = toLengths(flattened)
            val isNew = set.add(lengths)
            assertTrue(isNew)
            assertEquals(
                flattened,
                lengths.flatten()
            )
        }
        assertEquals(560, set.size)
    }

    @Test
    fun testIterationOrder() {
        var i = 0
        iterateShapes { s, h, d, c ->
            assertEquals(i, Lengths(s, h, d, c).flatten())
            i++
        }
        assertEquals(560, i)
    }

}

operator fun Shape.contains(lengths: Lengths): Boolean = contains(listOf(lengths.s, lengths.h, lengths.d, lengths.c))
data class Lengths(val s: Int, val h: Int, val d: Int) {
    val c: Int get() = 13 - s - h - d

    init {
        require(s >= 0)
        require(h >= 0)
        require(d >= 0)
        require(c >= 0)
    }

    constructor(s: Int, h: Int, d: Int, c: Int) : this(s, h, d) {
        require(c == this.c) { "$s $h $d $c" }
    }

    fun flatten(): Int = flatten(s, h, d)

    override fun toString(): String {
        return "Lengths(s=$s, h=$h, d=$d, c=$c)"
    }
}
