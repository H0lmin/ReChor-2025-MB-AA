package ch.epfl.rechor;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

public class MyPackedRangeTest {

    /**
     * Tests a typical interval. For example, the interval [1234, 1278)
     * has a length of 44. This test verifies that pack produces the expected
     * packed value and that the startInclusive, length, and endExclusive methods
     * correctly extract the values.
     */
    @Test
    public void testNormalInterval() {
        int start = 1234;
        int end = 1278; // length = 44
        int packed = PackedRange.pack(start, end);

        assertEquals(start, PackedRange.startInclusive(packed),
                "startInclusive should match the original start");
        assertEquals(end - start, PackedRange.length(packed),
                "Length should match (end - start)");
        assertEquals(end, PackedRange.endExclusive(packed),
                "endExclusive should equal start + length");
    }

    /**
     * Tests the edge case of a zero-length interval [0, 0).
     * In this case, all components should be 0.
     */
    @Test
    public void testZeroLengthInterval() {
        int start = 0;
        int end = 0;
        int packed = PackedRange.pack(start, end);

        assertEquals(0, packed, "Packing [0, 0) should yield 0");
        assertEquals(0, PackedRange.startInclusive(packed), "startInclusive should be 0");
        assertEquals(0, PackedRange.length(packed), "Length should be 0");
        assertEquals(0, PackedRange.endExclusive(packed), "endExclusive should be 0");
    }

    /**
     * Tests the edge case using the maximum valid values.
     * The maximum start that fits in 24 bits is 0xFFFFFF (16,777,215) and
     * the maximum length that fits in 8 bits is 0xFF (255).
     */
    @Test
    public void testMaximumValidInterval() {
        int start = 0xFFFFFF;  // 16,777,215
        int length = 0xFF;     // 255
        int end = start + length;
        int packed = PackedRange.pack(start, end);

        assertEquals(start, PackedRange.startInclusive(packed),
                "Maximum startInclusive should be extracted correctly");
        assertEquals(length, PackedRange.length(packed),
                "Maximum length should be extracted correctly");
        assertEquals(end, PackedRange.endExclusive(packed),
                "endExclusive should equal start + length");
    }

    /**
     * Tests that an invalid start (one that cannot be represented in 24 bits)
     * causes an exception. For example, a start of 0x1000000 (2^24) exceeds 24 bits.
     */
    @Test
    public void testInvalidStart() {
        int invalidStart = 0x1000000; // 16,777,216: one more than max valid
        int end = invalidStart + 10;  // arbitrary length
        assertThrows(IllegalArgumentException.class, () -> {
            PackedRange.pack(invalidStart, end);
        }, "A startInclusive exceeding 24 bits should throw an exception");
    }

    /**
     * Tests that an interval with a length that is too large (i.e. 256 or more)
     * throws an exception. The length must fit in 8 bits (0 to 255).
     */
    @Test
    public void testInvalidLength() {
        int start = 100;
        int invalidEnd = start + 256; // length 256 does not fit in 8 bits
        assertThrows(IllegalArgumentException.class, () -> {
            PackedRange.pack(start, invalidEnd);
        }, "A length of 256 should throw an exception as it doesn't fit in 8 bits");
    }

    /**
     * Tests that providing an interval where the end is less than the start
     * (resulting in a negative length) causes an exception.
     */
    @Test
    public void testNegativeLength() {
        int start = 200;
        int end = 150;  // negative length
        assertThrows(IllegalArgumentException.class, () -> {
            PackedRange.pack(start, end);
        }, "A negative interval (end < start) should throw an exception");
    }
}

