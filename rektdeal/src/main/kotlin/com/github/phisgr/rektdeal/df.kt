package com.github.phisgr.rektdeal

import com.github.phisgr.rektdeal.internal.mapToArray
import org.jetbrains.kotlinx.dataframe.AnyFrame
import org.jetbrains.kotlinx.dataframe.api.DataFrameBuilder
import org.jetbrains.kotlinx.dataframe.api.FormattedFrame
import org.jetbrains.kotlinx.dataframe.api.format
import org.jetbrains.kotlinx.dataframe.api.with

data class Stat(val mean: Double, val stdErr: Double) {
    override fun toString() = "%+.2f ± %.2f".format(mean, stdErr)
}

fun <T> PayOff<T>.toDataFrame(): AnyFrame {
    val names = entries.mapToArray { it.toString() }
    val means = calcMeans()
    val stdErrs = calcStdErrs()

    val values = names.flatMapIndexed { i, name ->
        buildList(names.size + 1) {
            add(name)
            repeat(names.size) { j ->
                add(
                    if (i == j) {
                        null
                    } else {
                        val index = i * names.size + j
                        Stat(mean = means[index], stdErr = stdErrs[index])
                    }
                )
            }
        }
    }.toTypedArray<Any?>()
    return DataFrameBuilder(listOf("strategy", *names))(*values)
}


fun <T> PayOff<T>.toFormattedFrame(): FormattedFrame<Any?> {
    val names = entries.mapToArray { it.toString() }
    return toDataFrame()
        .format(*names)
        .with {
            if (it == null) return@with null
            val stat = it as Stat
            when {
                stat.mean < -stat.stdErr -> background(rgb(255, 192, 192))
                stat.mean > stat.stdErr -> background(rgb(192, 255, 192))
                else -> null
            }
        }
}

fun OpeningLeadStat.trickFrequencyTable(): AnyFrame {
    val values = trickFrequencies.flatMap { (key, value) ->
        listOf(key) + value.wrapped.asList()
    }.toTypedArray<Any?>()
    return DataFrameBuilder(listOf("card", *Array(14) { it.toString() }))(*values)
}
