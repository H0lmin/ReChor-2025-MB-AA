package ch.epfl.rechor.timetable.mapped;

import ch.epfl.rechor.Bits32_24_8;
import ch.epfl.rechor.PackedRange;
import ch.epfl.rechor.timetable.Transfers;

import java.nio.ByteBuffer;
import java.util.NoSuchElementException;


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

    private int arrStationId (int id) {
        return structuredBuffer.getU16(ARR_STATION_ID, id);
    }

    @Override
    public int depStationId (int id) {
        return structuredBuffer.getU16(DEP_STATION_ID, id);
    }

    @Override
    public int minutes (int id) {
        return structuredBuffer.getU8(TRANSFER_MINUTES, id);
    }

    @Override
    public int arrivingAt (int stationId) {
        if (stationId < 0 || stationId >= arrivingAtTable.length) {
            throw new IndexOutOfBoundsException("The id isn't valid ");
        }
        return arrivingAtTable[stationId];
    }

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

    @Override
    public int size () {
        return structuredBuffer.size();
    }

}
