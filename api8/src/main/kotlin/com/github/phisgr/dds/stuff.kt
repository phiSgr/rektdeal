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

public class ParResultsMaster(
    public val memory: MemorySegment,
) {
    public val score: Int
        get() = STUB()

    public val number: Int
        get() = STUB()

    public val contracts: ContractType.Array
        get() = STUB()


    public constructor(allocator: SegmentAllocator) : this(STUB() as MemorySegment)

    public class Array(
        public val memory: MemorySegment,
    ) {
        public constructor(allocator: SegmentAllocator, size: Int) :
            this(STUB())

        public operator fun `get`(index: Int): ParResultsMaster =
            STUB()

        public fun size(): Int = STUB()

        public fun toString(count: Int): String = STUB()

    }
}

public class ContractType(
    public val memory: MemorySegment,
) {
    public val underTricks: Int
        get() = STUB()

    public val overTricks: Int
        get() = STUB()

    public val level: Int
        get() = STUB()

    public val denom: Strain
        get() = STUB()

    public val seats: Seats
        get() = STUB()

    public constructor(allocator: SegmentAllocator) : this(STUB() as MemorySegment)

    public class Array(
        public val memory: MemorySegment,
    ) {
        public constructor(allocator: SegmentAllocator, size: Int) :
            this(STUB())

        public operator fun `get`(index: Int): ContractType = STUB()

        public fun size(): Int = STUB()

        public fun toString(count: Int): String = STUB()
    }
}

public class DdTableDeal(
    public val memory: MemorySegment,
) {
    public val cards: Cards
        get() = Cards(memory)

    public constructor(allocator: SegmentAllocator) : this(STUB() as MemorySegment)
}

public class DdTableResults(
    public val memory: MemorySegment,
) {
    public val resTable: DdTable
        get() = DdTable(memory)

    public constructor(allocator: SegmentAllocator) : this(STUB() as MemorySegment)
}
