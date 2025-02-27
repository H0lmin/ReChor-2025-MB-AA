package ch.epfl.rechor.timetable.mapped;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Example tests for the StructuredBuffer class.
 */
public class MyStructuredBufferTest {

    // A helper method to create a simple Structure with 3 fields:
    //  - field 0 -> U8
    //  - field 1 -> U16
    //  - field 2 -> S32
    // totalSize = 1 + 2 + 4 = 7 bytes
    private static Structure createTestStructure() {
        return new Structure(
                new Structure.Field(0, Structure.FieldType.U8),
                new Structure.Field(1, Structure.FieldType.U16),
                new Structure.Field(2, Structure.FieldType.S32)
        );
    }

    // ----------------------------------------------------------------------
    // 1) Constructor tests
    // ----------------------------------------------------------------------

    @Test
    @DisplayName("Constructor succeeds when buffer size is a multiple of structure's totalSize")
    void testConstructorValid() {
        Structure structure = createTestStructure();  // totalSize = 7
        // We create a ByteBuffer of size 14 => 2 elements
        ByteBuffer buffer = ByteBuffer.allocate(14);

        // Should not throw
        StructuredBuffer sb = new StructuredBuffer(structure, buffer);
        assertNotNull(sb);
    }

    @Test
    @DisplayName("Constructor throws IllegalArgumentException when buffer size is not a multiple of structure's totalSize")
    void testConstructorInvalidSize() {
        Structure structure = createTestStructure();  // totalSize = 7
        // Buffer of size 13 => not multiple of 7
        ByteBuffer buffer = ByteBuffer.allocate(13);

        assertThrows(IllegalArgumentException.class, () -> new StructuredBuffer(structure, buffer));
    }

    @Test
    @DisplayName("Constructor handles zero-sized buffer with a non-zero structure totalSize")
    void testConstructorZeroBufferNonZeroStructure() {
        Structure structure = createTestStructure(); // totalSize=7
        ByteBuffer buffer = ByteBuffer.allocate(0);  // capacity=0

        // 0 is not a multiple of 7 (besides 0) => we expect an exception
        // or if your spec allows it to be a multiple (0 * 7 = 0),
        // decide how you want to handle that. Usually we'd expect 0 / 7 = 0 elements.
        // The question is if you consider 0 a valid multiple of 7 or not.
        // Below, we assume it is valid mathematically (0 mod 7 = 0).
        // So if you want to allow that, remove the exception check.
        // Otherwise, keep the check:
        assertDoesNotThrow(() -> {
            new StructuredBuffer(structure, buffer);
        });
    }

    // ----------------------------------------------------------------------
    // 2) size() tests
    // ----------------------------------------------------------------------

    @Test
    @DisplayName("size() returns the correct number of elements")
    void testSize() {
        Structure structure = createTestStructure(); // totalSize=7
        // 14 / 7 = 2 elements
        ByteBuffer buffer = ByteBuffer.allocate(14);
        StructuredBuffer sb = new StructuredBuffer(structure, buffer);

        assertEquals(2, sb.size());
    }

    @Test
    @DisplayName("size() returns 0 if the buffer capacity is 0 and totalSize is non-zero (if allowed)")
    void testSizeZeroCapacity() {
        Structure structure = createTestStructure(); // totalSize=7
        ByteBuffer buffer = ByteBuffer.allocate(0);  // capacity=0

        // If we allow 0 as a multiple of 7 => we get 0 elements
        StructuredBuffer sb = new StructuredBuffer(structure, buffer);
        assertEquals(0, sb.size());
    }

    // ----------------------------------------------------------------------
    // 3) getU8, getU16, getS32 tests
    // ----------------------------------------------------------------------
    //
    // We'll create a buffer with 2 elements, each of size 7 bytes:
    // Element 0:
    //   field 0 (U8)  -> 0x12 (decimal 18)
    //   field 1 (U16) -> 0x3456 (decimal 13398)
    //   field 2 (S32) -> 0x78 9A BC DE (hex) => 0x789ABCDE => 2023406814 in decimal
    // Element 1:
    //   field 0 (U8)  -> 0xFF (decimal 255)
    //   field 1 (U16) -> 0xFFFF (decimal 65535)
    //   field 2 (S32) -> 0xFFFF0000 (hex) => -65536 in decimal (signed)

    @Test
    @DisplayName("getU8, getU16, getS32 return correct values for known data")
    void testGetMethodsValid() {
        Structure structure = createTestStructure(); // totalSize=7

        // Build the byte array of size 14 => 2 elements
        // Make sure to cast to (byte) for values > 127 so we don't get sign warnings
        byte[] data = {
                // Element 0
                0x12,
                0x34, 0x56,
                0x78, (byte)0x9A, (byte)0xBC, (byte)0xDE,

                // Element 1
                (byte)0xFF,
                (byte)0xFF, (byte)0xFF,
                (byte)0xFF, (byte)0xFF, 0x00, 0x00
        };
        ByteBuffer buffer = ByteBuffer.wrap(data);
        StructuredBuffer sb = new StructuredBuffer(structure, buffer);

        // Element count
        assertEquals(2, sb.size());

        // --- Element 0 ---
        assertEquals(0x12, sb.getU8(0, 0));    // 18
        assertEquals(0x3456, sb.getU16(1, 0)); // 13398
        assertEquals(0x789ABCDE, sb.getS32(2, 0)); // 2023406814

        // --- Element 1 ---
        assertEquals(0xFF, sb.getU8(0, 1));        // 255
        assertEquals(0xFFFF, sb.getU16(1, 1));     // 65535
        assertEquals(0xFFFF0000, sb.getS32(2, 1)); // -65536 in signed 32-bit
    }

    @Test
    @DisplayName("getU8 throws IndexOutOfBoundsException for invalid fieldIndex or elementIndex")
    void testGetU8IndexOutOfBounds() {
        Structure structure = createTestStructure(); // totalSize=7
        ByteBuffer buffer = ByteBuffer.allocate(14); // 2 elements
        StructuredBuffer sb = new StructuredBuffer(structure, buffer);

        // Valid field indices: 0..2
        // Valid element indices: 0..1
        // Let's test invalid fieldIndex
        assertThrows(IndexOutOfBoundsException.class, () -> sb.getU8(-1, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> sb.getU8(3, 0));

        // Let's test invalid elementIndex
        assertThrows(IndexOutOfBoundsException.class, () -> sb.getU8(0, -1));
        assertThrows(IndexOutOfBoundsException.class, () -> sb.getU8(0, 2));
    }

    @Test
    @DisplayName("getU16 throws IndexOutOfBoundsException for invalid fieldIndex or elementIndex")
    void testGetU16IndexOutOfBounds() {
        Structure structure = createTestStructure();
        ByteBuffer buffer = ByteBuffer.allocate(14); // 2 elements
        StructuredBuffer sb = new StructuredBuffer(structure, buffer);

        // fieldIndex out of range
        assertThrows(IndexOutOfBoundsException.class, () -> sb.getU16(-1, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> sb.getU16(3, 0));

        // elementIndex out of range
        assertThrows(IndexOutOfBoundsException.class, () -> sb.getU16(1, -1));
        assertThrows(IndexOutOfBoundsException.class, () -> sb.getU16(1, 2));
    }

    @Test
    @DisplayName("getS32 throws IndexOutOfBoundsException for invalid fieldIndex or elementIndex")
    void testGetS32IndexOutOfBounds() {
        Structure structure = createTestStructure();
        ByteBuffer buffer = ByteBuffer.allocate(14); // 2 elements
        StructuredBuffer sb = new StructuredBuffer(structure, buffer);

        // fieldIndex out of range
        assertThrows(IndexOutOfBoundsException.class, () -> sb.getS32(-1, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> sb.getS32(3, 0));

        // elementIndex out of range
        assertThrows(IndexOutOfBoundsException.class, () -> sb.getS32(2, -1));
        assertThrows(IndexOutOfBoundsException.class, () -> sb.getS32(2, 2));
    }
}
