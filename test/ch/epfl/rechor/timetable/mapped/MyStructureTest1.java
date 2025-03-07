package ch.epfl.rechor.timetable.mapped;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ch.epfl.rechor.timetable.mapped.Structure.Field;
import static ch.epfl.rechor.timetable.mapped.Structure.field;
import static org.junit.jupiter.api.Assertions.*;

public class MyStructureTest1 {
    @Test
    void structureThrowsWhenIllegalArgument() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Structure structure = new Structure(field(1, Structure.FieldType.S32));
        });
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Structure structure = new Structure(field(0, Structure.FieldType.S32), field(2, Structure.FieldType.S32));
        });
        Assertions.assertDoesNotThrow(() -> {
            Structure structure = new Structure(field(0, Structure.FieldType.S32), field(1, Structure.FieldType.S32), field(2, Structure.FieldType.S32));
        });
        Assertions.assertDoesNotThrow(() -> {
            Structure structure = new Structure();
        });
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Structure structure = new Structure(field(-1, Structure.FieldType.S32));
        });
    }

    @Test
    void totalSizeWorks() {
        Structure structure0 = new Structure(field(0, Structure.FieldType.S32));
        assertEquals(4, structure0.totalSize());
        Structure structure1 = new Structure(field(0, Structure.FieldType.U8), field(1, Structure.FieldType.U16));
        assertEquals(3, structure1.totalSize());
        Structure structure2 = new Structure(field(0, Structure.FieldType.S32), field(1, Structure.FieldType.U8), field(2, Structure.FieldType.U16), field(3, Structure.FieldType.S32), field(4, Structure.FieldType.U16));
        assertEquals(13, structure2.totalSize());
        Structure structure3 = new Structure();
        assertEquals(0, structure3.totalSize());
    }

    @Test
    void offsetWorks() {
        Structure structure0 = new Structure(field(0, Structure.FieldType.U16), field(1, Structure.FieldType.U16));
        assertEquals(6, structure0.offset(1, 1));
        assertEquals(4, structure0.offset(0, 1));
        Structure structure1 = new Structure(field(0, Structure.FieldType.S32));
        assertEquals(0, structure1.offset(0, 0));
        assertEquals(12, structure1.offset(0, 3));
        Structure structure2 = new Structure(field(0, Structure.FieldType.S32), field(1, Structure.FieldType.U8), field(2, Structure.FieldType.U16), field(3, Structure.FieldType.S32), field(4, Structure.FieldType.U16));
        assertEquals(17, structure2.offset(1, 1));
        assertEquals(31, structure2.offset(2, 2));
        assertEquals(271, structure2.offset(4, 20));
    }

    @Test
    void offsetThrowsWhenIndexOutOfBounds() {
        Structure structure0 = new Structure(field(0, Structure.FieldType.U16), field(1, Structure.FieldType.U16));
        Structure structure1 = new Structure(field(0, Structure.FieldType.S32));
        Structure structure2 = new Structure(field(0, Structure.FieldType.S32), field(1, Structure.FieldType.U8), field(2, Structure.FieldType.U16), field(3, Structure.FieldType.S32), field(4, Structure.FieldType.U16));
        assertThrows(IndexOutOfBoundsException.class, () -> {
            structure0.offset(2, 80);
        });
        assertThrows(IndexOutOfBoundsException.class, () -> {
            structure1.offset(1, 80);
        });
        assertThrows(IndexOutOfBoundsException.class, () -> {
            structure2.offset(5, 80);
        });
        assertDoesNotThrow(() -> {
            structure0.offset(0, 80);
        });
        assertDoesNotThrow(() -> {
            structure2.offset(4, 80);
        });
    }

    @Test
    void fieldThrowsWhenNullArgument() {
        assertThrows(NullPointerException.class, () -> {
            Field field = new Field(96, null);
        });
        assertThrows(NullPointerException.class, () -> {
            field(96, null);
        });
    }
}

