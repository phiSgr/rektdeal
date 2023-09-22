package com.github.phisgr.dds

import java.lang.foreign.Arena
import kotlin.test.Test

class TestDdTable {

    @Test
    fun testCalcDdTable() {
        Arena.ofConfined().use { arena ->
            val deal = DdTableDeal(arena)
            val res = DdTableResults(arena)

            repeat(3) { i ->
                deal.cards.fromPbn(i)

                calcDdTable(deal, res)
                println(res)
                println()
                compareTable(res.resTable, i)
            }
        }
    }

    @Test
    fun testCalcDdTablePbn() {
        repeat(3) { i ->
            Arena.ofConfined().use { arena -> // does not reuse memory this time
                val deal = DdTableDealPBN(arena)
                val res = DdTableResults(arena)

                deal.cards = deals[i]

                calcDdTablePBN(deal, res)
                println(res)
                println()
                compareTable(res.resTable, i)
            }
        }
    }

}
