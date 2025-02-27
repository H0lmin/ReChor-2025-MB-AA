package ch.epfl.rechor.timetable.mapped;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Example tests for the BufferedStations class.
 * Adapt method/field names to your actual code if needed.
 */
public class MyBufferedStationsTest {

    // ----------------------------------------------------------------------
    // 1) Basic constructor tests
    // ----------------------------------------------------------------------

    @Test
    @DisplayName("Constructor succeeds with valid data (example from §2.6.1)")
    void testConstructorValid() {
        // Example data from the specification §2.6.1:
        // 2 stations, 10 bytes each => 20 bytes total
        //   Station 0 => name index = 4 => "Lausanne"
        //                LON = 0x04 b6 ca 14 => ~ 6.629092
        //                LAT = 0x21 14 1f a1 => ~ 46.516792
        //   Station 1 => name index = 6 => "Palézieux"
        //                LON = 0x04 dc cc 12 => ~ 6.837875
        //                LAT = 0x21 18 da 03 => ~ 46.542764
        //
        // Hex string: 00 04 04 b6 ca 14 21 14 1f a1 00 06 04 dc cc 12 21 18 da 03
        // We'll place them into a ByteBuffer.

        byte[] stationBytes = new byte[] {
                0x00, 0x04,  0x04, (byte)0xb6, (byte)0xca, 0x14,  0x21, 0x14, 0x1f, (byte)0xa1,
                0x00, 0x06,  0x04, (byte)0xdc, (byte)0xcc, 0x12,  0x21, 0x18, (byte)0xda, 0x03
        };
        ByteBuffer buffer = ByteBuffer.wrap(stationBytes);

        // Minimal string table for indices 0..6:
        //  index 4 => "Lausanne"
        //  index 6 => "Palézieux"
        // We'll just fill with placeholders for 0..3,5
        List<String> stringTable = Arrays.asList(
                "1", "70", "Anet", "Ins", "Lausanne", "Losanna", "Palézieux"
        );

        BufferedStations stations = new BufferedStations(stringTable, buffer);
        assertNotNull(stations);
        // We expect 2 stations
        assertEquals(2, stations.size());
    }

    @Test
    @DisplayName("Constructor throws IllegalArgumentException if buffer size is not multiple of 10 bytes")
    void testConstructorInvalidBufferSize() {
        // 10 bytes per station => if not multiple of 10 => fail
        byte[] invalidBytes = new byte[15]; // not multiple of 10
        ByteBuffer buffer = ByteBuffer.wrap(invalidBytes);

        List<String> stringTable = List.of("A", "B", "C");

        assertThrows(IllegalArgumentException.class, () ->
                new BufferedStations(stringTable, buffer)
        );
    }

    @Test
    @DisplayName("Constructor with empty buffer => zero stations (if allowed)")
    void testConstructorEmptyBuffer() {
        // 0 is a multiple of 10 => means 0 stations
        ByteBuffer buffer = ByteBuffer.allocate(0);
        List<String> stringTable = List.of("A", "B", "C");

        BufferedStations stations = new BufferedStations(stringTable, buffer);
        assertEquals(0, stations.size());
    }

    @Test
    @DisplayName("Constructor throws if stringTable is null (optional)")
    void testConstructorNullStringTable() {
        ByteBuffer buffer = ByteBuffer.allocate(10); // enough for 1 station
        assertThrows(NullPointerException.class, () ->
                new BufferedStations(null, buffer)
        );
    }

    @Test
    @DisplayName("Constructor throws if buffer is null (optional)")
    void testConstructorNullBuffer() {
        List<String> stringTable = List.of("A", "B", "C");
        assertThrows(NullPointerException.class, () ->
                new BufferedStations(stringTable, null)
        );
    }

    // ----------------------------------------------------------------------
    // 2) Tests reading station data
    // ----------------------------------------------------------------------

    @Test
    @DisplayName("stationCount() returns correct number of stations")
    void testStationCount() {
        // Create a buffer for 3 stations => 3 * 10 = 30 bytes
        ByteBuffer buffer = ByteBuffer.allocate(30);
        List<String> table = List.of("A", "B");
        BufferedStations stations = new BufferedStations(table, buffer);
        assertEquals(3, stations.size());
    }

    @Test
    @DisplayName("Reading name, longitude, latitude from example data")
    void testReadStationDataExample() {
        // Reuse the example data from §2.6.1 for 2 stations
        byte[] stationBytes = new byte[] {
                0x00, 0x04,  0x04, (byte)0xb6, (byte)0xca, 0x14,  0x21, 0x14, 0x1f, (byte)0xa1,
                0x00, 0x06,  0x04, (byte)0xdc, (byte)0xcc, 0x12,  0x21, 0x18, (byte)0xda, 0x03
        };
        ByteBuffer buffer = ByteBuffer.wrap(stationBytes);

        // Indices 4 => "Lausanne", 6 => "Palézieux"
        // Fill placeholders for other indices
        List<String> stringTable = Arrays.asList(
                "1", "70", "Anet", "Ins", "Lausanne", "Losanna", "Palézieux"
        );

        BufferedStations stations = new BufferedStations(stringTable, buffer);
        assertEquals(2, stations.size());

        // Station 0
        assertEquals("Lausanne", stations.name(0), "Name of station 0");
        double lon0 = stations.longitude(0);
        double lat0 = stations.latitude(0);

        // The example in the doc says ~6.629092 / 46.516792
        // We'll allow a small epsilon for floating comparison
        assertEquals(6.629092, lon0, 1e-6, "Longitude of station 0");
        assertEquals(46.516792, lat0, 1e-6, "Latitude of station 0");

        // Station 1
        assertEquals("Palézieux", stations.name(1), "Name of station 1");
        double lon1 = stations.longitude(1);
        double lat1 = stations.latitude(1);

        // The example in the doc says ~6.837875 / 46.542764
        assertEquals(6.837875, lon1, 1e-6, "Longitude of station 1");
        assertEquals(46.542764, lat1, 1e-6, "Latitude of station 1");
    }

    // ----------------------------------------------------------------------
    // 3) Tests for invalid station indices
    // ----------------------------------------------------------------------

    @Test
    @DisplayName("Calling stationName/Longitude/Latitude with invalid index => IndexOutOfBoundsException")
    void testInvalidStationIndex() {
        // 2 stations
        ByteBuffer buffer = ByteBuffer.allocate(20);
        List<String> table = List.of("A", "B", "C", "D", "E", "F", "G");
        BufferedStations stations = new BufferedStations(table, buffer);

        // Valid indices: 0..1
        assertThrows(IndexOutOfBoundsException.class, () -> stations.name(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> stations.name(2));
        assertThrows(IndexOutOfBoundsException.class, () -> stations.longitude(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> stations.longitude(2));
        assertThrows(IndexOutOfBoundsException.class, () -> stations.latitude(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> stations.latitude(2));
    }

    // ----------------------------------------------------------------------
    // 4) Tests for name resolution from the string table
    // ----------------------------------------------------------------------

    @Test
    @DisplayName("Resolves correct name from string table for each station")
    void testStationNameResolution() {
        // Let's create a buffer for 2 stations, each 10 bytes
        // Station 0 => name index=2
        // Station 1 => name index=0
        // We'll put dummy lat/lon just for completeness

        byte[] data = new byte[] {
                // Station 0
                0x00, 0x02, // name index = 2
                0x00, 0x00, 0x00, 0x00, // LON
                0x00, 0x00, 0x00, 0x00, // LAT

                // Station 1
                0x00, 0x00, // name index = 0
                0x11, 0x22, 0x33, 0x44, // LON
                0x55, 0x66, 0x77, (byte)0x88 // LAT
        };
        ByteBuffer buffer = ByteBuffer.wrap(data);

        // stringTable => index 0 => "Alpha", 1 => "Beta", 2 => "Gamma"
        List<String> stringTable = Arrays.asList("Alpha", "Beta", "Gamma");

        BufferedStations stations = new BufferedStations(stringTable, buffer);

        assertEquals(2, stations.size());
        assertEquals("Gamma", stations.name(0));
        assertEquals("Alpha", stations.name(1));
    }

}
