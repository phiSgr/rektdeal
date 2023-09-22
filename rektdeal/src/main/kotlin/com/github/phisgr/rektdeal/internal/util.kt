@file:OptIn(ExperimentalTypeInference::class)

package com.github.phisgr.rektdeal.internal

import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.util.concurrent.atomic.AtomicInteger
import kotlin.experimental.ExperimentalTypeInference

inline fun <A, reified B> List<A>.mapToArray(f: (A) -> B): Array<B> = Array(size) { f(this[it]) }
inline fun <A> List<A>.mapToIntArray(f: (A) -> Int): IntArray = IntArray(size) { f(this[it]) }

/**
 * Work around for KT-45563
 * `(0..13).sumOf { ... }` not compiled down to a simple loop
 *
 * See [Iterable.sumOf]
 */
@OverloadResolutionByLambdaReturnType
@JvmName("sumOfInt")
inline fun sumOf(start: Int = 0, end: Int, f: (Int) -> Int): Int {
    var sum = 0
    for (i in start until end) {
        sum += f(i)
    }
    return sum
}

@OverloadResolutionByLambdaReturnType
@JvmName("sumOfDouble")
inline fun sumOf(start: Int = 0, end: Int, f: (Int) -> Double): Double {
    var sum = 0.0
    for (i in start until end) {
        sum += f(i)
    }
    return sum
}

fun List<Int>.permutations(): Sequence<List<Int>> = sequence {
    if (isEmpty()) yield(emptyList())
    else if (size == 1) yield(this@permutations)
    else {
        // reusing the sub-lists isn't great for readability
        val copy = ArrayList(subList(1, size))
        forEachIndexed { index, i ->
            val res = ArrayList<Int>(size)
            res.add(i)
            repeat(size - 1) { res.add(0) }

            copy.permutations().forEach {
                repeat(size - 1) { i -> res[i + 1] = it[i] }
                yield(res)
            }
            if (index < lastIndex) {
                copy[index] = this@permutations[index]
            }
        }
    }
}

fun List<List<Int>>.nestingPermutations(): Sequence<List<Int>> = sequence {
    if (isEmpty()) yield(emptyList())
    else first().permutations().forEach { firstPerm ->
        subList(1, size).nestingPermutations().forEach { restPerm ->
            yield(firstPerm + restPerm)
        }
    }
}

fun List<Int>.substituteWildCard(remaining: Int, wildcardCount: Int): Sequence<List<Int>> = sequence {
    if (wildcardCount == 0) {
        yield(this@substituteWildCard)
    } else {
        val index = indexOf(-1)
        if (wildcardCount == 1) {
            yield(this@substituteWildCard.toMutableList().also {
                it[index] = remaining
            })
        } else {
            val prefix = subList(0, index)
            for (i in 0..remaining) {
                subList(index + 1, size).substituteWildCard(
                    remaining = remaining - i,
                    wildcardCount = wildcardCount - 1
                ).forEach {
                    yield(ArrayList<Int>(size).apply {
                        addAll(prefix)
                        add(i)
                        addAll(it)
                    })
                }
            }
        }
    }
}

class ReadOnlyIntArray internal constructor(@PublishedApi internal val wrapped: IntArray) : AbstractList<Int>() {
    @Suppress("OVERRIDE_BY_INLINE", "NOTHING_TO_INLINE") // save some boxing cost
    override inline operator fun get(index: Int): Int = wrapped[index]

    override val size: Int get() = wrapped.size

    override operator fun iterator(): IntIterator = wrapped.iterator()

    override fun toString(): String = wrapped.contentToString()

    // if it weren't for hashCode and equals
    // this could be made an inline value class
    override fun hashCode(): Int = wrapped.contentHashCode()
    override fun equals(other: Any?): Boolean {
        if (other is ReadOnlyIntArray) return wrapped.contentEquals(other.wrapped)
        return super.equals(other)
    }

}

fun AtomicInteger.gte(count: Int): Boolean {
    val value = get()
    // guards against overflow
    return value < 0 || value >= count
}

fun ByteArray.contains(element: Byte, start: Int, end: Int): Boolean {
    for (index in start until end) {
        if (element == this[index]) {
            return true
        }
    }
    return false
}

const val ONE_MIL = 1 shl 20

val dateFormat: DateTimeFormatter = DateTimeFormatterBuilder().appendPattern("HH:mm:ss").toFormatter()

/**
 * See [combinatorial number system](https://en.wikipedia.org/wiki/Combinatorial_number_system)
 */
fun flatten(s: Int, h: Int, d: Int): Int {
    val second = s + h + 1
    val third = second + d + 1

    return s + second * (second - 1) / 2 + third * (third - 1) * (third - 2) / 6
}
