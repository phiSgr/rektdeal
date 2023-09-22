package com.github.phisgr.dds;

public final class Direction extends Seats {
    Direction(int encoded, String shortName) {
        super(encoded, shortName);
    }

    public static Direction fromChar(char c) {
        return switch (c) {
            case 'N' -> Direction.N;
            case 'E' -> Direction.E;
            case 'S' -> Direction.S;
            case 'W' -> Direction.W;
            default -> throw new IllegalStateException("Unexpected value: " + c);
        };
    }

    public static Direction fromInt(int i) {
        return DIRECTIONS.get(i);
    }

    public Direction next() {
        return DIRECTIONS.get((getEncoded() + 1) % 4);
    }
}
