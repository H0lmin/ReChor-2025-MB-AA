package ch.epfl.rechor.timetable.mapped;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import org.junit.jupiter.api.Test;
import ch.epfl.rechor.Bits32_24_8;

public class MyBufferedConnectionsTest {

    /**
     * Helper method to create a ByteBuffer containing flattened connection records.
     * Each connection occupies 12 bytes:
     *   - Field 0: DEP_STOP_ID (U16)  – 2 bytes
     *   - Field 1: DEP_MINUTES (U16)  – 2 bytes
     *   - Field 2: ARR_STOP_ID (U16)  – 2 bytes
     *   - Field 3: ARR_MINUTES (U16)  – 2 bytes
     *   - Field 4: TRIP_POS_ID (S32)  – 4 bytes, packed using Bits32_24_8 (24 bits for course index, 8 bits for position)
     *
     * The input array "connections" should have one row per connection, each with 6 integers:
     * {depStopId, depMinutes, arrStopId, arrMinutes, courseIndex, tripPosition}
     */
    private ByteBuffer createConnectionsBuffer(int[][] connections) {
        // Each connection occupies 12 bytes.
        ByteBuffer buffer = ByteBuffer.allocate(connections.length * 12);
        buffer.order(ByteOrder.BIG_ENDIAN);
        for (int[] conn : connections) {
            // conn: {depStopId, depMinutes, arrStopId, arrMinutes, courseIndex, tripPosition}
            buffer.putShort((short) conn[0]);  // DEP_STOP_ID
            buffer.putShort((short) conn[1]);  // DEP_MINUTES
            buffer.putShort((short) conn[2]);  // ARR_STOP_ID
            buffer.putShort((short) conn[3]);  // ARR_MINUTES
            int tripPosId = Bits32_24_8.pack(conn[4], conn[5]);
            buffer.putInt(tripPosId);          // TRIP_POS_ID (packed)
        }
        buffer.flip();
        return buffer;
    }

    /**
     * Helper method to create a ByteBuffer for the "next connection" table.
     * Each entry is a 32-bit integer (4 bytes). The input array "succs" has one entry per connection.
     */
    private ByteBuffer createSuccBuffer(int[] succs) {
        ByteBuffer buffer = ByteBuffer.allocate(succs.length * 4);
        buffer.order(ByteOrder.BIG_ENDIAN);
        for (int s : succs) {
            buffer.putInt(s);
        }
        buffer.flip();
        return buffer;
    }

    @Test
    public void testEmptyConnections() {
        // Create empty buffers for connections and next-connection table.
        ByteBuffer connectionsBuffer = ByteBuffer.allocate(0);
        ByteBuffer succBuffer = ByteBuffer.allocate(0);
        BufferedConnections bc = new BufferedConnections(connectionsBuffer, succBuffer);

        assertEquals(0, bc.size(), "Size should be 0 for empty connections.");

        // Attempting to access any connection index should throw IndexOutOfBoundsException.
        assertThrows(IndexOutOfBoundsException.class, () -> bc.depStopId(0));
        assertThrows(IndexOutOfBoundsException.class, () -> bc.depMins(0));
        assertThrows(IndexOutOfBoundsException.class, () -> bc.arrStopId(0));
        assertThrows(IndexOutOfBoundsException.class, () -> bc.arrMins(0));
        assertThrows(IndexOutOfBoundsException.class, () -> bc.tripPos(0));
        assertThrows(IndexOutOfBoundsException.class, () -> bc.tripId(0));
        assertThrows(IndexOutOfBoundsException.class, () -> bc.nextConnectionId(0));
    }

    @Test
    public void testSingleConnection() {
        // Single connection:
        // DEP_STOP_ID = 101, DEP_MINUTES = 500, ARR_STOP_ID = 102, ARR_MINUTES = 510,
        // Trip: course index = 5, trip position = 2.
        int[][] connections = { {101, 500, 102, 510, 5, 2} };
        // For a single connection, the next connection is itself (circular linkage).
        int[] succs = { 0 };

        ByteBuffer connectionsBuffer = createConnectionsBuffer(connections);
        ByteBuffer succBuffer = createSuccBuffer(succs);
        BufferedConnections bc = new BufferedConnections(connectionsBuffer, succBuffer);

        assertEquals(1, bc.size(), "Size should be 1.");

        // Test field getters.
        assertEquals(101, bc.depStopId(0), "depStopId should be 101.");
        assertEquals(500, bc.depMins(0), "depMinutes should be 500.");
        assertEquals(102, bc.arrStopId(0), "arrStopId should be 102.");
        assertEquals(510, bc.arrMins(0), "arrMinutes should be 510.");
        assertEquals(5, bc.tripId(0), "Course index should be 5.");
        assertEquals(2, bc.tripPos(0), "Trip position should be 2.");

        // Test next connection.
        assertEquals(0, bc.nextConnectionId(0), "For single connection, next connection should be itself (0).");
    }

    @Test
    public void testMultipleConnections() {
        // Create three connections.
        // Connection 0: belongs to course 100, position 0.
        //   DEP_STOP_ID = 10, DEP_MINUTES = 700, ARR_STOP_ID = 20, ARR_MINUTES = 710.
        // Connection 1: belongs to course 100, position 1.
        //   DEP_STOP_ID = 11, DEP_MINUTES = 650, ARR_STOP_ID = 21, ARR_MINUTES = 660.
        // Connection 2: belongs to course 200, position 0.
        //   DEP_STOP_ID = 12, DEP_MINUTES = 600, ARR_STOP_ID = 22, ARR_MINUTES = 610.
        int[][] connections = {
                {10, 700, 20, 710, 100, 0},
                {11, 650, 21, 660, 100, 1},
                {12, 600, 22, 610, 200, 0}
        };
        // Set up the next connection table:
        // For course 100, connection 0's next is connection 1, and connection 1's next is connection 0.
        // For course 200, connection 2's next is itself.
        int[] succs = { 1, 0, 2 };

        ByteBuffer connectionsBuffer = createConnectionsBuffer(connections);
        ByteBuffer succBuffer = createSuccBuffer(succs);
        BufferedConnections bc = new BufferedConnections(connectionsBuffer, succBuffer);

        assertEquals(3, bc.size(), "Size should be 3.");

        // Test connection 0.
        assertEquals(10, bc.depStopId(0), "Connection 0 depStopId should be 10.");
        assertEquals(700, bc.depMins(0), "Connection 0 depMinutes should be 700.");
        assertEquals(20, bc.arrStopId(0), "Connection 0 arrStopId should be 20.");
        assertEquals(710, bc.arrMins(0), "Connection 0 arrMinutes should be 710.");
        assertEquals(100, bc.tripId(0), "Connection 0 course index should be 100.");
        assertEquals(0, bc.tripPos(0), "Connection 0 trip position should be 0.");
        assertEquals(1, bc.nextConnectionId(0), "Connection 0 next connection should be 1.");

        // Test connection 1.
        assertEquals(11, bc.depStopId(1), "Connection 1 depStopId should be 11.");
        assertEquals(650, bc.depMins(1), "Connection 1 depMinutes should be 650.");
        assertEquals(21, bc.arrStopId(1), "Connection 1 arrStopId should be 21.");
        assertEquals(660, bc.arrMins(1), "Connection 1 arrMinutes should be 660.");
        assertEquals(100, bc.tripId(1), "Connection 1 course index should be 100.");
        assertEquals(1, bc.tripPos(1), "Connection 1 trip position should be 1.");
        assertEquals(0, bc.nextConnectionId(1), "Connection 1 next connection should be 0.");

        // Test connection 2.
        assertEquals(12, bc.depStopId(2), "Connection 2 depStopId should be 12.");
        assertEquals(600, bc.depMins(2), "Connection 2 depMinutes should be 600.");
        assertEquals(22, bc.arrStopId(2), "Connection 2 arrStopId should be 22.");
        assertEquals(610, bc.arrMins(2), "Connection 2 arrMinutes should be 610.");
        assertEquals(200, bc.tripId(2), "Connection 2 course index should be 200.");
        assertEquals(0, bc.tripPos(2), "Connection 2 trip position should be 0.");
        assertEquals(2, bc.nextConnectionId(2), "Connection 2 next connection should be itself (2).");
    }

    @Test
    public void testInvalidIndices() {
        // Create a single connection.
        int[][] connections = { {101, 500, 102, 510, 5, 2} };
        int[] succs = { 0 };

        ByteBuffer connectionsBuffer = createConnectionsBuffer(connections);
        ByteBuffer succBuffer = createSuccBuffer(succs);
        BufferedConnections bc = new BufferedConnections(connectionsBuffer, succBuffer);

        // Negative indices or indices >= size() should throw IndexOutOfBoundsException.
        assertThrows(IndexOutOfBoundsException.class, () -> bc.depStopId(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> bc.depMins(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> bc.arrStopId(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> bc.arrMins(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> bc.tripId(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> bc.tripPos(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> bc.nextConnectionId(-1));

        assertThrows(IndexOutOfBoundsException.class, () -> bc.depStopId(1));
        assertThrows(IndexOutOfBoundsException.class, () -> bc.depMins(1));
        assertThrows(IndexOutOfBoundsException.class, () -> bc.arrStopId(1));
        assertThrows(IndexOutOfBoundsException.class, () -> bc.arrMins(1));
        assertThrows(IndexOutOfBoundsException.class, () -> bc.tripId(1));
        assertThrows(IndexOutOfBoundsException.class, () -> bc.nextConnectionId(1));
    }
}

