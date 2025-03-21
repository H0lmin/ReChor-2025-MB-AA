package ch.epfl.rechor.timetable.mapped;

import ch.epfl.rechor.journey.Vehicle;
import ch.epfl.rechor.timetable.Routes;

import java.nio.ByteBuffer;
import java.util.List;

import static ch.epfl.rechor.journey.Vehicle.ALL;

public class BufferedRoutes implements Routes {

    private final static int NAME_ID = 0;
    private final static int KIND = 1;

    private final static Structure ROUTES_STRUCTURE = new Structure(
            Structure.field(NAME_ID, Structure.FieldType.U16),
            Structure.field(KIND, Structure.FieldType.U8)
    );
    private final List<String> stringTable;
    private final StructuredBuffer buffer;

    public BufferedRoutes(List<String> stringTable, ByteBuffer buffer) {
        this.stringTable = List.copyOf(stringTable);
        this.buffer = new StructuredBuffer(ROUTES_STRUCTURE, buffer);
    }

    @Override
    public int size() {
        return buffer.size();
    }

    @Override
    public Vehicle vehicle(int id) {
        int kind = buffer.getU8(KIND, id);
        return ALL.get(kind);
    }

    @Override
    public String name(int id) {
        int nameIndex = buffer.getU16(NAME_ID, id);
        return stringTable.get(nameIndex);
    }

}
