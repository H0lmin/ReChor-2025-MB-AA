package ch.epfl.rechor;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

public class MyBits32_24_8Test {

    /**
     * Tests that packing valid values returns a combined integer
     * and that unpacking it returns the original values.
     */
    @Test
    public void testPackAndUnpack_NormalValues() {
        // Test a few sample valid pairs
        int[][] testCases = {
                {0x000000, 0x00},  // both zeros
                {0x000001, 0x01},  // minimal nonzero values
                {0x123456, 0xAB},  // arbitrary values: expect 0x123456AB
                {0xFFFFFF, 0xFF}   // maximum valid values for 24 and 8 bits
        };

        for (int[] testCase : testCases) {
            int bits24 = testCase[0];
            int bits8 = testCase[1];
            int packed = Bits32_24_8.pack(bits24, bits8);
            // Expected: bits24 shifted left 8 OR bits8.
            int expected = (bits24 << 8) | bits8;
            assertEquals(expected, packed,
                    "Packed value should equal bits24<<8 OR bits8");
            assertEquals(bits24, Bits32_24_8.unpack24(packed),
                    "Unpacked 24 bits should equal the original bits24");
            assertEquals(bits8, Bits32_24_8.unpack8(packed),
                    "Unpacked 8 bits should equal the original bits8");
        }
    }

    /**
     * Tests that pack throws an IllegalArgumentException when bits24
     * does not fit within 24 bits.
     */
    @Test
    public void testPack_InvalidBits24() {
        // Value that uses a bit in the top 8 bits.
        int invalidBits24 = 0x01000000; // equals 1 << 24
        int validBits8 = 0x00;
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            Bits32_24_8.pack(invalidBits24, validBits8);
        });
        assertTrue(exception.getMessage().contains("24 bits"),
                "Exception message should indicate bits24 must be represented using 24 bits");
    }

    /**
     * Tests that pack throws an IllegalArgumentException when bits8
     * does not fit within 8 bits.
     */
    @Test
    public void testPack_InvalidBits8() {
        int validBits24 = 0x000123; // some valid 24-bit number
        int invalidBits8 = 0x100; // 256, which does not fit in 8 bits
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            Bits32_24_8.pack(validBits24, invalidBits8);
        });
        assertTrue(exception.getMessage().contains("8 bits"),
                "Exception message should indicate bits8 must have 8 bits");
    }

    /**
     * Tests edge cases using the minimum and maximum valid values.
     */
    @Test
    public void testPack_EdgeCases() {
        // Minimum edge: both parts are 0.
        int packedZero = Bits32_24_8.pack(0, 0);
        assertEquals(0, packedZero, "Packing (0,0) should yield 0");
        assertEquals(0, Bits32_24_8.unpack24(packedZero), "Unpacking 24 bits from 0 should be 0");
        assertEquals(0, Bits32_24_8.unpack8(packedZero), "Unpacking 8 bits from 0 should be 0");

        // Maximum edge: bits24 = 0xFFFFFF, bits8 = 0xFF.
        int packedMax = Bits32_24_8.pack(0xFFFFFF, 0xFF);
        int expectedMax = (0xFFFFFF << 8) | 0xFF;
        assertEquals(expectedMax, packedMax, "Packed maximum value incorrect");
        assertEquals(0xFFFFFF, Bits32_24_8.unpack24(packedMax), "Unpacked 24 bits should be 0xFFFFFF");
        assertEquals(0xFF, Bits32_24_8.unpack8(packedMax), "Unpacked 8 bits should be 0xFF");
    }

    /**
     * Tests that negative values are rejected.
     * Negative numbers have their high bits set, so they should trigger exceptions.
     */
    @Test
    public void testPack_NegativeValues() {
        int negativeBits24 = -1; // in two's complement, this has all bits set.
        int validBits8 = 0x00;
        assertThrows(IllegalArgumentException.class, () -> {
            Bits32_24_8.pack(negativeBits24, validBits8);
        }, "Negative bits24 should not be allowed");

        int validBits24 = 0x000123;
        int negativeBits8 = -1; // likewise, not allowed.
        assertThrows(IllegalArgumentException.class, () -> {
            Bits32_24_8.pack(validBits24, negativeBits8);
        }, "Negative bits8 should not be allowed");
    }
}

