package com.github.phisgr.dds

import java.lang.foreign.MemorySegment

fun MemorySegment.getInt(index: Int): Int = STUB()
fun MemorySegment.setInt(index: Int, value: Int) {
    STUB()
}

val MemorySegment.intSize: Int get() = STUB()

fun Int.toVulnerability(): Vulnerability = STUB()

fun Int.toRank(): Rank {
    STUB()
}

fun Int.toHolding(): Holding = STUB()
