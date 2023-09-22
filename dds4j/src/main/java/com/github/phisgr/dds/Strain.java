package com.github.phisgr.dds;

import java.util.List;

/**
 * Also known as {@link ContractType#getDenom() denomination}.
 */
public sealed class Strain permits Suit {
    private final int encoded;
    private final String shortName;

    public int getEncoded() {
        return encoded;
    }

    @Override
    public String toString() {
        return shortName;
    }

    Strain(int encoded, String shortName) {
        this.encoded = encoded;
        this.shortName = shortName;
    }

    public static final Strain N = new Strain(4, "N");
    public static final Suit S = new Suit(0, "S");
    public static final Suit H = new Suit(1, "H");
    public static final Suit D = new Suit(2, "D");
    public static final Suit C = new Suit(3, "C");

    public static Strain fromChar(char c) {
        if (c == 'N') {
            return N;
        } else {
            return Suit.fromChar(c);
        }
    }

    public static Strain fromInt(int i) {
        return STRAINS.get(i);
    }

    public static final List<Strain> STRAINS = List.of(S, H, D, C, N);
    public static final List<Suit> SUITS = List.of(S, H, D, C);
}
