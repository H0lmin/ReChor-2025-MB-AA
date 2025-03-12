package ch.epfl.rechor.timetable.mapped;

import ch.epfl.rechor.timetable.StationAliases;

import java.nio.ByteBuffer;
import java.util.List;

public final class BufferedStationAliases implements StationAliases {

    private static final int ALIAS_ID = 0;
    private static final int STATION_NAME_ID = 1;

    private static final Structure ALIASES_STRUCTURE = new Structure(
            Structure.field(ALIAS_ID, Structure.FieldType.U16),
            Structure.field(STATION_NAME_ID, Structure.FieldType.U16)
    );

    private final List<String> stringTable;
    private final StructuredBuffer buffer;

    public BufferedStationAliases(List<String> stringTable, ByteBuffer buffer) {
        this.stringTable = List.copyOf(stringTable);
        this.buffer = new StructuredBuffer(ALIASES_STRUCTURE, buffer);
    }

    @Override
    public String alias(int id) {
        checkIndex(id);
        int stringIndex = buffer.getU16(ALIAS_ID, id);
        return stringTable.get(stringIndex);
    }

    @Override
    public String stationName(int id) {
        checkIndex(id);
        int stringIndex = buffer.getU16(STATION_NAME_ID, id);
        return stringTable.get(stringIndex);
    }

    @Override
    public int size() {
        return buffer.size();
    }

    private void checkIndex(int id) {
        if (id < 0 || id >= size()) {
            throw new IndexOutOfBoundsException("The id isn't valid ");
        }
    }
}
