package com.github.phisgr.rektdeal

import com.github.phisgr.dds.*
import com.github.phisgr.rektdeal.internal.*
import java.lang.foreign.Arena
import java.lang.foreign.MemorySegment
import java.util.*
import kotlin.collections.AbstractList
import kotlin.math.max
import com.github.phisgr.dds.Deal as DdsDeal

internal const val SIZE = 52

class Deal internal constructor() : AbstractList<Hand>() {
    internal val cards: ByteArray = ByteArray(SIZE)

    override val size: Int get() = 4

    override fun get(index: Int): Hand {
        val hand = hands[index]
        val mask = 0b10000 shl index
        if (handInit and mask == 0) {
            handInit = handInit or mask
            val sortMask = 1 shl index
            if (handInit and sortMask == 0) {
                hand.sortCards()
            }
            hand.init()
        }
        return hand
    }

    override fun toString(): String = (0..3).joinToString(separator = " ") { this[it].toString() }

    private val hands = DIRECTIONS.mapToArray { direction ->
        Hand(cards, direction)
    }

    /**
     * Lowest 4 bits are for whether the [SuitHolding]s are sorted.
     * Then 4 bits for whether [Hand.init] is called.
     */
    private var handInit: Int = 0
    fun getHand(direction: Direction): Hand = this[direction.encoded]

    internal fun reset(presorted: Int) {
        handInit = presorted
    }

    val north: Hand get() = this[0]
    val east: Hand get() = this[1]
    val south: Hand get() = this[2]
    val west: Hand get() = this[3]

    /**
     * Load the cards of this deal into the DDS [Cards] object for solving.
     */
    fun setCards(ddsCards: Cards) {
        DIRECTIONS.forEach { direction ->
            var holdings = HoldingBySuit(0)
            val offset = direction.encoded * 13
            repeat(13) { i ->
                holdings = holdings.withCard(cards[offset + i])
            }
            SUITS.forEach { suit ->
                ddsCards[direction, suit] = holdings.getHolding(suit)
            }
        }
    }

    /**
     * Compute defenders' number of double dummy tricks for all leads.
     *
     * Cards for which the card immediately above is in the same hand are not
     * listed; i.e., equivalent leads are only listed once.
     */
    fun ddAllTricks(strain: Strain, leader: Direction): Map<Card, Int> {
        return maybeUseResources { deal, futureTricks, threadIndex ->
            deal.trump = strain
            deal.first = leader
            deal.currentTrickRank.clear()
            deal.currentTrickSuit.clear()
            setCards(deal.remainCards)

            solveBoard(deal, target = -1, solutions = 3, mode = 1, futureTricks, threadIndex)
            (0 until futureTricks.cards).associate {
                Card(futureTricks.suit[it], futureTricks.rank[it]) to futureTricks.score[it]
            }
        }
    }

    /**
     * @return the score of the [contract] when [ddTricks] is won.
     */
    fun ddScore(contract: Contract, declarer: Direction, vulnerable: Boolean): Int {
        return contract.score(ddTricks(contract.strain, declarer), vulnerable)
    }

    /**
     * @return number of double dummy tricks the [declarer] makes in [strain]
     */
    fun ddTricks(strain: Strain, declarer: Direction): Int =
        maybeUseResources { deal, futureTricks, threadIndex ->
            deal.trump = strain
            deal.first = declarer.next()
            deal.currentTrickRank.clear()
            deal.currentTrickSuit.clear()
            setCards(deal.remainCards)

            solveBoard(deal, target = -1, solutions = 1, mode = 1, futureTricks, threadIndex)
            13 - futureTricks.score[0]
        }


    /**
     * You can provide [cachedResults] when you're invoking [parScore] after
     * you have already solved the deal for some trump suit and declarer.
     *
     * If [multiThreaded], DDS's [calcDdTable], which solves the contracts in parallel, will be used.
     * It is `multiThreaded` by default unless
     * other threads are also using the solver (inferred from the presence of [solverResources]) or
     * there are `cachedResults`, which `calcDdTable` cannot make use of.
     *
     * @return A pair, [DdTable] and a list of par score contracts.
     * The list is empty if there's no making contract.
     */
    fun parScore(
        dealer: Direction,
        vulnerability: Vulnerability,
        cachedResults: Map<Pair<Strain, Direction>, Int>? = null,
        multiThreaded: Boolean? = null,
    ): Pair<DdTable, List<ScoredContract>> {
        var tempArena: Arena? = null

        try {
            val resources = solverResources.get()
            val parResultsMaster: ParResultsMaster
            val ddTableResults: DdTableResults
            val hasThreadId = resources != null

            // if current thread has a thread id, other threads are running.
            // calcDdTable cannot make use of cachedResults
            // with either case, do not use the multithreaded calcDdTable unless explicitly stated
            if (multiThreaded ?: (!hasThreadId && cachedResults.isNullOrEmpty())) {
                val ddTableDeal: DdTableDeal
                when (resources) {
                    null -> {
                        tempArena = Arena.ofConfined()
                        ddTableDeal = DdTableDeal(tempArena)
                        ddTableResults = DdTableResults(tempArena)
                        parResultsMaster = ParResultsMaster(tempArena)
                    }
                    else -> {
                        ddTableDeal = DdTableDeal(resources.deal.remainCards.memory)
                        ddTableResults = resources.ddTableResults
                        parResultsMaster = resources.parResultsMaster
                    }
                }
                setCards(ddTableDeal.cards)
                calcDdTable(ddTableDeal, ddTableResults)
            } else {
                val ddsDeal: DdsDeal
                val futureTricks: FutureTricks
                val threadIndex: Int
                when (resources) {
                    null -> {
                        tempArena = Arena.ofConfined()
                        ddsDeal = DdsDeal(tempArena)
                        futureTricks = FutureTricks(tempArena)
                        ddTableResults = DdTableResults(tempArena)
                        parResultsMaster = ParResultsMaster(tempArena)
                        threadIndex = 0
                    }
                    else -> {
                        ddsDeal = resources.deal
                        futureTricks = resources.futureTricks
                        ddTableResults = resources.ddTableResults
                        parResultsMaster = resources.parResultsMaster
                        threadIndex = resources.threadIndex
                    }
                }
                val ddTable = ddTableResults.resTable

                ddsDeal.currentTrickRank.clear()
                ddsDeal.currentTrickSuit.clear()
                setCards(ddsDeal.remainCards)

                for (strain in STRAINS) {
                    for (declarer in DIRECTIONS) {
                        when (val tricks = cachedResults?.get(Pair(strain, declarer))) {
                            null -> {
                                ddsDeal.trump = strain
                                ddsDeal.first = declarer.next()
                                solveBoard(ddsDeal, target = -1, solutions = 1, mode = 1, futureTricks, threadIndex)
                                ddTable[strain, declarer] = 13 - futureTricks.score[0]
                            }
                            else -> ddTable[strain, declarer] = tricks
                        }
                    }
                }
            }

            dealerParBin(ddTableResults, parResultsMaster, dealer, vulnerability)
            val score = parResultsMaster.score
            val contracts = List(if (score == 0) 0 else parResultsMaster.number) {
                val ddsContract = parResultsMaster.contracts[it]
                ScoredContract(
                    Contract(
                        level = ddsContract.level,
                        strain = ddsContract.denom,
                        // if the contract goes down, it's always doubled, never redoubled
                        doubled = ddsContract.underTricks.coerceAtMost(1)
                    ),
                    declarer = ddsContract.seats,
                    score = score,
                    tricks = ddsContract.level + ddsContract.overTricks - ddsContract.underTricks
                )
            }

            val onHeap = MemorySegment.ofArray(IntArray(20))
            onHeap.copyFrom(ddTableResults.resTable.memory)
            return Pair(DdTable(onHeap), contracts)
        } finally {
            tempArena?.close()
        }
    }

    @Suppress("LocalVariableName")
    fun handDiagram(
        vararg directions: Direction,
    ): String {
        if (directions.isEmpty()) return handDiagram(NORTH, EAST, SOUTH, WEST)
        val N = NORTH in directions
        val E = EAST in directions
        val S = SOUTH in directions
        val W = WEST in directions

        val EIGHT_SPACES = "        "
        val builder = StringBuilder()

        fun appendNS(hand: Hand) {
            hand.handDiagram().split("\n").forEach {
                if (W) {
                    builder.append(EIGHT_SPACES)
                }
                builder.append(it)
                builder.append('\n')
            }
        }

        if (N) appendNS(north)
        val westLines: List<String>
        val eastLines: List<String>

        if (W && E) {
            westLines = west.handDiagram().split("\n").map { it.padEnd(16) }
            eastLines = east.handDiagram().split("\n")
        } else if (W) { // && !E
            westLines = west.handDiagram().split("\n")
            eastLines = List(4) { "" }
        } else if (E) { // && !W
            westLines = List(4) { EIGHT_SPACES }
            eastLines = east.handDiagram().split("\n")
        } else {
            westLines = List(4) { "" }
            eastLines = List(4) { "" }
        }

        repeat(4) {
            builder.append(westLines[it])
            builder.append(eastLines[it])
            builder.append('\n')
        }
        if (S) appendNS(south)
        return builder.toString()
    }
}

class Hand internal constructor(
    private val dealCards: ByteArray,
    direction: Direction,
) : AbstractList<SuitHolding>() {
    private val offset = direction.encoded * 13

    private val suits = SUITS.mapToArray { SuitHolding(dealCards, offset) }
    val spades: SuitHolding get() = suits[0]
    val hearts: SuitHolding get() = suits[1]
    val diamonds: SuitHolding get() = suits[2]
    val clubs: SuitHolding get() = suits[3]

    private var shapeInit = false
    private var l1234Init = false

    private val _shape = IntArray(4) { 0 }
    val shape: ReadOnlyIntArray
        get() {
            if (!shapeInit) {
                repeat(4) {
                    _shape[it] = suits[it].size
                }
                shapeInit = true
            }
            return ReadOnlyIntArray(_shape)
        }

    private val _shapeSorted = IntArray(4) { 0 }
    private fun getL(i: Int): Int {
        if (!l1234Init) {
            shape.wrapped.copyInto(_shapeSorted)
            _shapeSorted.sort()
            l1234Init = true
        }
        return _shapeSorted[4 - i]
    }

    private var _shapeFlattened = -1
    internal val shapeFlattened: Int
        get() {
            if (_shapeFlattened == -1) {
                val shapeArray = shape.wrapped
                _shapeFlattened = flatten(shapeArray[0], shapeArray[1], shapeArray[2])
            }
            return _shapeFlattened
        }

    /**
     * Length of the longest suit.
     */
    val l1: Int get() = getL(1)
    val l2: Int get() = getL(2)
    val l3: Int get() = getL(3)

    /**
     * Length of the shortest suit.
     */
    val l4: Int get() = getL(4)

    internal fun sortCards() {
        val cards = dealCards
        val toIndex = offset + 13

        // a special case of counting sort where the count is at most 1
        var bits = HoldingBySuit(0)
        for (it in offset until toIndex) {
            bits = bits.withCard(cards[it])
        }
        fromBitVector(bits)
    }

    internal fun fromBitVector(bits: HoldingBySuit) {
        bits.forEachIndexed(
            startIndex = offset,
            afterEachSuit = { index, suit ->
                if (suit != 3) {
                    suits[suit].end = index
                    suits[suit + 1].start = index
                }
            }
        ) { index, card ->
            dealCards[index] = card
        }
    }

    internal fun init() {
        // the sum of their bit sizes is exactly 32
        // is it meant to be?

        // reset lazy values
        shapeInit = false
        l1234Init = false
        _hcp = -1
        _qp = -1
        _controls = -1
        _freakness = -1
        _shapeFlattened = -1

        suits.forEach { it.reset() }
    }

    override val size: Int get() = 4
    override operator fun get(index: Int): SuitHolding = suits[index]
    operator fun get(suit: Suit): SuitHolding = get(suit.encoded)

    fun getCard(index: Int): Card {
        Objects.checkIndex(index, 13)
        return Card(dealCards[offset + index])
    }

    operator fun contains(card: Card): Boolean = dealCards.contains(card.encoded, start = offset, end = offset + 13)

    private var _hcp: Int = -1
    val hcp: Int
        get() {
            if (_hcp == -1) {
                _hcp = sumOf(start = offset, end = offset + 13) { Card(dealCards[it]).hcp }
            }
            return _hcp
        }

    private var _qp: Int = -1
    val qp: Int
        get() {
            if (_qp == -1) {
                _qp = sumOf(start = offset, end = offset + 13) { Card(dealCards[it]).qp }
            }
            return _qp
        }

    private var _controls: Int = -1
    val controls: Int
        get() {
            if (_controls == -1) {
                _controls = sumOf(start = offset, end = offset + 13) { Card(dealCards[it]).controls }
            }
            return _controls
        }

    private var _freakness: Int = -1

    /**
     * [Freakness](http://www.rpbridge.net/8j17.htm#7)
     */
    val freakness: Int
        get() {
            if (_freakness == -1) {
                val shapeArray = shape.wrapped
                _freakness = sumOf(end = 4) { index ->
                    val l = shapeArray[index]
                    max(l - 4, 3 - l) + (2 - l).coerceAtLeast(0)
                }
            }
            return _freakness
        }

    /**
     * Sum of the four [SuitHolding.playingTricks]
     */
    val playingTricks: Double get() = sumOf(end = 4) { suits[it].playingTricks }
    val losers: Double get() = sumOf(end = 4) { suits[it].losers }
    val newLtc: Double get() = sumOf(end = 4) { suits[it].newLtc }

    override fun toString(): String = "♠\uFE0F${get(S)}♥\uFE0F${get(H)}♦\uFE0F${get(D)}♣\uFE0F${get(C)}"
    fun handDiagram(): String = "♠\uFE0F${get(S)}\n♥\uFE0F${get(H)}\n♦\uFE0F${get(D)}\n♣\uFE0F${get(C)}"
}

class SuitHolding internal constructor(private val dealCards: ByteArray, handOffset: Int) : RankList() {
    internal var start = handOffset
    internal var end = handOffset + 13

    override val size: Int get() = end - start
    override operator fun get(index: Int): Rank = getCard(index).rank

    fun getCard(index: Int): Card {
        Objects.checkIndex(index, size)
        return Card(dealCards[start + index])
    }

    operator fun contains(card: Card): Boolean =
        dealCards.contains(card.encoded, start, end)

    internal fun reset() {
        _losers = -1.0
        _playingTricks = -1.0
        _newLtc = -1.0
    }

    val hcp: Int
        get() = sumOf(start = start, end = end.coerceAtMost(start + 4)) { Card(dealCards[it]).hcp }
    val qp: Int
        get() = sumOf(start = start, end = end.coerceAtMost(start + 3)) { Card(dealCards[it]).qp }
    val controls: Int
        get() = sumOf(start = start, end = end.coerceAtMost(start + 2)) { Card(dealCards[it]).controls }

    private var _losers: Double = -1.0
    val losers: Double
        get() {
            if (_losers == -1.0) {
                var topCardIndex = 0
                var halfLosers = 0
                repeat(3.coerceAtMost(size)) { i ->
                    if (this[topCardIndex].encoded != Rank.A.encoded - i) {
                        halfLosers += 2
                    } else if (i != 2) {
                        topCardIndex++
                    } else { // it's the queen iteration
                        if (topCardIndex == 0 && this[1] < Rank.T) {
                            halfLosers++
                        }
                    }
                }
                _losers = halfLosers / 2.0
            }
            return _losers
        }

    private var _newLtc: Double = -1.0
    val newLtc: Double
        get() {
            if (_newLtc == -1.0) {
                var topCardIndex = 0
                var halfLosers = 0
                repeat(3.coerceAtMost(size)) { i ->
                    if (this[topCardIndex].encoded != Rank.A.encoded - i) {
                        halfLosers += 3 - i
                    } else {
                        topCardIndex++
                    }
                }
                _newLtc = halfLosers / 2.0
            }
            return _newLtc
        }

    private var _playingTricks: Double = -1.0

    /**
     * [Pavlicek playing tricks](http://www.rpbridge.net/8j17.htm#4).
     *
     * Differences:
     * 1. Stiff K is 0 tricks instead of 0.5 tricks
     * 2. Kxx is 0.5 tricks instead of 1 trick
     * 3. Qx is 0 tricks instead of 0.5 tricks
     */
    val playingTricks: Double
        get() {
            if (_playingTricks == -1.0) {
                val extraLength = (size - 3).coerceAtLeast(0)
                val top3 = sumOf(end = size - extraLength) { i ->
                    getCard(i).rankEncoded shl (8 - 4 * i)
                }

                // A K Q J T 9 8 7
                // e d c b a 9 8 7
                _playingTricks = extraLength + when {
                    top3 == 0xedc -> 3.0 // AKQ
                    top3 == 0xedb || top3 == 0xecb -> 2.5 // AKJ AQJ
                    top3 >= 0xed0 || top3 == 0xeca || top3 == 0xdcb -> 2.0 // AK(x) AQT KQJ
                    top3 >= 0xeb0 -> 1.5 // A(Q/J)(x)
                    top3 >= 0xe00 -> 1.0 // A(xx)
                    top3 == 0xdba || top3 >= 0xdc2 -> 1.5 //  KJT KQx
                    top3 >= 0xdb0 -> 1.0 // K(Q/J)(x)
                    top3 > 0xd00 -> 0.5 // if (top3 and 0xf == 0) 0.5 else 1.0 // Kx(x)
                    top3 == 0xd00 -> 0.0 // stiff K
                    top3 >= 0xcb2 -> 1.0 // QJx
                    top3 >= 0xc20 -> if (top3 and 0xf == 0) 0.0 else 0.5 // 0.5 // Qx(x)
                    top3 == 0xc00 -> 0.0 // stiff Q
                    top3 >= 0xba2 -> 0.5 // JTx
                    else -> 0.0
                }
            }
            return _playingTricks
        }

    override fun toString(): String = StringBuilder(size).also { sb ->
        repeat(size) { sb.append(this[it].toChar()) }
    }.toString()
}
