package ch.epfl.rechor.timetable.mapped;

import ch.epfl.rechor.Bits32_24_8;
import ch.epfl.rechor.PackedRange;
import ch.epfl.rechor.timetable.Transfers;

import java.nio.ByteBuffer;
import java.util.NoSuchElementException;

/**
 * Provides access to flattened data about a table of changes.
 *
 * @author Amine AMIRA (393410)
 * @author Malak Berrada (379791)
 */

public class BufferedTransfers implements Transfers {

    private final static int DEP_STATION_ID = 0;
    private final static int ARR_STATION_ID = 1;
    private final static int TRANSFER_MINUTES = 2;

    private static final Structure TRANSFERS_STRUCTURE = new Structure(
            Structure.field(DEP_STATION_ID, Structure.FieldType.U16),
            Structure.field(ARR_STATION_ID, Structure.FieldType.U16),
            Structure.field(TRANSFER_MINUTES, Structure.FieldType.U8)
    );

    private final StructuredBuffer structuredBuffer;
    private final int[] arrivingAtTable;

    /**
     * Constructs a {@code BufferedTransfers} instance.
     *
     * @param buffer the byte buffer containing the flattened platform data.
     */
    public BufferedTransfers (ByteBuffer buffer) {
        structuredBuffer = new StructuredBuffer(TRANSFERS_STRUCTURE, buffer);
        int n = structuredBuffer.size();

        int maxStation = -1;
        for (int i = 0; i < n; i++) {
            int station = arrStationId(i);
            if (station > maxStation) {
                maxStation = station;
            }
        }
        arrivingAtTable = new int[maxStation + 1];

        for (int i = 0; i < n; ) {
            int station = arrStationId(i);
            int start = i;
            while (i < n && arrStationId(i) == station) {
                i++;
            }
            arrivingAtTable[station] = Bits32_24_8.pack(start, i - start);
        }
    }

    /**
     * Private method to return the index of the arrival station for the transfer at the given index.
     */
    private int arrStationId (int id) {
        return structuredBuffer.getU16(ARR_STATION_ID, id);
    }

    /**
     * Returns the index of the departure station for the transfer at the given index.
     *
     * @param id the index of the transfer
     * @throws IndexOutOfBoundsException if the index is invalid (id < 0 or id >= size())
     * @return the index of the departure station
     */
    @Override
    public int depStationId (int id) {
        return structuredBuffer.getU16(DEP_STATION_ID, id);
    }

    /**
     * Returns the duration, in minutes, of the transfer at the given index.
     *
     * @param id the index of the transfer
     * @throws IndexOutOfBoundsException if the index is invalid (id < 0 or id >= size())
     * @return the duration of the transfer
     */
    @Override
    public int minutes (int id) {
        return structuredBuffer.getU8(TRANSFER_MINUTES, id);
    }

    /**
     * Returns the packed interval of transfer indices whose arrival station is the one at the given index
     *
     * @param stationId the index of the arrival station
     * @throws IndexOutOfBoundsException if the index is invalid (stationId < 0 or stationId >= size())
     * @return the interval of transfer indices
     */
    @Override
    public int arrivingAt (int stationId) {
        if (stationId < 0 || stationId >= arrivingAtTable.length) {
            throw new IndexOutOfBoundsException("The id isn't valid ");
        }
        return arrivingAtTable[stationId];
    }

    /**
     * Returns the duration of the transfer between the two stations at the given indices
     *
     * @param depStationId the index of the departure station
     * @param arrStationId the index of the arrival station
     * @throws IndexOutOfBoundsException if either index is invalid
     * @throws NoSuchElementException    if no changes are possible between the two stations
     * @return the duration of change between the departure station and station of arrival
     */
    @Override
    public int minutesBetween (int depStationId, int arrStationId) {

        int interval = arrivingAt(arrStationId);

        int start = PackedRange.startInclusive(interval);
        int end = PackedRange.endExclusive(interval);
        for (int i = start; i < end; i++) {
            if (depStationId(i) == depStationId)
                return minutes(i);
        }

        throw new NoSuchElementException("No transfer found between stations " + depStationId + " and " + arrStationId);
    }

    /**
     * Returns the number of transfers stored in the buffer.
     *
     * @return the number of transfers.
     */
    @Override
    public int size () {
        return structuredBuffer.size();
    }

}
