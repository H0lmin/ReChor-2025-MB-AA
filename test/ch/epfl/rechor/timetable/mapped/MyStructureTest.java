package ch.epfl.rechor.timetable.mapped;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class MyStructureTest {

    // ----------------------------------------------------------------------
    // 1) Tests for the Field record
    // ----------------------------------------------------------------------

    @Test
    @DisplayName("Field constructor succeeds with valid index and non-null type")
    void testFieldConstructorValid() {
        Structure.Field f = new Structure.Field(0, Structure.FieldType.U8);
        assertEquals(0, f.index());
        assertEquals(Structure.FieldType.U8, f.type());
    }

    @Test
    @DisplayName("Field constructor throws NullPointerException if type is null")
    void testFieldConstructorNullType() {
        // index can be anything, the type is null => must throw NPE
        assertThrows(NullPointerException.class, () -> new Structure.Field(0, null));
    }

    // ----------------------------------------------------------------------
    // 2) Tests for the Structure constructor
    // ----------------------------------------------------------------------

    @Test
    @DisplayName("Structure constructor accepts empty field array (edge case)")
    void testStructureConstructorEmptyFields() {
        // Depending on your interpretation/spec, this might be valid or invalid.
        // Below we assume it is valid and simply yields a structure of size 0.
        Structure structure = new Structure();
        assertEquals(0, structure.totalSize());
        // offset(...) will fail for any fieldIndex, but that's expected.
    }

    @Test
    @DisplayName("Structure constructor accepts a correct ascending sequence of fields")
    void testStructureConstructorValidFields() {
        // Suppose we define 3 fields: 0->U8 (1 byte), 1->U16 (2 bytes), 2->S32 (4 bytes).
        Structure.Field f0 = new Structure.Field(0, Structure.FieldType.U8);
        Structure.Field f1 = new Structure.Field(1, Structure.FieldType.U16);
        Structure.Field f2 = new Structure.Field(2, Structure.FieldType.S32);

        Structure structure = new Structure(f0, f1, f2);

        // The total size = 1 (U8) + 2 (U16) + 4 (S32) = 7 bytes
        assertEquals(7, structure.totalSize());
    }

    @Test
    @DisplayName("Structure constructor throws IllegalArgumentException if first field is not index=0")
    void testStructureConstructorFirstFieldNot0() {
        // e.g. pass a single field with index=1
        Structure.Field f = new Structure.Field(1, Structure.FieldType.U8);
        assertThrows(IllegalArgumentException.class, () -> new Structure(f));
    }

    @Test
    @DisplayName("Structure constructor throws IllegalArgumentException if fields are out of order")
    void testStructureConstructorOutOfOrder() {
        // Indices must be 0,1,2,... in ascending order
        Structure.Field f0 = new Structure.Field(0, Structure.FieldType.U8);
        Structure.Field f2 = new Structure.Field(2, Structure.FieldType.U16);
        // The second field is index=2, so index=1 is missing => out of order
        assertThrows(IllegalArgumentException.class, () -> new Structure(f0, f2));
    }

    @Test
    @DisplayName("Structure constructor throws IllegalArgumentException if a field index is duplicated")
    void testStructureConstructorRepeatedIndex() {
        // Indices 0 and 0 repeated
        Structure.Field f0a = new Structure.Field(0, Structure.FieldType.U8);
        Structure.Field f0b = new Structure.Field(0, Structure.FieldType.U16);

        assertThrows(IllegalArgumentException.class, () -> new Structure(f0a, f0b));
    }

    @Test
    @DisplayName("Structure constructor throws IllegalArgumentException if a middle index is missing")
    void testStructureConstructorMissingIndex() {
        // We have fields for index=0,1,3 => index=2 is missing => should fail
        Structure.Field f0 = new Structure.Field(0, Structure.FieldType.U8);
        Structure.Field f1 = new Structure.Field(1, Structure.FieldType.U8);
        Structure.Field f3 = new Structure.Field(3, Structure.FieldType.U8);
        assertThrows(IllegalArgumentException.class, () -> new Structure(f0, f1, f3));
    }

    // ----------------------------------------------------------------------
    // 3) Tests for totalSize()
    // ----------------------------------------------------------------------

    @Test
    @DisplayName("totalSize() with mixed field types is correct")
    void testTotalSizeWithMixedFieldTypes() {
        // [0 -> U8, 1 -> U16, 2 -> U8, 3 -> S32]
        // Sizes: 1 + 2 + 1 + 4 = 8
        Structure.Field f0 = new Structure.Field(0, Structure.FieldType.U8);
        Structure.Field f1 = new Structure.Field(1, Structure.FieldType.U16);
        Structure.Field f2 = new Structure.Field(2, Structure.FieldType.U8);
        Structure.Field f3 = new Structure.Field(3, Structure.FieldType.S32);

        Structure structure = new Structure(f0, f1, f2, f3);
        assertEquals(8, structure.totalSize());
    }

    @Test
    @DisplayName("totalSize() is 0 for an empty structure (if allowed)")
    void testTotalSizeEmptyStructure() {
        Structure structure = new Structure();
        assertEquals(0, structure.totalSize());
    }

    // ----------------------------------------------------------------------
    // 4) Tests for offset(fieldIndex, elementIndex)
    // ----------------------------------------------------------------------

    @Test
    @DisplayName("offset() returns the correct offset for each field in a single 'element'")
    void testOffsetSingleElement() {
        // For [0->U8, 1->U16, 2->S32], the offsets within a single element are:
        //   field 0: offset = 0
        //   field 1: offset = 1  (because U8 is 1 byte)
        //   field 2: offset = 3  (1 + 2 for U16 = 3)
        // totalSize = 7
        Structure.Field f0 = new Structure.Field(0, Structure.FieldType.U8);
        Structure.Field f1 = new Structure.Field(1, Structure.FieldType.U16);
        Structure.Field f2 = new Structure.Field(2, Structure.FieldType.S32);
        Structure s = new Structure(f0, f1, f2);

        assertEquals(0, s.offset(0, 0));
        assertEquals(1, s.offset(1, 0));
        assertEquals(3, s.offset(2, 0));
    }

    @Test
    @DisplayName("offset() returns correct offset for multiple elements")
    void testOffsetMultipleElements() {
        // Using the same structure [0->U8, 1->U16, 2->S32], totalSize=7.
        // For elementIndex e, offset(fieldIndex, e) = offset(fieldIndex,0) + e*7
        Structure.Field f0 = new Structure.Field(0, Structure.FieldType.U8);
        Structure.Field f1 = new Structure.Field(1, Structure.FieldType.U16);
        Structure.Field f2 = new Structure.Field(2, Structure.FieldType.S32);
        Structure s = new Structure(f0, f1, f2);

        // field 0 offset(0, e) = 0 + e*7
        assertEquals(0, s.offset(0, 0));
        assertEquals(7, s.offset(0, 1));
        assertEquals(14, s.offset(0, 2));

        // field 2 offset(2, e) = 3 + e*7
        // e.g. for e=3 => offset(2, 3) = 3 + 3*7 = 24
        assertEquals(3,  s.offset(2, 0));
        assertEquals(10, s.offset(2, 1));
        assertEquals(17, s.offset(2, 2));
        assertEquals(24, s.offset(2, 3));
    }

    @Test
    @DisplayName("offset() throws IndexOutOfBoundsException for invalid field index")
    void testOffsetInvalidFieldIndex() {
        Structure.Field f0 = new Structure.Field(0, Structure.FieldType.U8);
        Structure.Field f1 = new Structure.Field(1, Structure.FieldType.U8);
        Structure s = new Structure(f0, f1);

        // We have valid field indices 0 and 1 only
        assertThrows(IndexOutOfBoundsException.class, () -> s.offset(-1, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> s.offset(2, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> s.offset(99, 0));
    }
}

