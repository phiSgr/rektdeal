package com.github.phisgr.dds

import com.github.phisgr.dds.internal.tryLoadLib
import dds.Dds
import java.lang.foreign.Arena
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.withLock

val threadCount = Arena.ofConfined().use {
    tryLoadLib()

    val threadCount = when (val override = System.getenv("DDS_MAX_THREADS")) {
        null -> 0 // auto-configure
        else -> override.toInt()
    }

    Dds.SetMaxThreads(threadCount)
    val info = DdsInfo(it)
    Dds.GetDDSInfo(info.memory)
    info.noOfThreads
}

private val allThreadLock = ReentrantReadWriteLock()
val singleThreadLock: Lock = allThreadLock.readLock()
val masterLock: Lock = allThreadLock.writeLock()

// List.of creates an immutable list. Kotlin `listOf` creates a read-only list.
val locks: List<ReentrantLock> = java.util.List.of(*Array(threadCount) { ReentrantLock() })

inline fun <T> withAllThreads(action: () -> T): T {
    return masterLock.withLock { action() }
}

inline fun <T> withThread(id: Int, action: () -> T): T {
    return singleThreadLock.withLock {
        locks[id].withLock {
            action()
        }
    }
}
