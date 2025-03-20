package ch.epfl.rechor.timetable.mapped;

import ch.epfl.rechor.timetable.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class MyFileTimeTableTest {

    // --- Helper class for dummy file data creation ---
    private static class DummyData {

        // Writes a ByteBuffer's content to a file.
        static void writeBufferToFile(Path file, ByteBuffer buffer) throws IOException {
            buffer.flip();
            byte[] data = new byte[buffer.remaining()];
            buffer.get(data);
            Files.write(file, data);
        }

        // Dummy strings for the strings table.
        static List<String> createDummyStrings() {
            return List.of("Station0", "Alias0", "Route0", "Extra");
        }

        // For BufferedStations: structure is U16 (NAME_ID), S32 (LON), S32 (LAT) → 10 bytes per record.
        static ByteBuffer createDummyStationsBuffer() {
            ByteBuffer buffer = ByteBuffer.allocate(10).order(ByteOrder.BIG_ENDIAN);
            buffer.putShort((short) 0);          // NAME_ID = 0 (points to "Station0")
            buffer.putInt(1073741824);             // LON (dummy value)
            buffer.putInt(536870912);              // LAT (dummy value)
            return buffer;
        }

        // For BufferedStationAliases: structure is U16, U16 → 4 bytes per record.
        static ByteBuffer createDummyStationAliasesBuffer() {
            ByteBuffer buffer = ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN);
            buffer.putShort((short) 0);            // ALIAS_ID = 0 (points to "Alias0")
            buffer.putShort((short) 0);            // STATION_NAME_ID = 0 (points to "Station0")
            return buffer;
        }

        // For routes.bin (used by both BufferedRoutes and BufferedPlatforms):
        // We create a buffer whose size (12 bytes) is divisible by both 3 and 4.
        static ByteBuffer createDummyRoutesBuffer() {
            ByteBuffer buffer = ByteBuffer.allocate(12).order(ByteOrder.BIG_ENDIAN);
            for (int i = 0; i < 12; i++) {
                buffer.put((byte) i);
            }
            return buffer;
        }

        // For BufferedTransfers: structure is U16, U16, U8 → 5 bytes per record.
        static ByteBuffer createDummyTransfersBuffer() {
            ByteBuffer buffer = ByteBuffer.allocate(5).order(ByteOrder.BIG_ENDIAN);
            buffer.putShort((short) 0);  // DEP_STATION_ID = 0
            buffer.putShort((short) 1);  // ARR_STATION_ID = 1
            buffer.put((byte) 3);        // TRANSFER_MINUTES = 3
            return buffer;
        }

        // For BufferedTrips: structure is U16, U16 → 4 bytes per record.
        static ByteBuffer createDummyTripsBuffer() {
            ByteBuffer buffer = ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN);
            buffer.putShort((short) 0);  // ROUTE_ID = 0
            buffer.putShort((short) 0);  // DESTINATION_ID = 0
            return buffer;
        }

        // For BufferedConnections: structure is U16, U16, U16, U16, S32 → 12 bytes per record.
        static ByteBuffer createDummyConnectionsBuffer() {
            ByteBuffer buffer = ByteBuffer.allocate(12).order(ByteOrder.BIG_ENDIAN);
            buffer.putShort((short) 0);    // DEP_STOP_ID = 0
            buffer.putShort((short) 100);  // DEP_MINUTES = 100
            buffer.putShort((short) 1);    // ARR_STOP_ID = 1
            buffer.putShort((short) 110);  // ARR_MINUTES = 110
            // For TRIP_POS_ID, pack 123 (24 bits) and 2 (8 bits)
            int tripPosId = (123 << 8) | 2;
            buffer.putInt(tripPosId);
            return buffer;
        }

        // For BufferedConnections' successors: create a buffer containing one integer (4 bytes).
        static ByteBuffer createDummyConnectionsSuccBuffer() {
            ByteBuffer buffer = ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN);
            buffer.putInt(0); // Dummy value
            return buffer;
        }
    }

    // --- Tests for FileTimeTable ---

    // Normal case: all files exist and have valid content.
    @Test
    void testFileTimeTableNormal(@TempDir Path tempDir) throws IOException {
        // Create strings.txt.
        List<String> dummyStrings = DummyData.createDummyStrings();
        Files.write(tempDir.resolve("strings.txt"), dummyStrings);

        // Create required binary files.
        DummyData.writeBufferToFile(tempDir.resolve("stations.bin"), DummyData.createDummyStationsBuffer());
        DummyData.writeBufferToFile(tempDir.resolve("station-aliases.bin"), DummyData.createDummyStationAliasesBuffer());
        DummyData.writeBufferToFile(tempDir.resolve("routes.bin"), DummyData.createDummyRoutesBuffer());
        DummyData.writeBufferToFile(tempDir.resolve("transfers.bin"), DummyData.createDummyTransfersBuffer());

        // Create day folder with trips.bin and connections files.
        LocalDate date = LocalDate.of(2025, 3, 18);
        Path dayFolder = tempDir.resolve(date.toString());
        Files.createDirectory(dayFolder);
        DummyData.writeBufferToFile(dayFolder.resolve("trips.bin"), DummyData.createDummyTripsBuffer());
        DummyData.writeBufferToFile(dayFolder.resolve("connections.bin"), DummyData.createDummyConnectionsBuffer());
        DummyData.writeBufferToFile(dayFolder.resolve("connections-succ.bin"), DummyData.createDummyConnectionsSuccBuffer());

        TimeTable tt = FileTimeTable.in(tempDir);
        assertNotNull(tt, "FileTimeTable.in should return a non-null TimeTable");
        assertNotNull(tt.stations(), "Stations must not be null");
        assertNotNull(tt.stationAliases(), "StationAliases must not be null");
        assertNotNull(tt.routes(), "Routes must not be null");
        assertNotNull(tt.platforms(), "Platforms must not be null");
        assertNotNull(tt.transfers(), "Transfers must not be null");
        assertNotNull(tt.tripsFor(date), "tripsFor(date) must not be null");
        assertNotNull(tt.connectionsFor(date), "connectionsFor(date) must not be null");
    }

    // Edge case: Missing strings.txt file.
    @Test
    void testFileTimeTableMissingStrings(@TempDir Path tempDir) {
        Exception ex = assertThrows(IOException.class, () -> FileTimeTable.in(tempDir));
        assertNotNull(ex.getMessage(), "Exception message should not be null when strings.txt is missing");
    }

    // Edge case: Non-divisible binary file size.
    @Test
    void testFileTimeTableNonDivisibleBinaryFileSize(@TempDir Path tempDir) throws IOException {
        Files.write(tempDir.resolve("strings.txt"), DummyData.createDummyStrings());
        // Create stations.bin with a size not divisible by 10.
        ByteBuffer badBuffer = ByteBuffer.allocate(7).order(ByteOrder.BIG_ENDIAN);
        Files.write(tempDir.resolve("stations.bin"), badBuffer.array());
        DummyData.writeBufferToFile(tempDir.resolve("station-aliases.bin"), DummyData.createDummyStationAliasesBuffer());
        DummyData.writeBufferToFile(tempDir.resolve("routes.bin"), DummyData.createDummyRoutesBuffer());
        DummyData.writeBufferToFile(tempDir.resolve("transfers.bin"), DummyData.createDummyTransfersBuffer());

        LocalDate date = LocalDate.of(2025, 3, 18);
        Path dayFolder = tempDir.resolve(date.toString());
        Files.createDirectory(dayFolder);
        DummyData.writeBufferToFile(dayFolder.resolve("trips.bin"), DummyData.createDummyTripsBuffer());
        DummyData.writeBufferToFile(dayFolder.resolve("connections.bin"), DummyData.createDummyConnectionsBuffer());
        DummyData.writeBufferToFile(dayFolder.resolve("connections-succ.bin"), DummyData.createDummyConnectionsSuccBuffer());

        assertThrows(IllegalArgumentException.class, () -> FileTimeTable.in(tempDir));
    }

    // --- Tests for date-specific files ---

    // Test tripsFor: verify that a valid trips.bin returns the expected number of records.
    @Test
    void testTripsForValid(@TempDir Path tempDir) throws IOException {
        Files.write(tempDir.resolve("strings.txt"), DummyData.createDummyStrings());
        DummyData.writeBufferToFile(tempDir.resolve("stations.bin"), DummyData.createDummyStationsBuffer());
        DummyData.writeBufferToFile(tempDir.resolve("station-aliases.bin"), DummyData.createDummyStationAliasesBuffer());
        DummyData.writeBufferToFile(tempDir.resolve("routes.bin"), DummyData.createDummyRoutesBuffer());
        DummyData.writeBufferToFile(tempDir.resolve("transfers.bin"), DummyData.createDummyTransfersBuffer());

        LocalDate date = LocalDate.of(2025, 3, 18);
        Path dayFolder = tempDir.resolve(date.toString());
        Files.createDirectory(dayFolder);

        // Create trips.bin with 2 records (each 4 bytes).
        ByteBuffer tripsBuffer = ByteBuffer.allocate(8).order(ByteOrder.BIG_ENDIAN);
        tripsBuffer.putShort((short) 0).putShort((short) 0);
        tripsBuffer.putShort((short) 1).putShort((short) 1);
        DummyData.writeBufferToFile(dayFolder.resolve("trips.bin"), tripsBuffer);

        DummyData.writeBufferToFile(dayFolder.resolve("connections.bin"), DummyData.createDummyConnectionsBuffer());
        DummyData.writeBufferToFile(dayFolder.resolve("connections-succ.bin"), DummyData.createDummyConnectionsSuccBuffer());

        TimeTable tt = FileTimeTable.in(tempDir);
        Trips trips = tt.tripsFor(date);
        assertNotNull(trips, "Trips must not be null for valid trips.bin");
        assertEquals(2, trips.size(), "Expected 2 trip records");
    }

    // Test connectionsFor: verify that a valid connections.bin returns the expected number of records.
    @Test
    void testConnectionsForValid(@TempDir Path tempDir) throws IOException {
        Files.write(tempDir.resolve("strings.txt"), DummyData.createDummyStrings());
        DummyData.writeBufferToFile(tempDir.resolve("stations.bin"), DummyData.createDummyStationsBuffer());
        DummyData.writeBufferToFile(tempDir.resolve("station-aliases.bin"), DummyData.createDummyStationAliasesBuffer());
        DummyData.writeBufferToFile(tempDir.resolve("routes.bin"), DummyData.createDummyRoutesBuffer());
        DummyData.writeBufferToFile(tempDir.resolve("transfers.bin"), DummyData.createDummyTransfersBuffer());

        LocalDate date = LocalDate.of(2025, 3, 18);
        Path dayFolder = tempDir.resolve(date.toString());
        Files.createDirectory(dayFolder);

        // Create a valid trips.bin.
        DummyData.writeBufferToFile(dayFolder.resolve("trips.bin"), DummyData.createDummyTripsBuffer());

        // Create connections.bin with 2 records (each 12 bytes → 24 bytes total).
        ByteBuffer connBuffer = ByteBuffer.allocate(24).order(ByteOrder.BIG_ENDIAN);
        // Record 1.
        connBuffer.putShort((short) 0).putShort((short) 100)
                .putShort((short) 1).putShort((short) 110)
                .putInt((123 << 8) | 2);
        // Record 2.
        connBuffer.putShort((short) 1).putShort((short) 120)
                .putShort((short) 2).putShort((short) 130)
                .putInt((456 << 8) | 3);
        DummyData.writeBufferToFile(dayFolder.resolve("connections.bin"), connBuffer);

        // Create connections-succ.bin with 2 integers (8 bytes total).
        ByteBuffer succBuffer = ByteBuffer.allocate(8).order(ByteOrder.BIG_ENDIAN);
        succBuffer.putInt(0).putInt(1);
        DummyData.writeBufferToFile(dayFolder.resolve("connections-succ.bin"), succBuffer);

        TimeTable tt = FileTimeTable.in(tempDir);
        Connections conns = tt.connectionsFor(date);
        assertNotNull(conns, "Connections must not be null for valid connections.bin");
        assertEquals(2, conns.size(), "Expected 2 connection records");
    }
}
