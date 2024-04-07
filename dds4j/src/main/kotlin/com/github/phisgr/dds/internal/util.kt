package com.github.phisgr.dds.internal

import com.github.phisgr.dds.Holding
import com.github.phisgr.dds.Rank
import com.github.phisgr.dds.Vulnerability
import java.lang.foreign.MemorySegment
import java.lang.foreign.ValueLayout


internal fun MemorySegment.getInt(index: Int): Int = get(ValueLayout.JAVA_INT, index * 4L)
internal fun MemorySegment.setInt(index: Int, value: Int) {
    set(ValueLayout.JAVA_INT, index * 4L, value)
}

internal val MemorySegment.intSize: Int get() = Math.toIntExact(byteSize() / 4)

fun Int.toVulnerability(): Vulnerability = Vulnerability.entries[this]

fun Int.toRank(): Rank {
    require(this in 2..14)
    return Rank(this)
}

fun Int.toHolding(): Holding = Holding(this)
