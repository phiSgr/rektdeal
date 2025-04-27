package com.github.phisgr.rektdeal

import com.github.phisgr.dds.DdTableResults
import com.github.phisgr.dds.FutureTricks
import com.github.phisgr.dds.ParResultsMaster
import com.github.phisgr.dds.threadCount
import com.github.phisgr.rektdeal.internal.*
import java.lang.foreign.Arena
import java.time.LocalDateTime
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import com.github.phisgr.dds.Deal as DdsDeal

/**
 * Thread-local object for reusing [DdsDeal] and [FutureTricks]. See [solverResources].
 */
class SolverResources(val threadIndex: Int, val arena: Arena) {
    // Using `LazyThreadSafetyMode.NONE` for DDS wrapper objects because this class is meant to be thread-local.

    val deal: DdsDeal by lazy(LazyThreadSafetyMode.NONE) { DdsDeal(arena) }
    val futureTricks: FutureTricks by lazy(LazyThreadSafetyMode.NONE) { FutureTricks(arena) }
    val ddTableResults: DdTableResults by lazy(LazyThreadSafetyMode.NONE) { DdTableResults(arena) }
    val parResultsMaster: ParResultsMaster by lazy(LazyThreadSafetyMode.NONE) { ParResultsMaster(arena) }
}

val solverResources = ThreadLocal<SolverResources>()

fun <T> maybeUseResources(action: (DdsDeal, FutureTricks, threadIndex: Int) -> T): T {
    val resources = solverResources.get()
    val threadIndex: Int
    val deal: DdsDeal
    val futureTricks: FutureTricks
    var arena: Arena? = null

    try {
        if (resources == null) {
            threadIndex = 0
            arena = Arena.ofConfined()
            deal = DdsDeal(arena)
            futureTricks = FutureTricks(arena)
        } else {
            threadIndex = resources.threadIndex
            deal = resources.deal
            futureTricks = resources.futureTricks
        }
        return action(deal, futureTricks, threadIndex)
    } finally {
        arena?.close()
    }
}

/**
 * Prints in the format "HH:mm:ss $[s]"
 */
fun log(s: Any?) {
    println("${dateFormat.format(LocalDateTime.now())} $s")
}

/**
 * Generates [count] deals.
 * Uses [threadCount] as determined by the DDS library.
 *
 * [dealer] is a function as [Dealer] objects are not thread-safe.
 * One has to be created for each thread.
 * They can, however, reuse the [SmartStack] objects.
 *
 * Be careful to not mutate non-thread-safe objects in the [action],
 * or in the [accept] criteria - the latter should be pure anyway.
 *
 * The dealCount in [action] is 1-based.
 * You can log the progress by writing
 * ```
 * if (dealCount % 1000 == 0) {
 *     log("$dealCount analyzed")
 * }
 * ```
 */
inline fun <T> multiThread(
    count: Int,
    crossinline dealer: () -> Dealer = { Dealer() },
    crossinline state: () -> T,
    crossinline accept: (Deal) -> Boolean = { true },
    crossinline action: (dealCount: Int, Deal, state: T) -> Unit,
): List<T> {
    // given the time taken to solve a deal,
    // contention should not be a big problem
    val dealCounter = AtomicInteger()
    return launchThreadsWithResources(threadCount = threadCount) {
        val d = dealer()
        val s = state()

        while (inRange(dealCounter.get(), count)) {
            if (Thread.interrupted()) break
            val dealt = d(maxTry = ONE_MIL) { accept(it) }
            if (dealt != null) {
                val current = dealCounter.getAndIncrement()
                if (inRange(current, count)) { // guards against overflow
                    action(current + 1, dealt, s)
                }
            }
        }

        s
    }
}

/**
 * An overload without the `state` param.
 *
 * You can
 * - close over your (thread-safe) state objects in your [action], or
 * - access some unshared object with `stateObjects[`[solverResources]`.get().threadIndex]`.
 */
inline fun multiThread(
    count: Int,
    crossinline dealer: () -> Dealer = { Dealer() },
    crossinline accept: (Deal) -> Boolean = { true },
    crossinline action: (dealCount: Int, Deal) -> Unit,
) {
    // mildly wasteful to create a List<Unit>
    multiThread(count, dealer, state = {}, accept) { dealCount, deal, _ ->
        action(dealCount, deal)
    }
}

inline fun <T> launchThreadsWithResources(
    threadCount: Int,
    // idk, maybe you have 96 threads and want each task to only take 8 threads
    // then you say useResourcesMultiThreaded(threadCount * 8, offset = 8 * i) { ...
    offset: Int = 0,
    crossinline action: () -> T,
): List<T> {
    require(threadCount + offset <= com.github.phisgr.dds.threadCount) {
        "Thread index ${threadCount + offset - 1} is out of range."
    }

    val throwable = AtomicReference<Throwable?>()

    val futures = Executors.newThreadPerTaskExecutor(Thread.ofPlatform().factory()).use { executor ->
        List(threadCount) { threadIndex ->
            safeSubmit(executor, object : ResourceCallable<T>(threadIndex + offset, throwable, executor) {
                override fun action(): T {
                    return action()
                }
            })
        }
    }
    handleError(throwable)
    return futures.map { it.get() }
}
