package com.github.phisgr.dds

import dds.Dds
import java.lang.foreign.Arena

private val errorMessages = HashMap.newHashMap<Int, String>(26).also {
    var holeCount = 0
    val errorMessageRange = (1..19) + (98..104) + listOf(201, 202, 301)

    Arena.ofConfined().use { arena ->
        val res = arena.allocate(80)
        errorMessageRange.forEach { code ->
            Dds.ErrorMessage(-code, res)
            val msg = res.getUtf8String(0)
            if (msg == "Not a DDS error code") {
                holeCount++
            } else {
                it[-code] = msg
            }
        }
    }
    check(holeCount == 3)
    check(it.size == 26)
}

class DdsError(val code: Int) : Throwable(errorMessages[code] ?: "unknown code $code")

internal fun Int.checkErrorCode() {
    if (this != 1) {
        throw DdsError(this)
    }
}
