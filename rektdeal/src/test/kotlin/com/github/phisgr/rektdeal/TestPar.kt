package com.github.phisgr.rektdeal

import com.github.phisgr.dds.*
import com.github.phisgr.rektdeal.internal.mapToArray
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.atomic.AtomicInteger
import kotlin.random.Random
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals

private const val COUNT = 500
private val iteration = if (System.getenv("EXTENDED_TEST")?.toBoolean() ?: false) COUNT else 15

private data class SolvedDeal(
    val deal: Deal,
    val dealer: Direction,
    val vulnerability: Vulnerability,
    val contracts: List<String>,
)

class TestPar {
    private val preCalculated: List<SolvedDeal> = run {
        val file = this::class.java.classLoader.getResourceAsStream("par.txt")!!
            .bufferedReader()
            .use { it.readText() }
        val deals = file.split("\n\n")
        assertEquals(COUNT, deals.size)

        val mapped = deals.mapToArray { record ->
            val lines = record.split("\n")
            val (d, v, deal) = lines[0].split(" ", limit = 3)
            val dealer = Direction.fromChar(d.single())
            val vul = Vulnerability.valueOf(v)
            val hands = deal.substring(1).split(" \"")
            SolvedDeal(
                deal = Dealer(DIRECTIONS.associateWith { direction ->
                    PreDealCards(hands[direction.encoded]).also {
                        require(it.holding.size == 13)
                    }
                })(),
                dealer,
                vul,
                lines.subList(1, lines.size)
            )
        }

        if (iteration < COUNT) {
            mapped.shuffle()
        }
        listOf(*mapped).subList(0, iteration)
    }

    private fun testDeal(solvedDeal: SolvedDeal, mt: Boolean?) {
        println(solvedDeal.deal)
        val (ddTable, contracts) = solvedDeal.deal.parScore(
            solvedDeal.dealer, solvedDeal.vulnerability,
            multiThreaded = mt
        )

        println(ddTable)

        val contractStrings = contracts.mapTo(mutableSetOf()) {
            val difference = it.tricks - it.contract.level
            val result = when {
                difference == 0 -> "="
                difference > 0 -> "+$difference"
                else -> "$difference"
            }
            val score = "${if (it.score > 0) "+" else ""}${it.score}"
            "${it.contract} ${it.declarer} $result $score"
        }
        assertEquals(solvedDeal.contracts.toSet(), contractStrings)

    }

    @Test
    // all calculation done in one single thread
    // of course it's the slowest
    fun testParSequential() {
        preCalculated.forEach { testDeal(it, mt = false) }
    }

    @Test
    // threads idle waiting on others during fork-join per deal
    // performance is not great
    fun testParParallel() {
        preCalculated.forEach { testDeal(it, mt = true) }
    }

    @Test
    // each thread grabs a task and processes it independently
    // approx. 2x as fast as testParParallel
    fun testParSplitSeq() {
        val queue = ArrayBlockingQueue(iteration, false, preCalculated)
        val i = AtomicInteger()
        launchThreadsWithResources(threadCount = threadCount) {
            while (true) {
                val polled = queue.poll() ?: break
                testDeal(polled, mt = false)
                i.incrementAndGet()
            }
        }
        assertEquals(iteration, i.get())
    }

    @Test
    // spawning multithreaded tasks from multiple threads makes no sense
    // takes about the same time as testParParallel
    fun testParSplitParallel() {
        val queue = ArrayBlockingQueue(iteration, false, preCalculated)
        val i = AtomicInteger()
        launchThreadsWithResources(threadCount = threadCount) {
            while (true) {
                val polled = queue.poll() ?: break
                testDeal(polled, mt = true)
                i.incrementAndGet()
            }
        }
        assertEquals(iteration, i.get())
    }

    /**
     * found using the [lookForNobodyMakes] function
     *
     * the par score being 0 is so incredibly rare (roughly 1 in 420k),
     * so i thought it might be interesting to list a few such hands
     * that are selected from a random distribution rather than carefully constructed.
     */
    private val nobodyMakes = listOf(
        listOf("874 K9654 A3 AT2", "AJ32 Q8 KJT QJ98", "QT9 A2 Q986 7643", "K65 JT73 7542 K5"),
        listOf("AKJ7 JT8 T532 A7", "T654 AKQ2 74 J62", "Q3 963 K96 KQ853", "982 754 AQJ8 T94"),
        listOf("75 T72 AT65 AKJ6", "KQ96 A4 Q84 T753", "AJ83 8653 732 Q8", "T42 KQJ9 KJ9 942"),
        listOf("854 A Q8762 KQ98", "AQ J87 AJ954 T72", "T973 KQ643 T3 A6", "KJ62 T952 K J543"),
        listOf("9862 T942 QJ T94", "A3 QJ8 9875 AJ86", "Q54 A76 AKT2 KQ5", "KJT7 K53 643 732"),
        listOf("8753 J7 T9 A8432", "QJ942 AK4 865 97", "AT6 Q865 KQJ2 KT", "K T932 A743 QJ65"),
        listOf("987 AJ76543 Q Q9", "AKQ65 K92 976 42", "JT42 8 AKJ32 JT3", "3 QT T854 AK8765"),
        listOf("QJ42 T62 J84 A85", "765 AJ85 KT73 76", "AK KQ74 Q95 9432", "T983 93 A62 KQJT"),
        listOf("Q972 AQ63 8 QJ96", "KJ4 JT84 J3 AK75", "A65 975 KQ972 T3", "T83 K2 AT654 842")
    )

    @Test
    fun testNobodyMakes() = nobodyMakes.forEach { (n, e, s, w) ->
        val deal = Dealer(N = n, E = e, S = s, W = w)()

        print(deal.handDiagram())

        fun test(mt: Boolean, cache: Map<Pair<Strain, Direction>, Int>? = null): DdTable {
            val (ddTable, contracts) = deal.parScore(
                DIRECTIONS.random(),
                Vulnerability.entries.random(),
                multiThreaded = mt,
                cachedResults = cache
            )
            assertEquals(emptyList(), contracts)
            return ddTable
        }

        val ddTable = test(mt = true)
        println(ddTable)
        println()

        val cache = mutableMapOf<Pair<Strain, Direction>, Int>()
        DIRECTIONS.toMutableList().apply { shuffle() }.take(Random.nextInt(DIRECTIONS.size)).forEach { direction ->
            STRAINS.toMutableList().apply { shuffle() }.take(Random.nextInt(STRAINS.size)).forEach { strain ->
                cache[Pair(strain, direction)] = ddTable[strain, direction]
            }
        }
        test(mt = false, cache)
    }


    @Test
    @Ignore
    fun lookForNobodyMakes() {
        val dealCounter = AtomicInteger()
        val toGenerate = 5

        // i started with using multiThread(count = toGenerate, accept = { ... }) { ... }
        // but its `dealCounter` is checked only after a hand is dealt,
        // or after a lot of hands are checked.

        // this clears up the hot loop in normal cases,
        // but with the `accept` logic heavy like below,
        // the threads will not know it's time to stop.

        // so we drop down to a lower level of abstraction
        val counters = launchThreadsWithResources(threadCount = threadCount) {
            val threadId = solverResources.get().threadIndex
            var i = 0
            val dealer = Dealer()
            while (dealCounter.get() < toGenerate) {
                val deal = dealer()

                val (ddTable, _) = deal.parScore(
                    // doesn't matter
                    NORTH, Vulnerability.NONE
                )
                val nobodyMakes = STRAINS.all { strain ->
                    DIRECTIONS.all { seat ->
                        ddTable[strain, seat] < 7
                    }
                }
                if (nobodyMakes) {
                    println(deal)
                    dealCounter.getAndIncrement()
                }
                i++
                if (i % 1024 == 0) {
                    log("thread $threadId: $i deals checked")
                }
            }
            i
        }
        println(counters)
        println(counters.sum())
    }
}
