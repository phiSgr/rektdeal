@file:OptIn(ExperimentalTypeInference::class)

package com.github.phisgr.rektdeal.internal

import com.github.phisgr.dds.Direction
import com.github.phisgr.rektdeal.PreDeal
import com.github.phisgr.rektdeal.PreDealCards
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.util.concurrent.ThreadLocalRandom
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

fun ByteArray.contains(element: Byte, start: Int, end: Int): Boolean {
    for (index in start until end) {
        if (element == this[index]) {
            return true
        }
    }
    return false
}

// in hot loops, we check every once in a while if the thread is interrupted
const val ONE_MIL = 1 shl 20

val dateFormat: DateTimeFormatter = DateTimeFormatterBuilder().appendPattern("HH:mm:ss").toFormatter()

fun validateOpeningLeadPreDeals(
    leader: Direction,
    hand: PreDealCards,
    extraPreDeals: Map<Direction, PreDeal>,
): Map<Direction, PreDeal> {
    hand.requireWholeHand()

    return extraPreDeals.toMutableMap().also {
        val oldValue = it.put(leader, hand)
        require(oldValue == null) { "extraPreDeals should not contain leader." }
    }
}

fun PreDealCards.requireWholeHand() {
    require(holding.size == 13) {
        "$holding is incomplete. Only ${holding.size} cards."
    }
}

fun ByteArray.shuffleWithoutPermuteHead(length: Int) {
    val random = ThreadLocalRandom.current()
    // length is usually 13, but can be smaller because of partial pre-deal
    // shuffling through 0..<length is useless, as that does not change which card goes to which hand
    for (i in lastIndex downTo length) {
        val j = random.nextInt(i + 1)
        val copy = this[i]
        this[i] = this[j]
        this[j] = copy
    }
}
