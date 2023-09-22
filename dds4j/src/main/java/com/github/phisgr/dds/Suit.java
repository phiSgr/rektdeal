package com.github.phisgr.dds;

import java.lang.foreign.MemorySegment;

public final class Suit extends Strain {
    Suit(int encoded, String shortName) {
        super(encoded, shortName);
    }

    public static Suit fromChar(char c) {
        return switch (c) {
            case 'S' -> Strain.S;
            case 'H' -> Strain.H;
            case 'D' -> Strain.D;
            case 'C' -> Strain.C;
            default -> throw new IllegalStateException("Unexpected value: " + c);
        };
    }

    public static Suit fromInt(int i) {
        return SUITS.get(i);
    }

    // not making it a record
    // so that it looks the same as other nested classes also named `Array`
    public static final class Array {
        private final MemorySegment memory;

        public Array(MemorySegment memory) {
            this.memory = memory;
        }

        public MemorySegment getMemory() {
            return memory;
        }

        public Suit get(int index) {
            return SUITS.get(InternalUtilKt.getInt(memory, index));
        }

        public void set(int index, Suit value) {
            InternalUtilKt.setInt(memory, index, value.getEncoded());
        }

        public int size() {
            return InternalUtilKt.getIntSize(memory);
        }

        public String toString(int count) {
            var buffer = new StringBuilder(3 * count);
            buffer.append("[");
            for (int i = 0; i < count; i++) {
                buffer.append(get(i));
                buffer.append(i == count - 1 ? "]" : ", ");
            }
            return buffer.toString();
        }

        @Override
        public String toString() {
            return toString(size());
        }

        public void clear() {
            memory.fill((byte) 0);
        }
    }
}
