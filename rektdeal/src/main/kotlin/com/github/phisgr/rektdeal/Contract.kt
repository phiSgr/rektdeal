package com.github.phisgr.rektdeal

import com.github.phisgr.dds.N
import com.github.phisgr.dds.Seats
import com.github.phisgr.dds.Strain

private val pattern = Regex("([1-7])(C|D|H|S|NT?)(X{0,2})", RegexOption.IGNORE_CASE)

data class ScoredContract(val contract: Contract, val declarer: Seats, val score: Int, val tricks: Int)

data class Contract(val level: Int, val strain: Strain, val doubled: Int) {
    init {
        require(level in 1..7)
        require(doubled in 0..2)
    }

    // Workaround for not having code before constructor call.
    private constructor(matchResult: MatchResult) : this(
        matchResult.groupValues[1].toInt(),
        Strain.fromChar(matchResult.groupValues[2].first().uppercaseChar()),
        doubled = matchResult.groupValues[3].length
    )

    constructor(contract: String) : this(
        pattern.matchEntire(contract) ?: throw IllegalArgumentException("Malformed contract: $contract")
    )

    fun score(tricks: Int, vulnerable: Boolean): Int {
        require(tricks in 0..13)

        val overTricks = tricks - (level + 6)
        return if (overTricks >= 0) {
            val perTrick = if (strain.encoded in 2..3) 20 else 30
            val belowTheLine = (perTrick * level + if (strain == N) 10 else 0) shl doubled
            val insult = 50 * doubled

            val bonus = when {
                belowTheLine < 100 -> 50 // partial
                vulnerable -> 500        // vulnerable game
                else -> 300              // non-vul game
            }

            val slam = when (level) {
                6 -> if (vulnerable) 750 else 500
                7 -> if (vulnerable) 1500 else 1000
                else -> 0
            }
            val perOverTrick = when (doubled) {
                0 -> perTrick
                else -> doubled * (if (vulnerable) 200 else 100)
            }
            belowTheLine + insult + bonus + slam + overTricks * perOverTrick
        } else {
            if (doubled == 0) {
                val perUnder = if (vulnerable) 100 else 50
                overTricks * perUnder
            } else {
                val penalty = when (overTricks) {
                    -1 -> if (vulnerable) -200 else -100
                    -2 -> if (vulnerable) -500 else -300
                    else -> 300 * overTricks + if (vulnerable) 100 else 400
                }
                penalty * doubled
            }
        }
    }

    override fun toString(): String = "$level$strain${"X".repeat(doubled)}"

}
