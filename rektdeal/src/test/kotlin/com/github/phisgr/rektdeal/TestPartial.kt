package com.github.phisgr.rektdeal

import com.github.phisgr.dds.Rank
import java.util.concurrent.ThreadLocalRandom
import kotlin.math.sqrt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.measureTime

private data class StatSummary(val mean: Double, val sd: Double, val stdErr: Double) {
    fun compare(that: StatSummary, sdOfSd: Double) {
        print("mean ")
        assertNotTooDifferent(expected = this.mean, actual = that.mean, sd = stdErr)
        print("sd ")
        assertNotTooDifferent(expected = this.sd, actual = that.sd, sd = sdOfSd)
    }
}

private fun stat(sum: Long, sumOfSquares: Long, n: Double): StatSummary {
    val mean = sum / n
    val sd = sqrt((sumOfSquares * n - sum * sum) / n / (n - 1))
    return StatSummary(mean, sd, sd / sqrt(n))
}

private val N = if (shortTest) 10 else 1_000_000

private inline fun getStat(genDeal: () -> Deal): Pair<StatSummary, StatSummary> {
    var hLengthSum = 0L
    var hLengthSquareSum = 0L
    var hcpSum = 0L
    var hcpSquareSum = 0L
    val time = measureTime {
        repeat(N) {
            val hand = genDeal().north
            hLengthSum += hand.hearts.size
            hLengthSquareSum += hand.hearts.size * hand.hearts.size
            hcpSum += hand.hcp
            hcpSquareSum += hand.hcp * hand.hcp
        }
    }
    println("Took $time")
    val count = N.toDouble()
    return Pair(
        stat(hLengthSum, hLengthSquareSum, count),
        stat(hcpSum, hcpSquareSum, count),
    )
}

class TestPartial {

    @Test
    fun testHeartAce() {
        var i = 0
        val simpleDealer = Dealer()
        val (referenceHeartLength, referenceHcp) = getStat {
            simpleDealer { deal ->
                i++
                deal.north.hearts.contains(Rank.A)
            }
        }

        // only time when it's easy to know the probability in closed-form
        // we might as well test the trial count
        print("Took $i tries. ")
        assertNotTooDifferent(expected = 4 * N.toDouble(), actual = i.toDouble(), sd = sdNegativeBinomial(N, 0.25))

        println("heartLength $referenceHeartLength hcp $referenceHcp")

        val randomCard = ThreadLocalRandom.current().run {
            Card(suit = nextInt(4), rank = nextInt(2, 15)).encoded
        }
        var hasRandomCard = 0

        val stackedDealer = Dealer(N = "- A - -")
        val (stackHeartLength, stackHcp) = getStat {
            stackedDealer().also { deal ->
                var bitVector = HoldingBySuit(0)
                deal.cards.forEachIndexed { i, card ->
                    if (randomCard == card && i < 13) {
                        hasRandomCard++
                    }
                    bitVector = bitVector.withCard(card)
                }
                assertEquals(HoldingBySuit.WHOLE_DECK.encoded, bitVector.encoded)
            }
        }

        println("heartLength $stackHeartLength hcp $stackHcp")
        // sdOfSd estimated empirically at N=10^6
        // for a different N, this rough scaling should be in the same order of magnitude
        val referenceN = 1_000_000.0
        referenceHeartLength.compare(stackHeartLength, sdOfSd = 0.00115 * sqrt(referenceN / N))
        referenceHcp.compare(stackHcp, sdOfSd = 0.00251 * sqrt(referenceN / N))

        // specifying the heart ace should not mess up the probability of holding another card
        val holdCardChance = 12 / 51.0
        val expected = N * holdCardChance
        print("hasRandomCard ")
        assertNotTooDifferent(
            expected = expected,
            actual = hasRandomCard.toDouble(),
            sd = sdBinomial(N, holdCardChance)
        )
    }
}
