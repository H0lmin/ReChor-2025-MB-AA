package ch.epfl.rechor.timetable.mapped;

import ch.epfl.rechor.timetable.Stations;

import java.nio.ByteBuffer;
import java.util.List;

import static java.lang.Math.scalb;

public final class BufferedStations implements Stations {

    private final List<String> stringTable;
    private final StructuredBuffer buffer;

    private static final double UNIT_TO_DEGREES = scalb(360, -32);

    private static final int NAME_ID = 0;
    private static final int LON     = 1;
    private static final int LAT     = 2;


    private static final Structure STATION_STRUCTURE =
            new Structure(
                    Structure.field(0, Structure.FieldType.U16),
                    Structure.field(1, Structure.FieldType.S32),
                    Structure.field(2, Structure.FieldType.S32)
            );


    public BufferedStations(List<String> stringTable, ByteBuffer buffer) {
        this.stringTable = List.copyOf(stringTable);
        this.buffer = new StructuredBuffer(STATION_STRUCTURE, buffer);
    }

    @Override
    public String name(int id) {
        int stringIndex = buffer.getU16(NAME_ID, id);
        return stringTable.get(stringIndex);
    }

    @Override
    public double longitude(int id) {
        int rawLongitude = buffer.getS32(LON, id);
        return rawLongitude * UNIT_TO_DEGREES;
    }


    @Override
    public double latitude(int id) {
        int rawLatitude = buffer.getS32(LAT, id);
        return rawLatitude * UNIT_TO_DEGREES;
    }

    @Override
    public int size() {
        return buffer.size();
    }
}
