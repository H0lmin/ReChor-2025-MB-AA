package ch.epfl.rechor.timetable.mapped;

import ch.epfl.rechor.Bits32_24_8;
import ch.epfl.rechor.PackedRange;
import ch.epfl.rechor.timetable.Transfers;

import java.nio.ByteBuffer;
import java.util.NoSuchElementException;

public class BufferedTransfers implements Transfers {

    private final StructuredBuffer structuredBuffer;
    private final int[] arrivingAtTable;

    private static final Structure TRANSFERS_STRUCTURE = new Structure(
            Structure.field(0, Structure.FieldType.U16),
            Structure.field(1, Structure.FieldType.U16),
            Structure.field(2, Structure.FieldType.U8)
    );

    public BufferedTransfers (ByteBuffer buffer){
        structuredBuffer = new StructuredBuffer(TRANSFERS_STRUCTURE, buffer);
        int n = structuredBuffer.size();

        if (n == 0){
            arrivingAtTable = new int[0];
        } else {
            int maxArrivalStation = structuredBuffer.getU16(1, n - 1);
            arrivingAtTable = new int[maxArrivalStation + 1];

            int i = 0;
            while (i < n){
                int station = structuredBuffer.getU16(1, i);
                int start = i;
                while (i < n && structuredBuffer.getU16(1, i) == station){
                    i++;
                }
                int count = i - start;
                arrivingAtTable[station] = Bits32_24_8.pack(start, count);
            }
        }

    }
    @Override
    public int depStationId(int id) {
        checkIndex(id);
        return structuredBuffer.getU16(0, id);
    }

    @Override
    public int minutes(int id) {
        checkIndex(id);
        return structuredBuffer.getU8(2, id);
    }

    @Override
    public int arrivingAt(int stationId) {
        if (stationId < 0 || stationId >= arrivingAtTable.length) {
            throw new IndexOutOfBoundsException("The id isn't valid ");
        }
        return arrivingAtTable[stationId];
    }

    @Override
    public int minutesBetween(int depStationId, int arrStationId) {

        int interval = arrivingAt(arrStationId);

        if (interval == 0){
            throw new NoSuchElementException("No transfer found between stations "
                    + depStationId + " and " + arrStationId);
        }

        int start = PackedRange.startInclusive(interval);
        int end = PackedRange.endExclusive(interval);

        for (int i = start; i < end; i++){
            if(depStationId(i) == depStationId)
                return minutes(i);
        }

        throw new NoSuchElementException("No transfer found between stations "
                + depStationId + " and " + arrStationId);
    }

    @Override
    public int size() {
        return structuredBuffer.size();
    }

    private void checkIndex(int id) {
        if (id < 0 || id >= size()) {
            throw new IndexOutOfBoundsException("The id isn't valid ");
        }
    }
}
