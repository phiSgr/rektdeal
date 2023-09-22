package com.github.phisgr.dds

import java.lang.foreign.Arena
import kotlin.test.Test
import kotlin.test.assertEquals

class TestPar {

    @Test
    fun testPar() {
        Arena.ofConfined().use { arena ->
            val deal = DdTableDeal(arena)
            val res = DdTableResults(arena)

            val par = ParResults(arena)
            val parBin = ParResultsMaster(arena)
            val parStr = ParResultsDealer(arena)

            repeat(3) { i ->
                deal.cards.fromPbn(i)

                calcDdTable(deal, res)
                println(res)
                compareTable(res.resTable, i)

                dealerParBin(res, parBin, dealer[i], vul[i])
                println(parBin)
                assertEquals(dealerScore[i], parBin.score)
                assertEquals(dealerParNo[i], parBin.number)
                assertEquals(dealerContract[i], parBin.contracts[0].let {
                    val result = when {
                        it.underTricks > 0 -> {
                            assertEquals(0, it.overTricks)
                            "-${it.underTricks}"
                        }

                        it.overTricks > 0 -> {
                            assertEquals(0, it.underTricks)
                            "+${it.overTricks}"
                        }

                        else -> {
                            assertEquals(0, it.underTricks)
                            assertEquals(0, it.overTricks)
                            ""
                        }
                    }
                    val sac = if (it.underTricks == 0) "" else "*"
                    "${it.level}${it.denom}${sac}-${it.seats}$result"
                })

                dealerPar(res, parStr, dealer[i], vul[i])
                println(parStr)

                assertEquals(dealerParNo[i], parStr.number)
                assertEquals(dealerScore[i], parStr.score)
                assertEquals(dealerContract[i], parStr.contracts[0])

                par(res, par, vul[i])
                println(par)

                assertEquals("NS ${dealerScore[i]}", par.parScore[0])
                assertEquals("EW ${-dealerScore[i]}", par.parScore[1])
                val contract = parBin.contracts[0].let {
                    val sac = if (it.underTricks == 0) "" else "x"
                    "${it.level}${it.denom}${sac}"
                }
                assertEquals("NS:${parBin.contracts[0].seats} $contract", par.parContractsString[0])
                assertEquals("EW:${parBin.contracts[0].seats} $contract", par.parContractsString[1])
            }
            println()

            // test empty
            res.memory.fill(0)
            dealerParBin(res, parBin, dealer[0], vul[0])
            println(parBin)
            assertEquals(0, parBin.score)
            assertEquals(1, parBin.number)
            // parBin.contracts is not written. i.e. contains previous result

            dealerPar(res, parStr, dealer[0], vul[0])
            println(parStr)
            // parStr.score is not written. i.e. contains previous result

            assertEquals(1, parStr.number)
            assertEquals("pass", parStr.contracts[0])

            par(res, par, vul[0])
            println(par)

            assertEquals("NS 0", par.parScore[0])
            assertEquals("EW 0", par.parScore[1])
            assertEquals("NS:", par.parContractsString[0])
            assertEquals("EW:", par.parContractsString[1])
        }
    }

    @Test
    fun testBoth7n() {
        // https://bridge.thomasoandrews.com/deals/everybody/
        Arena.ofConfined().use { arena ->
            val deal = DdTableDeal(arena)
            val cards = deal.cards
            cards.memory.fill(0)
            cards[NORTH, S] = Holding("AKQJT98")
            cards[WEST, D] = cards[NORTH, S]
            cards[NORTH, H] = Holding("AKQJT9")
            cards[WEST, C] = cards[NORTH, H]
            cards[EAST, S] = Holding("765432")
            cards[SOUTH, D] = cards[EAST, S]
            cards[EAST, H] = Holding("8765432")
            cards[SOUTH, C] = cards[EAST, H]

            val ddTable = DdTableResults(arena)

            calcDdTable(deal, ddTable)
            println(ddTable)

            println("=== Testing par")

            ParResults(arena).let { res ->
                par(ddTable, res, Vulnerability.EW)
                println(res)
                assertEquals(2, res.parScore.size())
                assertEquals("NS 1520", res.parScore[0])
                assertEquals("EW 2220", res.parScore[1])
                assertEquals(2, res.parContractsString.size())
                assertEquals("NS:N 7N", res.parContractsString[0])
                assertEquals("EW:E 7N", res.parContractsString[1])
            }

            println("=== Testing sidesPar")

            ParResultsDealer.Array(arena, 2).let { res ->
                sidesPar(ddTable, res, Vulnerability.BOTH)
                println(res)
                (0..1).forEach { i ->
                    assertEquals(1, res[i].number)
                    assertEquals(2220, res[i].score)
                    val by = if (i == 0) "N" else "E"
                    assertEquals("7N-$by", res[i].contracts[0])
                }
            }

            println("=== Testing sidesParBin")

            ParResultsMaster.Array(arena, 2).let { res ->
                sidesParBin(ddTable, res, Vulnerability.BOTH)
                println(res)
                val dealTexts = (0..1).map { i ->
                    assertEquals(1, res[i].number)
                    assertEquals(2220, res[i].score)


                    assertEquals(0, res[i].contracts[0].underTricks)
                    assertEquals(0, res[i].contracts[0].overTricks)
                    assertEquals(7, res[i].contracts[0].level)
                    assertEquals(N, res[i].contracts[0].denom)
                    assertEquals(if (i == 0) NORTH else EAST, res[i].contracts[0].seats)

                    val resString = arena.allocate(128).also {
                        convertToDealerTextFormat(res[i], it)
                    }.getUtf8String(0)
                    println(resString)
                    val by = if (i == 0) "N" else "E"
                    assertEquals("Par 2220: $by 7N", resString)

                    resString
                }

                val resText = ParTextResults(arena)
                convertToSidesTextFormat(res, resText)
                (0..1).forEach { i ->
                    // LMAO
                    assertEquals(resText.parText[i], dealTexts[i].replace(Regex("N$"), "NT"))
                }
                assertEquals(false, resText.equal)
                println(resText)
            }

            println("=== Testing dealerPar")

            ParResultsDealer(arena).let { res ->
                dealerPar(ddTable, res, NORTH, Vulnerability.BOTH)
                println(res)
                assertEquals(1, res.number)
                assertEquals(2220, res.score)
                // because north opens, east can't bid 7N
                assertEquals("7N-N", res.contracts[0])
            }

            println("=== Testing dealerParBin")

            ParResultsMaster(arena).let { res ->
                dealerParBin(ddTable, res, EAST, Vulnerability.NS)
                println(res)
                assertEquals(1, res.number)
                assertEquals(-1520, res.score) // from North's perspective
                assertEquals(0, res.contracts[0].underTricks)
                assertEquals(0, res.contracts[0].overTricks)
                assertEquals(7, res.contracts[0].level)
                assertEquals(N, res.contracts[0].denom)
                assertEquals(EAST, res.contracts[0].seats)

                val resString = arena.allocate(128)
                convertToDealerTextFormat(res, resString)
                assertEquals("Par -1520: E 7N", resString.getUtf8String(0))
            }

        }
    }
}
