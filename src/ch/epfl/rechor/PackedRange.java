package ch.epfl.rechor;

import static ch.epfl.rechor.Preconditions.checkArgument;

/**
 * Utility class for packing and unpacking integer ranges into a single integer.
 */
public class PackedRange {
    private PackedRange() {}

    /**
     * Packs a start and end value into a single integer.
     */
    public static int pack(int startInclusive, int endExclusive) {
        checkArgument((startInclusive >>> 24) == 0 && (endExclusive - startInclusive) >>> 8 == 0);
        return Bits32_24_8.pack(startInclusive, endExclusive - startInclusive);
    }

    /**
     * Returns the length of the range.
     */
    public static int length(int interval) {
        return interval & 0xFF;
    }

    /**
     * Returns the start value of the range.
     */
    public static int startInclusive(int interval) {
        return interval >>> 8;
    }

    /**
     * Returns the end value of the range.
     */
    public static int endExclusive(int interval) {
        return startInclusive(interval) + length(interval);
    }
}
