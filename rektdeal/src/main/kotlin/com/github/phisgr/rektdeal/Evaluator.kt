package com.github.phisgr.rektdeal

fun interface Evaluator {
    fun evaluate(holding: RankList): Int

    companion object {
        val hcp: Evaluator = Evaluator(4, 3, 2, 1)
        val qp: Evaluator = Evaluator(3, 2, 1)
        val controls: Evaluator = Evaluator(2, 1)

        operator fun invoke(vararg points: Int): Evaluator {
            require(points.size <= 13)
            val defensiveCopy = points.copyOf()
            return Evaluator { holding ->
                var sum = 0
                for (rank in holding) {
                    val index = 14 - rank.encoded
                    if (index >= defensiveCopy.size) {
                        break
                    }
                    sum += defensiveCopy[index]

                }
                sum
            }
        }
    }
}
