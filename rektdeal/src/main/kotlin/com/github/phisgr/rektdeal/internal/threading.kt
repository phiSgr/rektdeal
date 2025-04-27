package com.github.phisgr.rektdeal.internal

import com.github.phisgr.rektdeal.SolverResources
import com.github.phisgr.rektdeal.solverResources
import java.lang.foreign.Arena
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicReference


/**
 * Although unlikely in realistic scenarios,
 * with ~1 million deals checked per core,
 * int overflow can be reached within several minutes.
 * see [com.github.phisgr.rektdeal.TestMultiThreaded.testOverflow]
 */
fun inRange(current: Int, count: Int): Boolean = current in 0..<count

fun handleError(throwable: AtomicReference<Throwable?>) {
    // ExecutorService#close does not throw InterruptedException
    if (Thread.interrupted()) {
        throw InterruptedException()
    }

    throwable.get()?.let { throw it }
}

fun <T> safeSubmit(executor: ExecutorService, callable: Callable<T>): Future<T> = try {
    // ExecutorService#shutdownNow might be called before all the tasks are submitted
    executor.submit(callable)
} catch (e: RejectedExecutionException) {
    CompletableFuture.failedFuture(e)
}

abstract class ResourceCallable<T>(
    private val threadIndex: Int,
    private val throwable: AtomicReference<Throwable?>,
    private val executor: ExecutorService,
) : Callable<T> {
    override fun call(): T {
        try {
            return Arena.ofConfined().use { arena ->
                solverResources.set(SolverResources(threadIndex, arena))
                action()
            }
        } catch (e: Throwable) {
            throwable.compareAndSet(null, e)
            // A poor man's structured concurrency (awaitAllSuccessfulOrThrow, aka ShutdownOnFailure)
            // but I don't want to wait for it to come out of preview.
            executor.shutdownNow()
            throw e
        }
    }

    abstract fun action(): T
}
