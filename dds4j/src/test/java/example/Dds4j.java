package example;

import com.github.phisgr.dds.*;
import dds.dealPBN;
import dds.futureTricks;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.util.Map;

import static com.github.phisgr.dds.Dds.solveBoard;
import static com.github.phisgr.dds.Dds.solveBoardPBN;
import static dds.Dds.SolveBoardPBN;
import static example.Dds4jKt.dds4jKotlin;

public class Dds4j {

    void main() {
        dds4j();
        System.out.println();

        dds4jPBN();
        System.out.println();

        rawDds();
        System.out.println();

        dds4jKotlin();
    }

    void rawDds() {
        Arena arena = Arena.global();
        MemorySegment deal = dealPBN.allocate(arena);
        dealPBN.trump$set(deal, 0);
        dealPBN.first$set(deal, 0);
        dealPBN.currentTrickSuit$slice(deal).fill((byte) 0);
        dealPBN.currentTrickRank$slice(deal).fill((byte) 0);
        dealPBN.remainCards$slice(deal)
                .setUtf8String(0, "N:QJ6.K652.J85.T98 873.J97.AT764.Q4 K5.T83.KQ9.A7652 AT942.AQ4.32.KJ3");

        MemorySegment ftMemory = futureTricks.allocate(arena);
        int returnCode = SolveBoardPBN(deal, -1, 1, 1, ftMemory, 0);
        if (returnCode != 1) throw new IllegalArgumentException(STR."Got return code \{returnCode}");

        int trickCount = 13 - futureTricks.score$slice(ftMemory).get(ValueLayout.JAVA_INT, 0);

        System.out.println(STR."The double dummy tricks for declarer is \{trickCount}");
    }

    void dds4jPBN() {
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
        System.out.println(STR."The double dummy tricks for declarer is \{13 - futureTricks.getScore().get(0)}");
    }

    void dds4j() {
        Arena arena = Arena.global();
        Deal deal = new Deal(arena);
        deal.setTrump(Suit.S);
        deal.setFirst(Direction.N);
        deal.getCurrentTrickSuit().clear();
        deal.getCurrentTrickRank().clear();
        Cards cards = deal.getRemainCards();

        Map<Direction, Map<Suit, String>> holdings = Map.of(
                Direction.N, Map.of(
                        Suit.S, "QJ6",
                        Suit.H, "K652",
                        Suit.D, "J85",
                        Suit.C, "T98"
                ),
                Direction.E, Map.of(
                        Suit.S, "873",
                        Suit.H, "J97",
                        Suit.D, "AT764",
                        Suit.C, "Q4"
                ),
                Direction.S, Map.of(
                        Suit.S, "K5",
                        Suit.H, "T83",
                        Suit.D, "KQ9",
                        Suit.C, "A7652"
                ),
                Direction.W, Map.of(
                        Suit.S, "AT942",
                        Suit.H, "AQ4",
                        Suit.D, "32",
                        Suit.C, "KJ3"
                )
        );

        for (var direction : Direction.DIRECTIONS) {
            for (var suit : Suit.SUITS) {
                String holdingString = holdings.get(direction).get(suit);
                cards.set(direction, suit, Holding.parse(holdingString));
            }
        }

        FutureTricks futureTricks = new FutureTricks(arena);
        solveBoard(deal, -1, 1, 1, futureTricks, 0);

        System.out.println(futureTricks);
        System.out.println(STR."The double dummy tricks for declarer is \{13 - futureTricks.getScore().get(0)}");
    }
}
