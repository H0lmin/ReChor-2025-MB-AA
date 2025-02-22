package ch.epfl.rechor;

public class PackedRange {
    private PackedRange() {
    }

    public static int pack(int startInclusive, int endExclusive) {
        int length = endExclusive - startInclusive;

        if ((startInclusive & 0xFF000000) != 0) {
            throw new IllegalArgumentException("The lower bound must be represented using 24 bits.");
        } else if (length < 0 || length > 255) {
            throw new IllegalArgumentException("The length must be between 0 and 255.");
        }

        return (startInclusive << 8) | length;
    }

    public static int length(int interval) {
        return interval & 0xFF;
    }

    public static int startInclusive(int interval) {
        return (interval >>> 8) & 0xFFFFFF;
    }

    public static int endExclusive(int interval) {
        return startInclusive(interval) + length(interval);
    }
}
