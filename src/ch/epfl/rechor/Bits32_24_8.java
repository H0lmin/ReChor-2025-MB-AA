package ch.epfl.rechor;

    public class Bits32_24_8 {
    private Bits32_24_8() {
    }

    public static int pack(int bits24, int bits8) {
        if ((bits24 & 0xFF000000) != 0) {
            throw new IllegalArgumentException("The lower bound must be represented using 24 bits.");
        }
        if ((bits8 & 0xFFFFFF00) != 0) {
            throw new IllegalArgumentException("The lower bound must have 8 bits.");
        }
        return (bits24 << 8) | bits8;
    }

    public static int unpack24(int bits32) {
        return (bits32 >>> 8) & 0xFFFFFF;
    }

    public static int unpack8(int bits32) {
        return bits32 & 0xFF;
    }
}
