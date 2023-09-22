package com.github.phisgr.dds;

import kotlin.NotImplementedError;

import java.lang.foreign.MemorySegment;

public class Suit extends Strain {
    public static class Array {
        public MemorySegment getMemory() {
            throw new NotImplementedError();
        }

        public Suit get(int index) {
            throw new NotImplementedError();
        }

        public void set(int index, Suit value) {
            throw new NotImplementedError();
        }

        public void clear() {
            throw new NotImplementedError();
        }
    }
}
