package com.github.phisgr.rektdeal

import com.github.phisgr.dds.FutureTricks
import com.github.phisgr.dds.threadCount
import com.github.phisgr.rektdeal.internal.ONE_MIL
import com.github.phisgr.rektdeal.internal.dateFormat
import com.github.phisgr.rektdeal.internal.gte
import java.lang.foreign.Arena
import java.time.LocalDateTime
import java.util.concurrent.Callable
import java.util.concurrent.StructuredTaskScope
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.Function
import com.github.phisgr.dds.Deal as DdsDeal

/**
 * Thread-local object for reusing [DdsDeal] and [futureTricks]. See [solverResources].
 */
class SolverResources(val threadIndex: Int, val arena: Arena) {
    // Using `LazyThreadSafetyMode.NONE` for DDS wrapper objects because this class is meant to be thread-local.

    val deal: DdsDeal by lazy(LazyThreadSafetyMode.NONE) { DdsDeal(arena) }
    val futureTricks: FutureTricks by lazy(LazyThreadSafetyMode.NONE) { FutureTricks(arena) }
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
            arena = null
        }
        return action(deal, futureTricks, threadIndex)
    } finally {
        arena?.close()
    }
}

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
    // given the time taken to generate and solve a deal,
    // contention should not be a big problem
    val dealCounter = AtomicInteger()

    return StructuredTaskScope.ShutdownOnFailure(
        null,
        // default is ofVirtual,
        // but we're not doing IO to benefit from that
        Thread.ofPlatform().factory()
    ).use { scope ->
        val states = List(threadCount) { threadIndex ->
            scope.fork(object : ResourceCallable<T>(threadIndex, dealCounter) {
                override fun action(): T {
                    val d = dealer()
                    val s = state()

                    while (!counter.gte(count)) {
                        if (Thread.interrupted()) break
                        val dealt = d(maxTry = ONE_MIL) { accept(it) }
                        if (dealt != null) {
                            val current = counter.getAndIncrement()
                            if (current in 0..<count) { // guards against overflow
                                action(current + 1, dealt, s)
                            }
                        }
                    }

                    return s
                }
            })
        }
        scope.join().throwIfFailed(Function.identity())
        states.map { it.get() }
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

abstract class ResourceCallable<T>(private val threadIndex: Int, protected val counter: AtomicInteger) : Callable<T> {
    override fun call(): T {
        return Arena.ofConfined().use { arena ->
            solverResources.set(SolverResources(threadIndex, arena))
            action()
        }
    }

    abstract fun action(): T
}
