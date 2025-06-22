package com.github.phisgr.rektdeal

import com.github.phisgr.dds.Rank
import com.github.phisgr.rektdeal.internal.sumOf

/**
 * Guaranteed to be descending.
 */
abstract class RankList internal constructor() : AbstractList<Rank>() {
    abstract override fun get(index: Int): Rank
    override fun listIterator(): RankListIterator = RankListIterator(this)

    /**
     * Higher order functions like `forEach` cause boxing, as of 1.9.20.
     * When in doubt, use `for in` loop.
     */
    override fun iterator(): RankListIterator = RankListIterator(this)

    /**
     * half-losers - losers scaled by 2 to be an integral value
     */
    fun losersX2(): Int {
        var topCardIndex = 0
        var halfLosers = 0
        repeat(3.coerceAtMost(size)) { i ->
            if (this[topCardIndex].encoded != Rank.A.encoded - i) {
                halfLosers += 2
            } else if (i != 2) {
                topCardIndex++
            } else { // it's the queen iteration
                if (topCardIndex == 0 && this[1] < Rank.T) {
                    halfLosers++
                }
            }
        }
        return halfLosers
    }

    /**
     * new losing trick count scaled by 2 to be an integral value
     */
    fun newLtcX2(): Int {
        var topCardIndex = 0
        var halfLosers = 0
        repeat(3.coerceAtMost(size)) { i ->
            if (this[topCardIndex].encoded != Rank.A.encoded - i) {
                halfLosers += 3 - i
            } else {
                topCardIndex++
            }
        }
        return halfLosers
    }

    /**
     * playing tricks scaled by 2 to be an integral value
     * See [SuitHolding.playingTricks]
     */
    fun playingTricksX2(): Int {
        val extraLength = (size - 3).coerceAtLeast(0)
        val top3 = sumOf(end = size - extraLength) { i ->
            this[i].encoded shl (8 - 4 * i)
        }

        // A K Q J T 9 8 7
        // e d c b a 9 8 7
        return extraLength * 2 + when {
            top3 == 0xedc -> 6 // AKQ
            top3 == 0xedb || top3 == 0xecb -> 5 // AKJ AQJ
            top3 >= 0xed0 || top3 == 0xeca || top3 == 0xdcb -> 4 // AK(x) AQT KQJ
            top3 >= 0xeb0 -> 3 // A(Q/J)(x)
            top3 >= 0xe00 -> 2 // A(xx)
            top3 == 0xdba || top3 >= 0xdc2 -> 3 //  KJT KQx
            top3 >= 0xdb0 -> 2 // K(Q/J)(x)
            top3 > 0xd00 -> 1 // if (top3 and 0xf == 0) 0.5 else 1.0 // Kx(x)
            top3 == 0xd00 -> 0 // stiff K
            top3 >= 0xcb2 -> 2 // QJx
            top3 >= 0xc20 -> if (top3 and 0xf == 0) 0 else 1 // 0.5 // Qx(x)
            top3 == 0xc00 -> 0 // stiff Q
            top3 >= 0xba2 -> 1 // JTx
            else -> 0
        }
    }
}

class RankListIterator(private val rankList: RankList) : ListIterator<Rank> {
    private var index = 0
    override fun hasNext(): Boolean = index < rankList.size
    override fun hasPrevious(): Boolean = index > 0

    override fun next(): Rank {
        if (!hasNext()) throw NoSuchElementException()
        return rankList[index++]
    }

    override fun nextIndex(): Int = index

    override fun previous(): Rank {
        if (!hasPrevious()) throw NoSuchElementException()
        return rankList[--index]
    }

    override fun previousIndex(): Int = index - 1
}
