package com.github.phisgr.rektdeal

import com.github.phisgr.dds.Rank

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
