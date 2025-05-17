package example;

import com.github.phisgr.dds.*;
import dds.dealPBN;
import dds.futureTricks;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.Map;

import static com.github.phisgr.dds.Dds.solveBoard;
import static com.github.phisgr.dds.Dds.solveBoardPBN;
import static dds.Dds.SolveBoardPBN;
import static example.Dds4jKt.dds4jKotlin;

public class Dds4j {

    public static void main(String[] args) {
        dds4j();
        System.out.println();

        dds4jPBN();
        System.out.println();

        rawDds();
        System.out.println();

        dds4jKotlin();
    }

    public static void rawDds() {
        Arena arena = Arena.global();
        MemorySegment deal = dealPBN.allocate(arena);
        dealPBN.trump(deal, 0);
        dealPBN.first(deal, 0);
        dealPBN.currentTrickSuit(deal).fill((byte) 0);
        dealPBN.currentTrickRank(deal).fill((byte) 0);
        dealPBN.remainCards(deal)
                .setString(0, "N:QJ6.K652.J85.T98 873.J97.AT764.Q4 K5.T83.KQ9.A7652 AT942.AQ4.32.KJ3");

        MemorySegment ftMemory = futureTricks.allocate(arena);
        int returnCode = SolveBoardPBN(deal, -1, 1, 1, ftMemory, 0);
        if (returnCode != 1) throw new IllegalArgumentException("Got return code " + returnCode);

        int trickCount = 13 - futureTricks.score(ftMemory, 0);

        System.out.println("The double dummy tricks for declarer is " + trickCount);
    }

    public static void dds4jPBN() {
        Arena arena = Arena.global();
        DealPBN deal = new DealPBN(arena);
        deal.setTrump(Suit.S);
        deal.setFirst(Direction.N);
        deal.getCurrentTrickSuit().clear();
        deal.getCurrentTrickRank().clear();
        deal.setRemainCards("N:QJ6.K652.J85.T98 873.J97.AT764.Q4 K5.T83.KQ9.A7652 AT942.AQ4.32.KJ3");

        FutureTricks futureTricks = new FutureTricks(arena);
        solveBoardPBN(deal, -1, 1, 1, futureTricks, 0);

        System.out.println(futureTricks);
        // TODO: change it back to string interpolation when possible
        System.out.println("The double dummy tricks for declarer is " + (13 - futureTricks.getScore().get(0)));
    }

    public static void dds4j() {
        Arena arena = Arena.global();
        Deal deal = new Deal(arena);
        deal.setTrump(Suit.S);
        deal.setFirst(Direction.N);
        deal.getCurrentTrickSuit().clear();
        deal.getCurrentTrickRank().clear();
        Cards cards = deal.getRemainCards();

        Map<Direction, Map<Suit, Integer>> holdings = Map.of(
                Direction.N, Map.of(
                        Suit.S, Holding.parse("QJ6"),
                        Suit.H, Holding.parse("K652"),
                        Suit.D, Holding.parse("J85"),
                        Suit.C, Holding.parse("T98")
                ),
                Direction.E, Map.of(
                        Suit.S, Holding.parse("873"),
                        Suit.H, Holding.parse("J97"),
                        Suit.D, Holding.parse("AT764"),
                        Suit.C, Holding.parse("Q4")
                ),
                Direction.S, Map.of(
                        Suit.S, Holding.parse("K5"),
                        Suit.H, Holding.parse("T83"),
                        Suit.D, Holding.parse("KQ9"),
                        Suit.C, Holding.parse("A7652")
                ),
                Direction.W, Map.of(
                        // you can also use Holding.fromRanks to encode the cards into an int
                        Suit.S, Holding.fromRanks(14, 10, 9, 4, 2),
                        Suit.H, Holding.fromRanks(14, 12, 4),
                        Suit.D, Holding.fromRanks(3, 2),
                        Suit.C, Holding.fromRanks(13, 11, 3)
                )
        );

        for (var direction : Direction.DIRECTIONS) {
            for (var suit : Suit.SUITS) {
                int holding = holdings.get(direction).get(suit);
                cards.set(direction, suit, holding);
            }
        }

        FutureTricks futureTricks = new FutureTricks(arena);
        solveBoard(deal, -1, 1, 1, futureTricks, 0);

        System.out.println(futureTricks);
        System.out.println("The double dummy tricks for declarer is " + (13 - futureTricks.getScore().get(0)));
    }
}
