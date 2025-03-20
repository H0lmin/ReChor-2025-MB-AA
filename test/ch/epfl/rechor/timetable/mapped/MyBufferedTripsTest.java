package ch.epfl.rechor.timetable.mapped;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.nio.ByteBuffer;
import java.util.HexFormat;
import java.util.List;

import ch.epfl.rechor.timetable.Trips;

class MyBufferedTripsTest {

    /**
     * Helper method to create a read-only ByteBuffer from a hexadecimal string.
     * The hex string should use spaces as delimiters.
     */
    private static ByteBuffer byteBuffer(String hex) {
        return ByteBuffer.wrap(HexFormat.ofDelimiter(" ").parseHex(hex))
                .asReadOnlyBuffer();
    }

    // --- Sample Data Definitions ---

    // Single trip record:
    // Record structure (4 bytes per record):
    //   - ROUTE_ID: two bytes ("00 01") → value 1
    //   - DESTINATION_ID: two bytes ("00 02") → should retrieve the string at index 2
    private static final ByteBuffer TRIPS_1 = byteBuffer("00 01 00 02");
    private static final List<String> STRING_TABLE_1 = List.of("Line A", "Line B", "Dest A");

    // Two trip records:
    // Record 0:
    //   - ROUTE_ID: "00 03" → value 3
    //   - DESTINATION_ID: "00 04" → string at index 4
    // Record 1:
    //   - ROUTE_ID: "00 05" → value 5
    //   - DESTINATION_ID: "00 01" → string at index 1
    private static final ByteBuffer TRIPS_2 = byteBuffer("00 03 00 04 00 05 00 01");
    // The string table must have at least indices 1 and 4.
    // For clarity, we use placeholders for unused entries.
    private static final List<String> STRING_TABLE_2 = List.of("Ignore", "Dest Y", "Ignore", "Ignore", "Dest B", "Dest C");

    // Invalid trip record: destination index out of bounds.
    // Here, DESTINATION_ID is "00 03" but the string table has only 3 entries (indices 0,1,2)
    private static final ByteBuffer TRIPS_INVALID = byteBuffer("00 00 00 03");
    private static final List<String> STRING_TABLE_INVALID = List.of("Dest0", "Dest1", "Dest2");

    // Empty trips buffer
    private static final ByteBuffer TRIPS_EMPTY = ByteBuffer.allocate(0).asReadOnlyBuffer();
    private static final List<String> STRING_TABLE_EMPTY = List.of("Dummy");

    // --- Test Methods ---

    @Test
    void testSizeEmptyBuffer() {
        Trips trips = new BufferedTrips(STRING_TABLE_EMPTY, TRIPS_EMPTY);

        assertEquals(0, trips.size());
    }

    @Test
    void testSizeSingleTrip() {
        Trips trips = new BufferedTrips(STRING_TABLE_1, TRIPS_1);

        assertEquals(1, trips.size());
    }

    @Test
    void testRouteAndDestinationSingleTrip() {
        Trips trips = new BufferedTrips(STRING_TABLE_1, TRIPS_1);
        // For the single record:
        // ROUTE_ID should be 1 and destination should be the string at index 2 ("Dest A")
        assertEquals(1, trips.routeId(0));
        assertEquals("Dest A", trips.destination(0));
    }

    @Test
    void testRouteAndDestinationMultipleTrips() {
        Trips trips = new BufferedTrips(STRING_TABLE_2, TRIPS_2);
        assertEquals(2, trips.size());

        // Record 0: ROUTE_ID = 3, DESTINATION_ID = 4 → "Dest B"
        assertEquals(3, trips.routeId(0));
        assertEquals("Dest B", trips.destination(0));

        // Record 1: ROUTE_ID = 5, DESTINATION_ID = 1 → "Dest Y"
        assertEquals(5, trips.routeId(1));
        assertEquals("Dest Y", trips.destination(1));
    }

    @Test
    void testIndexOutOfBounds() {
        Trips trips = new BufferedTrips(STRING_TABLE_1, TRIPS_1);
        // Negative index
        assertThrows(IndexOutOfBoundsException.class, () -> trips.routeId(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> trips.destination(-1));

        // Index equal to size (i.e. 1) should throw an exception
        assertThrows(IndexOutOfBoundsException.class, () -> trips.routeId(1));
        assertThrows(IndexOutOfBoundsException.class, () -> trips.destination(1));
    }

    @Test
    void testInvalidDestinationIndex() {
        Trips trips = new BufferedTrips(STRING_TABLE_INVALID, TRIPS_INVALID);
        // Although route(0) is valid, destination(0) should throw an exception
        // because DESTINATION_ID (value 3) is not present in the string table.
        assertEquals(0, trips.routeId(0));
        assertThrows(IndexOutOfBoundsException.class, () -> trips.destination(0));
    }
}
