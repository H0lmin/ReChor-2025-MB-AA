package ch.epfl.rechor.timetable.mapped;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.nio.ByteBuffer;
import java.util.HexFormat;
import java.util.List;

import ch.epfl.rechor.timetable.Routes;
import ch.epfl.rechor.journey.Vehicle;

class MyBufferedRoutesTest {

    /**
     * Helper method to convert a hexadecimal string (with space delimiters)
     * into a read-only ByteBuffer.
     */
    private static ByteBuffer byteBuffer(String hex) {
        return ByteBuffer.wrap(HexFormat.ofDelimiter(" ").parseHex(hex))
                .asReadOnlyBuffer();
    }

    // --- Sample Data Definitions ---

    // Single route record:
    // Record layout (3 bytes per record):
    //   NAME_ID: 00 00 (index 0) and
    //   KIND: 01 (vehicle kind 1, e.g., METRO)
    private static final ByteBuffer ROUTES_1 = byteBuffer("00 00 01");
    private static final List<String> STRING_TABLE_1 = List.of("Line A");

    // Two route records:
    // Record 0: NAME_ID = 00 01 (index 1), KIND = 00 (vehicle kind 0, e.g., TRAM)
    // Record 1: NAME_ID = 00 02 (index 2), KIND = 05 (vehicle kind 5)
    private static final ByteBuffer ROUTES_2 = byteBuffer("00 01 00 00 02 05");
    private static final List<String> STRING_TABLE_2 = List.of("Line X", "Line Y", "Line Z");

    // Invalid route record: NAME_ID is out of bounds.
    // Record: NAME_ID = 00 03 (index 3, but table has only 3 entries: 0,1,2), KIND = 02.
    private static final ByteBuffer ROUTES_INVALID = byteBuffer("00 03 02");
    private static final List<String> STRING_TABLE_INVALID = List.of("Line A", "Line B", "Line C");

    // Empty routes buffer
    private static final ByteBuffer ROUTES_EMPTY = ByteBuffer.allocate(0).asReadOnlyBuffer();
    private static final List<String> STRING_TABLE_EMPTY = List.of("Line A", "Line B");

    // --- Test Methods ---

    @Test
    void testSizeEmptyBuffer() {
        Routes routes = new BufferedRoutes(STRING_TABLE_EMPTY, ROUTES_EMPTY);
        assertEquals(0, routes.size());
    }

    @Test
    void testSizeSingleRoute() {
        Routes routes = new BufferedRoutes(STRING_TABLE_1, ROUTES_1);

        assertEquals(1, routes.size());
    }

    @Test
    void testNameAndVehicleSingleRoute() {
        Routes routes = new BufferedRoutes(STRING_TABLE_1, ROUTES_1);

        assertEquals("Line A", routes.name(0));
        assertEquals(Vehicle.METRO, routes.vehicle(0));
    }

    @Test
    void testNameAndVehicleMultipleRoutes() {
        Routes routes = new BufferedRoutes(STRING_TABLE_2, ROUTES_2);
        assertEquals(2, routes.size());

        // First record: NAME_ID = 1 -> "Line Y", vehicle kind = 0 (TRAM)
        assertEquals("Line Y", routes.name(0));
        assertEquals(Vehicle.TRAM, routes.vehicle(0));

        // Second record: NAME_ID = 2 -> "Line Z", vehicle kind = 5.
        assertEquals("Line Z", routes.name(1));
        assertEquals(5, routes.vehicle(1).ordinal());
    }

    @Test
    void testIndexOutOfBounds() {
        Routes routes = new BufferedRoutes(STRING_TABLE_1, ROUTES_1);
        // Negative indices
        assertThrows(IndexOutOfBoundsException.class, () -> routes.name(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> routes.vehicle(-1));
        // Index equal to size (out-of-bound)
        assertThrows(IndexOutOfBoundsException.class, () -> routes.name(1));
        assertThrows(IndexOutOfBoundsException.class, () -> routes.vehicle(1));
    }

    @Test
    void testInvalidNameIndex() {
        Routes routes = new BufferedRoutes(STRING_TABLE_INVALID, ROUTES_INVALID);
        // The record refers to NAME_ID = 3, which is not available in STRING_TABLE_INVALID.
        assertThrows(IndexOutOfBoundsException.class, () -> routes.name(0));
    }

    @Test
    void testVehicleKindBoundary() {
        // Test a record with the maximum allowed vehicle kind (e.g., 6).
        // Record: NAME_ID = 00 00, KIND = 06.
        ByteBuffer routeBoundary = byteBuffer("00 00 06");
        Routes routes = new BufferedRoutes(STRING_TABLE_1, routeBoundary);

        assertEquals(1, routes.size());
        assertEquals("Line A", routes.name(0));
        // Verify that the returned vehicle has ordinal 6.
        assertEquals(6, routes.vehicle(0).ordinal());
    }
}
