package ch.epfl.rechor.timetable.mapped;

import org.junit.jupiter.api.Test;
import org.w3c.dom.DOMStringList;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class MyBufferedStationsAliasesTest1 {
    static List<String> stringTable() {
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

    static List<String> stringTable2() {
        List<String> stringTable = new ArrayList<>();
        stringTable.add("0");
        stringTable.add("1");
        stringTable.add("2");
        stringTable.add("3");
        stringTable.add("4");
        stringTable.add("5");
        stringTable.add("6");
        return stringTable;
    }

    @Test
    public void trivialMethodWorks() {
        HexFormat hexFormat = HexFormat.ofDelimiter(" ");
        byte[] bytes = hexFormat.parseHex("00 05 00 04 00 02 00 03");
        ByteBuffer buffer = ByteBuffer.allocate(bytes.length);
        buffer.put(bytes);
        buffer.flip();
        BufferedStationAliases exemple = new BufferedStationAliases(stringTable() ,  buffer);
        assertEquals("Lausanne" , exemple.stationName(0));
        assertEquals("Ins" , exemple.stationName(1));
        assertEquals("Losanna" , exemple.alias(0));
        assertEquals("Anet" , exemple.alias(1));
        assertEquals(2 , exemple.size());
    }

    @Test
    void aliasWorks() {
        byte[] bite = {00, 01, 20, 30, 00, 04, 00, 70, 00, 06, 100, 110};
        BufferedStationAliases exemple = new BufferedStationAliases(stringTable2(), ByteBuffer.wrap(bite));
        assertEquals("1" , exemple.alias(0));
        assertEquals("4" , exemple.alias(1));
        assertEquals("6" , exemple.alias(2));
    }

    @Test
    void stationNameWorks() {
        byte[] bite = {10, 20, 00, 02, 30, 40, 00, 06, 50, 60, 00, 01};
        BufferedStationAliases exemple = new BufferedStationAliases(stringTable2(), ByteBuffer.wrap(bite));
        assertEquals("2" , exemple.stationName(0));
        assertEquals("6" , exemple.stationName(1));
        assertEquals("1" , exemple.stationName(2));
    }

    @Test
    void sizeWorks() {
        byte[] bite = {00, 01, 99, 99, 00, 02, 99, 99, 00, 03, 99, 99, 00, 04, 99, 99, 00, 05, 99, 99};
        byte[] bite2 = {00, 01, 99, 99, 00, 02, 99, 99};
        BufferedStationAliases exemple = new BufferedStationAliases(stringTable(), ByteBuffer.wrap(bite));
        BufferedStationAliases exemple2 = new BufferedStationAliases(stringTable2(), ByteBuffer.wrap(bite2));
        assertEquals(5, exemple.size());
        assertEquals(2, exemple2.size());
    }

}

