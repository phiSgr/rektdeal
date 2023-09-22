package com.github.phisgr

import kotlin.math.pow
import kotlin.system.measureNanoTime

inline fun <T> logTimeMs(s: (String) -> String = { "Took $it." }, block: () -> T): T {
    var stuff: T
    val time = measureNanoTime {
        stuff = block()
    } * 10.0.pow(-6.0)
    println(s("%.3fms".format(time)))
    return stuff
}
