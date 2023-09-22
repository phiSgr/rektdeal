package com.github.phisgr.dds

import java.lang.foreign.MemorySegment
import java.lang.foreign.SegmentAllocator

internal fun STUB(): Nothing = throw NotImplementedError()

public class Deal(
    public val memory: MemorySegment,
) {
    public var trump: Strain
        get() = STUB()
        set(`value`) {
            STUB()
        }

    public var first: Direction
        get() = STUB()
        set(`value`) {
            STUB()
        }

    public val currentTrickSuit: Suit.Array
        get() = STUB()

    public val currentTrickRank: Rank.Array
        get() = STUB()

    public val remainCards: Cards
        get() = STUB()

    public constructor(allocator: SegmentAllocator) : this(STUB() as MemorySegment)

    override fun toString(): String = STUB()

    public class Array(
        public val memory: MemorySegment,
    ) {
        public constructor(allocator: SegmentAllocator, size: Int) :
            this(STUB())

        public operator fun `get`(index: Int): Deal = STUB()

        public fun size(): Int = STUB()

        public fun toString(count: Int): String = STUB()

        override fun toString(): String = STUB()
    }
}


public class FutureTricks(
    public val memory: MemorySegment,
) {
    public val nodes: Int
        get() = STUB()

    public val cards: Int
        get() = STUB()

    public val suit: Suit.Array
        get() = STUB()

    public val rank: Rank.Array
        get() = STUB()

    public val equals: Holding.Array
        get() = STUB()

    public val score: CIntArray
        get() = STUB()

    public constructor(allocator: SegmentAllocator) : this(STUB() as MemorySegment)

    override fun toString(): String {
        STUB()
    }

    public class Array(
        public val memory: MemorySegment,
    ) {
        public constructor(allocator: SegmentAllocator, size: Int) :
            this(STUB())

        public operator fun `get`(index: Int): FutureTricks = STUB()

        public fun size(): Int = STUB()

        public fun toString(count: Int): String = STUB()

        override fun toString(): String = STUB()
    }
}
