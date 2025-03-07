package ch.epfl.rechor.timetable.mapped;

import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

public class MyBufferedPlatformsTest1 {
    static List<String> exampleTest() {
        List<String> stringTable = new ArrayList<>();
        stringTable.add("1");
        stringTable.add("70");
        stringTable.add("Anet");
        stringTable.add("Ins");
        stringTable.add("Lausanne");
        stringTable.add("Losanna");
        stringTable.add("Palézieux");
        return stringTable;
    }

    @Test
    void trivialMethodWorks() {
        byte[] bite = {00, 00, 00, 00, 00, 01, 00, 00, 00, 00, 00, 01};
        BufferedPlatforms bf = new BufferedPlatforms(exampleTest(), ByteBuffer.wrap(bite));
        assertEquals("1", bf.name(0));
        assertEquals("70", bf.name(1));
        assertEquals("1", bf.name(2));
        assertEquals(0, bf.stationId(0));
        assertEquals(0, bf.stationId(1));
        assertEquals(1, bf.stationId(2));
        assertEquals(3, bf.size());
    }

    @Test
    void stationIdWorks() {
        byte[] bite = {10, 03, 00, 02, 20, 20, 0b01010101, (byte) 0b11001100, 50, 60, (byte) 0b10010010, 0b011111};
        BufferedPlatforms bf = new BufferedPlatforms(exampleTest(), ByteBuffer.wrap(bite));
        assertEquals(2, bf.stationId(0));
        assertEquals(0b01010101_11001100, bf.stationId(1));
        assertEquals(0b10010010_00011111, bf.stationId(2));
    }

    @Test
    void nameWorks() {
        byte[] bite = {00, 05, 00, 00, 00, 06, 00, 00, 00, 03, 00, 01};
        BufferedPlatforms bf = new BufferedPlatforms(exampleTest(), ByteBuffer.wrap(bite));
        assertEquals("Losanna", bf.name(0));
        assertEquals("Palézieux", bf.name(1));
        assertEquals("Ins", bf.name(2));
    }

    @Test
    void sizeWorks() {
        byte[] bite = {12, 32, 45, 65, 65, 34, 87, 1, 23, -21, -127, 123, 64, 98, 90, 75, 93, 84, 92, 93, 20, 04, 02, 39, 48, 90, 02, -12};
        BufferedPlatforms bf = new BufferedPlatforms(exampleTest(), ByteBuffer.wrap(bite));
        assertEquals(7, bf.size());
    }
}

