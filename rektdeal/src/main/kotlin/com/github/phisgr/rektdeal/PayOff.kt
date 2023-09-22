package com.github.phisgr.rektdeal

import com.github.phisgr.rektdeal.internal.mapToIntArray
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.util.function.IntBinaryOperator
import kotlin.math.sign
import kotlin.math.sqrt

private const val BRIGHT_GREEN = "\u001b[1m\u001b[32m"
private const val BRIGHT_RED = "\u001b[1m\u001b[31m"
private const val RESET_ALL = "\u001b[0m"

/**
 * Not thread-safe.
 * With the [multiThread] function, use the `state` parameter to create one.
 * For an example, see the [openingLead] function.
 */
class PayOff<T>(val entries: List<T>, val diff: IntBinaryOperator) {
    companion object {
        private val impTable = intArrayOf(
            20, 50, 90, 130, 170, 220, 270, 320, 370, 430, 500, 600, 750, 900,
            1100, 1300, 1500, 1750, 2000, 2250, 2500, 3000, 3500, 4000
        )

        fun calculateImp(scoreDiff: Int): Int {
            val sign: Int = scoreDiff.sign
            val index = impTable.binarySearch(scoreDiff * sign)
            return sign * (if (index >= 0) (index + 1) else (-index - 1))
        }

        val matchPoint: IntBinaryOperator = IntBinaryOperator { us, them -> // can be tricks, can be scores
            (us - them).sign
        }

        val impFromScores: IntBinaryOperator = IntBinaryOperator { ourScore, theirScore ->
            calculateImp(ourScore - theirScore)
        }

        /**
         * Calculate the IMPs for the defenders
         */
        fun impFromTricks(contract: Contract, vulnerable: Boolean): IntBinaryOperator =
            IntBinaryOperator { ourTricks, theirTricks ->
                calculateImp(
                    contract.score(13 - theirTricks, vulnerable) -
                        contract.score(13 - ourTricks, vulnerable)
                )
            }

        fun <T> convertMapToArray(entries: List<T>, scoreMap: Map<T, Int>): IntArray =
            entries.mapToIntArray {
                scoreMap[it] ?: throw IllegalStateException(
                    "Entry $it not found in $scoreMap"
                )
            }
    }

    private val size = entries.size

    var count = 0
        private set
    private val sums = LongArray(size * size)
    private val squareSums = LongArray(size * size)

    fun addData(rawScores: IntArray) {
        require(rawScores.size == size)
        count++

        repeat(size) { i ->
            repeat(size) { j ->
                val index = i * size + j
                val diff = diff.applyAsInt(rawScores[i], rawScores[j]).toLong()
                sums[index] += diff
                squareSums[index] += diff * diff
            }
        }
    }

    fun addData(rawScores: Map<T, Int>) {
        addData(convertMapToArray(entries, rawScores))
    }

    fun combine(that: PayOff<T>): PayOff<T> {
        require(this.entries == that.entries)
        require(this.diff === that.diff)

        val res = PayOff(entries, diff)

        res.count = this.count + that.count
        repeat(sums.size) { i ->
            res.sums[i] = this.sums[i] + that.sums[i]
        }
        repeat(sums.size) { i ->
            res.squareSums[i] = this.squareSums[i] + that.squareSums[i]
        }

        return res
    }

    fun calcMeans(): DoubleArray = DoubleArray(sums.size) { sums[it].toDouble() / count }
    fun calcStdErrs(): DoubleArray = DoubleArray(sums.size) {
        // val stdDev = sqrt(
        //     (N * sumOfSquares - squareOfSums) /
        //         N * (N - 1)
        // )
        // https://en.wikipedia.org/wiki/Standard_deviation#Standard_deviation_of_the_mean
        // val stdErr = stdDev / sqrt(N)

        // extracting the two `/ sqrt(N)` gives the following expression
        sqrt(
            (squareSums[it].toDouble() * count - sums[it] * sums[it]) / (count - 1)
        ) / count
    }

    fun report(output: PrintStream = System.out) = output.run {
        val means = calcMeans()
        val stdErrs = calcStdErrs()

        val entryStrings = entries.map { it.toString().take(7).padEnd(8, ' ') }

        println(entryStrings.joinToString("", prefix = EIGHT_SPACES))

        repeat(size) { i ->
            print(entryStrings[i])
            repeat(size) { j ->
                val index = i * size + j
                val mean = means[index]
                val stdErr = stdErrs[index]
                val style = when {
                    mean > stdErr -> BRIGHT_GREEN
                    mean < -stdErr -> BRIGHT_RED
                    else -> ""
                }

                if (i != j) {
                    print(style)
                    print(String.format("%+.2f", mean).padEnd(8, ' '))
                    print(RESET_ALL)
                } else {
                    print(EIGHT_SPACES)
                }

                if (j == size - 1) {
                    println()
                }
            }

            print(EIGHT_SPACES)
            repeat(size) { j ->
                val index = i * size + j
                val stdErr = stdErrs[index]
                if (i != j) {
                    print(String.format("(%.2f)", stdErr).padEnd(8, ' '))
                } else {
                    print(EIGHT_SPACES)
                }
                if (j == size - 1) {
                    println()
                }
            }
        }
    }

    override fun toString(): String =
        ByteArrayOutputStream().also {
            report(output = PrintStream(it))
        }.toString(Charsets.UTF_8)

}

// Gradle has trouble with tabs when we have coloured output
private const val EIGHT_SPACES = "        "
