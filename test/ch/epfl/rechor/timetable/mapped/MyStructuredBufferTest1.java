package ch.epfl.rechor.timetable.mapped;

import org.junit.jupiter.api.Test;
import java.nio.ByteBuffer;
import static ch.epfl.rechor.timetable.mapped.Structure.field;

import static org.junit.jupiter.api.Assertions.*;

public class MyStructuredBufferTest1 {
    @Test
    void constructorThrowsWhenIllegalArgument() {
        assertThrows(IllegalArgumentException.class, () -> {
            byte[] bite = {00, 10, 32, 99, 78};
            ByteBuffer buffer = ByteBuffer.wrap(bite);
            Structure structure = new Structure(field(0, Structure.FieldType.U16));
            StructuredBuffer s = new StructuredBuffer(structure, buffer);
        });
        assertThrows(IllegalArgumentException.class, () -> {
            byte[] bite = {00, 10, 32, 99, 78, 84, 23, 45, 76, 99, 12, 34};
            ByteBuffer buffer = ByteBuffer.wrap(bite);
            Structure structure = new Structure(field(0, Structure.FieldType.S32), field(1, Structure.FieldType.S32));
            StructuredBuffer s = new StructuredBuffer(structure, buffer);
        });
        assertDoesNotThrow(() -> {
            byte[] bite = {00, 10, 32, 99, 78, 84, 23, 45, 76, 99, 12, 34};
            ByteBuffer buffer = ByteBuffer.wrap(bite);
            Structure structure = new Structure(field(0, Structure.FieldType.U16), field(1, Structure.FieldType.S32));
            StructuredBuffer s = new StructuredBuffer(structure, buffer);
        });
        assertThrows(IllegalArgumentException.class, () -> {
            byte[] bite = {00, 10, 32, 99, 78, 00, 10};
            ByteBuffer buffer = ByteBuffer.wrap(bite);
            Structure structure = new Structure(field(0, Structure.FieldType.U8), field(1, Structure.FieldType.U8));
            StructuredBuffer s = new StructuredBuffer(structure, buffer);
        });
        assertDoesNotThrow(() -> {
            byte[] bite = {00, 10, 32, 99, 78, 00};
            ByteBuffer buffer = ByteBuffer.wrap(bite);
            Structure structure = new Structure(field(0, Structure.FieldType.U8), field(1, Structure.FieldType.U8));
            StructuredBuffer s = new StructuredBuffer(structure, buffer);
        });
    }

    @Test
    void sizeWorks() {
        byte[] bite = {00, 10, 32, 99, 78, 00, 10, 12};
        ByteBuffer buffer = ByteBuffer.wrap(bite);
        Structure structure = new Structure(field(0, Structure.FieldType.U8), field(1, Structure.FieldType.U8));
        StructuredBuffer s = new StructuredBuffer(structure, buffer);
        assertEquals(4, s.size());
        byte[] bite2 = {00, 10, 32, 99, 78, 84, 23, 45, 76, 99, 12, 34};
        ByteBuffer buffer2 = ByteBuffer.wrap(bite2);
        Structure structure2 = new Structure(field(0, Structure.FieldType.U16), field(1, Structure.FieldType.S32));
        StructuredBuffer s2 = new StructuredBuffer(structure2, buffer2);
        assertEquals(2, s2.size());
    }

    @Test
    void getU8Works() {
        byte[] bite = {00, 10, 32, 99, 78, 00, 10, -128, 127};
        ByteBuffer buffer = ByteBuffer.wrap(bite);
        Structure structure = new Structure(field(0, Structure.FieldType.U8), field(1, Structure.FieldType.U16));
        StructuredBuffer s = new StructuredBuffer(structure, buffer);
        assertEquals(10, s.getU8(0, 2));
    }

    @Test
    void getU16Works() {
        byte[] bite = {-127, -127, 127, 0, 4, 3, 92, 89, 19, 1, 92, 45, 67, 83, 74, 95, 29, 0b01011101, (byte) 0b11110100, 34, -23, -34, 34, -100};
        ByteBuffer buffer = ByteBuffer.wrap(bite);
        Structure structure = new Structure(field(0, Structure.FieldType.U8), field(1, Structure.FieldType.U16), field(2, Structure.FieldType.U8));
        StructuredBuffer s = new StructuredBuffer(structure, buffer);
        assertEquals(0b01011101_11110100, s.getU16(1, 4));
    }

    @Test
    void getS32Works() {
        byte[] bite = {00, 10, 32, 99, 78, 00, 10, -128, 127, 00, 12, (byte) 0b11111100, (byte) 0b01110011, (byte) 0b01110001, (byte) 0b11111111};
        ByteBuffer buffer = ByteBuffer.wrap(bite);
        Structure structure = new Structure(field(0, Structure.FieldType.U8), field(1, Structure.FieldType.S32));
        StructuredBuffer s = new StructuredBuffer(structure, buffer);
        assertEquals(0b11111100_01110011_01110001_11111111, s.getS32(1, 2));
    }

    @Test
    void getThrowsWhenIndexOutOfBounds() {
        byte[] bite = {00, 10, 32, 99, 78, 00, 10, -128, 127, 2, 1, 0};
        ByteBuffer buffer = ByteBuffer.wrap(bite);
        Structure structure = new Structure(field(0, Structure.FieldType.U8), field(1, Structure.FieldType.U16));
        StructuredBuffer s = new StructuredBuffer(structure, buffer);
        assertDoesNotThrow(() -> {
            s.getS32(1, 2);
        });
        assertThrows(IndexOutOfBoundsException.class, () -> {
            s.getS32(2, 2);
        });
        assertThrows(IndexOutOfBoundsException.class, () -> {
            s.getU8(3, 2);
        });
    }
}

