package ch.epfl.rechor;

import static ch.epfl.rechor.Preconditions.checkArgument;

/**
 * Utility class for packing and unpacking 32-bit integers into 24-bit and 8-bit parts.
 */
public class Bits32_24_8 {
    private Bits32_24_8() {}

    /**
     * Packs 24-bit and 8-bit values into a 32-bit integer.
     */
    public static int pack(int bits24, int bits8) {
        checkArgument((bits24 >> 24) == 0 && (bits8 >> 8) == 0);
        return (bits24 << 8) | bits8;
    }

    /**
     * Unpacks the 24-bit part from a 32-bit integer.
     */
    public static int unpack24(int bits32) {
        return bits32 >>> 8;
    }

    /**
     * Unpacks the 8-bit part from a 32-bit integer.
     */
    public static int unpack8(int bits32) {
        return bits32 & 0xFF;
    }
}
