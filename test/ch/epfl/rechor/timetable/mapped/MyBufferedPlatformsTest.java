package ch.epfl.rechor.timetable.mapped;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Example tests for the BufferedPlatforms class.
 * Adapt method/field names to your actual code if needed.
 */
public class MyBufferedPlatformsTest {

    // ----------------------------------------------------------------------
    // 1) Basic constructor tests
    // ----------------------------------------------------------------------

    @Test
    @DisplayName("Constructor succeeds with valid data (example from §2.6.3)")
    void testConstructorValid() {
        // Example from §2.6.3 => 3 entries => 3 * 4 = 12 bytes
        // Byte sequence: 00 00 00 00 00 01 00 00 00 00 00 01
        // We'll create a ByteBuffer with these 12 bytes.

        byte[] platformBytes = new byte[] {
                0x00, 0x00,  0x00, 0x00,
                0x00, 0x01,  0x00, 0x00,
                0x00, 0x00,  0x00, 0x01
        };
        ByteBuffer buffer = ByteBuffer.wrap(platformBytes);

        // Minimal string table for indices 0..1 (since NAME_ID references string table)
        //   index 0 => "1"
        //   index 1 => "70"
        // (In the example, 0 corresponds to "1", 1 corresponds to "70", etc.)
        List<String> stringTable = Arrays.asList("1", "70", "Anet", "Ins", "Lausanne", "Losanna", "Palézieux");

        BufferedPlatforms platforms = new BufferedPlatforms(stringTable, buffer);
        assertNotNull(platforms);
        // We expect 3 platforms
        assertEquals(3, platforms.size());
    }

    @Test
    @DisplayName("Constructor throws IllegalArgumentException if buffer size is not multiple of 4 bytes")
    void testConstructorInvalidBufferSize() {
        // Each platform is 4 bytes => if buffer size is not multiple of 4 => fail
        byte[] invalidBytes = new byte[6]; // 6 is not multiple of 4
        ByteBuffer buffer = ByteBuffer.wrap(invalidBytes);

        List<String> stringTable = List.of("A", "B");

        assertThrows(IllegalArgumentException.class, () ->
                new BufferedPlatforms(stringTable, buffer)
        );
    }

    @Test
    @DisplayName("Constructor with empty buffer => zero platforms (if allowed)")
    void testConstructorEmptyBuffer() {
        // 0 is a multiple of 4 => means 0 platforms
        ByteBuffer buffer = ByteBuffer.allocate(0);
        List<String> stringTable = List.of("A", "B");

        BufferedPlatforms platforms = new BufferedPlatforms(stringTable, buffer);
        assertEquals(0, platforms.size());
    }

    @Test
    @DisplayName("Constructor throws if stringTable is null (optional)")
    void testConstructorNullStringTable() {
        ByteBuffer buffer = ByteBuffer.allocate(4); // enough for 1 platform
        assertThrows(NullPointerException.class, () ->
                new BufferedPlatforms(null, buffer)
        );
    }

    @Test
    @DisplayName("Constructor throws if buffer is null (optional)")
    void testConstructorNullBuffer() {
        List<String> stringTable = List.of("A", "B");
        assertThrows(NullPointerException.class, () ->
                new BufferedPlatforms(stringTable, null)
        );
    }

    // ----------------------------------------------------------------------
    // 2) Tests reading platform data
    // ----------------------------------------------------------------------

    @Test
    @DisplayName("platformCount() returns correct number of platforms")
    void testPlatformCount() {
        // Create a buffer for 2 platforms => 2 * 4 = 8 bytes
        ByteBuffer buffer = ByteBuffer.allocate(8);
        List<String> table = List.of("X", "Y");
        BufferedPlatforms platforms = new BufferedPlatforms(table, buffer);
        assertEquals(2, platforms.size());
    }

    @Test
    @DisplayName("Reading name/station ID from example data (3 platforms)")
    void testReadPlatformDataExample() {
        // The example from §2.6.3 => 3 entries => 12 bytes
        byte[] platformBytes = new byte[] {
                // Platform 0 => NAME_ID=0, STATION_ID=0
                0x00, 0x00,  0x00, 0x00,
                // Platform 1 => NAME_ID=1, STATION_ID=0
                0x00, 0x01,  0x00, 0x00,
                // Platform 2 => NAME_ID=0, STATION_ID=1
                0x00, 0x00,  0x00, 0x01
        };
        ByteBuffer buffer = ByteBuffer.wrap(platformBytes);

        // stringTable => index 0 => "1", index 1 => "70"
        List<String> stringTable = Arrays.asList("1", "70", "Anet", "Ins", "Lausanne", "Losanna", "Palézieux");

        BufferedPlatforms platforms = new BufferedPlatforms(stringTable, buffer);
        assertEquals(3, platforms.size());

        // Check each platform's name (NAME_ID => stringTable) and station ID (raw int).
        // Station ID references the station table, so it's just an integer here.
        // If your design fetches the station name directly, adapt accordingly.

        // Platform 0 => name = "1", station ID = 0
        assertEquals("1", platforms.name(0));
        assertEquals(0,  platforms.stationId(0));

        // Platform 1 => name = "70", station ID = 0
        assertEquals("70", platforms.name(1));
        assertEquals(0,   platforms.stationId(1));

        // Platform 2 => name = "1", station ID = 1
        assertEquals("1", platforms.name(2));
        assertEquals(1,   platforms.stationId(2));
    }

    // ----------------------------------------------------------------------
    // 3) Tests for invalid platform indices
    // ----------------------------------------------------------------------

    @Test
    @DisplayName("Calling platformName/parentStationIndex with invalid index => IndexOutOfBoundsException")
    void testInvalidPlatformIndex() {
        // 2 platforms => valid indices 0..1
        ByteBuffer buffer = ByteBuffer.allocate(8);
        List<String> table = List.of("X", "Y", "Z");
        BufferedPlatforms platforms = new BufferedPlatforms(table, buffer);

        // invalid indices
        assertThrows(IndexOutOfBoundsException.class, () -> platforms.name(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> platforms.name(2));
        assertThrows(IndexOutOfBoundsException.class, () -> platforms.stationId(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> platforms.stationId(2));
    }

    // ----------------------------------------------------------------------
    // 4) Tests for name resolution from the string table
    // ----------------------------------------------------------------------

    @Test
    @DisplayName("Resolves correct platform name from string table for each entry")
    void testPlatformNameResolution() {
        // 2 platforms, each 4 bytes
        // Platform 0 => NAME_ID=2, STATION_ID=0
        // Platform 1 => NAME_ID=0, STATION_ID=1
        byte[] data = new byte[] {
                0x00, 0x02,  0x00, 0x00,
                0x00, 0x00,  0x00, 0x01
        };
        ByteBuffer buffer = ByteBuffer.wrap(data);

        // stringTable => index 0 => "Zero", 1 => "One", 2 => "Two"
        List<String> stringTable = Arrays.asList("Zero", "One", "Two");

        BufferedPlatforms platforms = new BufferedPlatforms(stringTable, buffer);

        assertEquals(2, platforms.size());
        // platform 0 => name = "Two", station ID = 0
        assertEquals("Two", platforms.name(0));
        assertEquals(0,     platforms.stationId(0));

        // platform 1 => name = "Zero", station ID = 1
        assertEquals("Zero", platforms.name(1));
        assertEquals(1,      platforms.stationId(1));
    }

}

