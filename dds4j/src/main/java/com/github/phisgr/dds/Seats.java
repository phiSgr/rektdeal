package com.github.phisgr.dds;

import java.util.List;

/**
 * {@link Direction} (N, E, S, W), or NS, EW.
 * Used in par score to show who declares.
 */
public sealed class Seats permits Direction {
    private final int encoded;
    private final String shortName;

    public int getEncoded() {
        return encoded;
    }

    @Override
    public String toString() {
        return shortName;
    }

    Seats(int encoded, String shortName) {
        this.encoded = encoded;
        this.shortName = shortName;
    }


    public static Seats fromInt(int i) {
        return SEATS.get(i);
    }

    public static final Seats NS = new Seats(4, "NS");
    public static final Seats EW = new Seats(5, "EW");
    public static final Direction N = new Direction(0, "N");
    public static final Direction E = new Direction(1, "E");
    public static final Direction S = new Direction(2, "S");
    public static final Direction W = new Direction(3, "W");

    public static final List<Direction> DIRECTIONS = List.of(N, E, S, W);
    public static final List<Seats> SEATS = List.of(N, E, S, W, NS, EW);
}
