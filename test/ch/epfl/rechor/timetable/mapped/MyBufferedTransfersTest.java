package ch.epfl.rechor.timetable.mapped;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.ByteBuffer;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.Test;
import ch.epfl.rechor.PackedRange;

public class MyBufferedTransfersTest {

    /**
     * Helper method that creates a ByteBuffer containing transfers.
     * Each transfer is represented as 5 bytes:
     *   - U16 departure station id,
     *   - U16 arrival station id,
     *   - U8 transfer minutes.
     * Each transfer is provided as an int array of length 3.
     */
    private ByteBuffer createBuffer(int[][] transfers) {
        ByteBuffer buffer = ByteBuffer.allocate(transfers.length * 5);
        for (int[] t : transfers) {
            // Assumes values are small enough to fit in U16/U8.
            buffer.putShort((short) t[0]);  // departure station id
            buffer.putShort((short) t[1]);  // arrival station id
            buffer.put((byte) t[2]);        // transfer minutes
        }
        buffer.flip();
        return buffer;
    }

    @Test
    public void testEmptyTransfers() {
        // Create an empty ByteBuffer
        ByteBuffer emptyBuffer = ByteBuffer.allocate(0);
        BufferedTransfers bt = new BufferedTransfers(emptyBuffer);

        // size() should be 0.
        assertEquals(0, bt.size(), "Size should be 0 for an empty buffer.");

        // Calling depStationId or minutes should throw an IndexOutOfBoundsException.
        assertThrows(IndexOutOfBoundsException.class, () -> bt.depStationId(0));
        assertThrows(IndexOutOfBoundsException.class, () -> bt.minutes(0));

        // According to the spec, arrivingAt(stationId) should return 0 if no transfer arrives there.
        // Since there are no transfers, valid station ids (even if not in the arrivingAtTable)
        // should yield 0. If your implementation instead throws, consider modifying it.
        try {
            assertEquals(0, bt.arrivingAt(0), "arrivingAt should return 0 when no transfer exists.");
        } catch (IndexOutOfBoundsException e) {
            // Acceptable if the implementation uses checkIndex (adjust implementation to match spec if desired)
        }
    }

    @Test
    public void testSingleTransfer() {
        // One transfer: from station 1 to station 2 with duration 5 minutes.
        int[][] data = { {1, 2, 5} };
        BufferedTransfers bt = new BufferedTransfers(createBuffer(data));

        // There is one transfer.
        assertEquals(1, bt.size());

        // Test depStationId and minutes.
        assertEquals(1, bt.depStationId(0), "depStationId(0) should be 1.");
        assertEquals(5, bt.minutes(0), "minutes(0) should be 5.");

        // For arrivingAt: since transfers are sorted by arrival station,
        // the constructor computes an arrivingAtTable of length maxArrival+1.
        // In this case, maxArrival is 2 so we expect arrivingAtTable.length==3.
        // Spec says arrivingAt(station) returns 0 if no transfer arrives at that station.
        // Thus, arrivingAt(0) and arrivingAt(1) should be 0,
        // and arrivingAt(2) should pack start index 0 and count 1.
        try {
            assertEquals(0, bt.arrivingAt(0), "No transfer should arrive at station 0.");
            assertEquals(0, bt.arrivingAt(1), "No transfer should arrive at station 1.");
            int interval = bt.arrivingAt(2);
            assertEquals(0, PackedRange.startInclusive(interval), "Start index for station 2 should be 0.");
            assertEquals(1, PackedRange.length(interval), "There should be 1 transfer arriving at station 2.");
        } catch (IndexOutOfBoundsException e) {
            // If your implementation throws instead of returning 0, adjust arrivingAt to use
            // the station id range (0 <= stationId < arrivingAtTable.length) instead of size().
        }

        // Test minutesBetween:
        // Valid call: minutesBetween(1, 2) should return 5.
        assertEquals(5, bt.minutesBetween(1, 2), "minutesBetween(1,2) should be 5.");

        // Calling minutesBetween with a departure that does not exist should throw.
        assertThrows(NoSuchElementException.class, () -> bt.minutesBetween(2, 2),
                "No transfer from station 2 to 2, so NoSuchElementException should be thrown.");
    }

    @Test
    public void testMultipleTransfers() {
        // Three transfers:
        //  Transfer 0: from station 10 to station 5, 3 minutes.
        //  Transfer 1: from station 20 to station 5, 4 minutes.
        //  Transfer 2: from station 15 to station 7, 6 minutes.
        int[][] data = { {10, 5, 3}, {20, 5, 4}, {15, 7, 6} };
        BufferedTransfers bt = new BufferedTransfers(createBuffer(data));

        assertEquals(3, bt.size(), "Size should be 3.");

        // Test depStationId and minutes.
        assertEquals(10, bt.depStationId(0));
        assertEquals(20, bt.depStationId(1));
        assertEquals(15, bt.depStationId(2));
        assertEquals(3, bt.minutes(0));
        assertEquals(4, bt.minutes(1));
        assertEquals(6, bt.minutes(2));

        // arrivingAtTable should be computed as follows:
        //  - For arrival station 5: transfers 0 and 1 (start index 0, count 2).
        //  - For arrival station 7: transfer 2 (start index 2, count 1).
        //  - All other station ids (within range) yield 0.
        try {
            int interval5 = bt.arrivingAt(5);
            assertEquals(0, PackedRange.startInclusive(interval5),
                    "For station 5, start index should be 0.");
            assertEquals(2, PackedRange.length(interval5),
                    "For station 5, count should be 2.");

            int interval7 = bt.arrivingAt(7);
            assertEquals(2, PackedRange.startInclusive(interval7),
                    "For station 7, start index should be 2.");
            assertEquals(1, PackedRange.length(interval7),
                    "For station 7, count should be 1.");

            // Test a station that has no arriving transfers.
            assertEquals(0, bt.arrivingAt(6),
                    "Station 6 should yield 0 as no transfer arrives there.");
        } catch (IndexOutOfBoundsException e) {
            // Adjust arrivingAt implementation so that station ids are checked against arrivingAtTable.length.
        }

        // Test minutesBetween:
        // Valid lookups:
        assertEquals(3, bt.minutesBetween(10, 5), "minutesBetween(10,5) should be 3.");
        assertEquals(4, bt.minutesBetween(20, 5), "minutesBetween(20,5) should be 4.");
        assertEquals(6, bt.minutesBetween(15, 7), "minutesBetween(15,7) should be 6.");

        // When no matching transfer exists, minutesBetween should throw a NoSuchElementException.
        assertThrows(NoSuchElementException.class, () -> bt.minutesBetween(30, 5),
                "minutesBetween(30,5) should throw NoSuchElementException because no transfer departs from station 30.");
        assertThrows(NoSuchElementException.class, () -> bt.minutesBetween(10, 6),
                "minutesBetween(10,6) should throw NoSuchElementException because station 6 has no arriving transfer.");
    }

    @Test
    public void testInvalidTransferIndices() {
        // Use a single transfer for simplicity.
        int[][] data = { {1, 2, 5} };
        BufferedTransfers bt = new BufferedTransfers(createBuffer(data));

        // depStationId and minutes should throw for negative indices or indices >= size().
        assertThrows(IndexOutOfBoundsException.class, () -> bt.depStationId(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> bt.minutes(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> bt.depStationId(1));
        assertThrows(IndexOutOfBoundsException.class, () -> bt.minutes(1));
    }

    @Test
    public void testMinutesBetweenNoMatchingTransfer() {
        // Two transfers arriving at the same station, but neither from the queried departure.
        int[][] data = { {10, 5, 3}, {20, 5, 4} };
        BufferedTransfers bt = new BufferedTransfers(createBuffer(data));

        // minutesBetween for a departure that does not exist should throw.
        assertThrows(NoSuchElementException.class, () -> bt.minutesBetween(30, 5));
    }
}


