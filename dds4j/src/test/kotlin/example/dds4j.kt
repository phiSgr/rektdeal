package example

import com.github.phisgr.dds.*
import java.lang.foreign.Arena

fun dds4jKotlin() {
    val arena = Arena.global()
    val deal = DealPBN(arena)
    deal.trump = S // property access syntax
    deal.first = NORTH
    deal.currentTrickSuit.clear()
    deal.currentTrickRank.clear()
    deal.remainCards = "N:QJ6.K652.J85.T98 873.J97.AT764.Q4 K5.T83.KQ9.A7652 AT942.AQ4.32.KJ3"

    val futureTricks = FutureTricks(arena)
    solveBoardPBN(deal, target = -1, solutions = 1, mode = 1, futureTricks, thrId = 0) // named arguments

    println(futureTricks)
    println("The double dummy tricks for declarer is ${13 - futureTricks.score[0]}") // indexed access operator
}
