package ch.epfl.rechor.timetable.mapped;

import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class MyBufferedStationsTest1 {
    @Test
    void BufferStationsWorkClassicExemple(){
        byte[] bite = {(byte) 0x00 , (byte) 0x04 , (byte) 0x04 , (byte) 0xb6 , (byte) 0xca , (byte) 0x14 , (byte) 0x21 , (byte) 0x14 ,
                (byte) 0x1f , (byte) 0xa1 , (byte) 0x00 , (byte) 0x06 , (byte) 0x04 , (byte)0xdc , (byte)0xcc, (byte) 0x12,
                (byte) 0x21, (byte) 0x18, (byte)0xda, (byte) 0x03};
        ByteBuffer buffer = ByteBuffer.allocate(bite.length);
        buffer.put(bite);
        buffer.flip();
        List<String> stringTable = new ArrayList<>();
        stringTable.add("1");
        stringTable.add("70");
        stringTable.add("Anet");
        stringTable.add("Ins");
        stringTable.add("Lausanne");
        stringTable.add("Losanna");
        stringTable.add("Palézieux");
        BufferedStations exemple = new BufferedStations(stringTable ,  buffer);
        assertEquals("Lausanne" , exemple.name(0));
        assertEquals("Palézieux" , exemple.name(1));
        assertEquals(((79088148)/Math.pow(2,32))*360 , exemple.longitude(0));
        assertEquals(((554966945)/Math.pow(2,32))*360 , exemple.latitude(0));

        assertEquals(((81579026)/Math.pow(2,32))*360 , exemple.longitude(1));
        assertEquals(((555276803)/Math.pow(2,32))*360 , exemple.latitude(1));

        assertEquals(2 , exemple.size());
    }

    @Test
    void BufferStationsWorkClassicExemple2(){
        HexFormat hexFormat = HexFormat.ofDelimiter(" ");
        byte[] bytes = hexFormat.parseHex("00 03 22 C4 D0 D6 01 AC 0F F7 00 04 24 97 B8 52 FF F1 A7 D7");
        ByteBuffer buffer = ByteBuffer.allocate(bytes.length);
        buffer.put(bytes);
        buffer.flip();
        List<String> stringTable = new ArrayList<>();
        stringTable.add("1");
        stringTable.add("70");
        stringTable.add("Anet");
        stringTable.add("Paris");
        stringTable.add("Londres");
        stringTable.add("pk tu lis le test fait confiance");
        stringTable.add("Palézieux");
        BufferedStations exemple = new BufferedStations(stringTable ,  buffer);
        assertEquals("Paris" , exemple.name(0));
        assertEquals("Londres" , exemple.name(1));
        assertEquals(((583323862)/Math.pow(2,32))*360 , exemple.longitude(0));
        assertEquals(((28053495)/Math.pow(2,32))*360 , exemple.latitude(0));
        assertEquals(((613922898)/Math.pow(2,32))*360 , exemple.longitude(1));
        assertEquals(((-940073)/Math.pow(2,32))*360 , exemple.latitude(1));
    }
}

