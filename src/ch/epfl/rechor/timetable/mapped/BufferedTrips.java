package ch.epfl.rechor.timetable.mapped;

import java.nio.ByteBuffer;
import java.util.List;

import ch.epfl.rechor.timetable.Trips;

public class BufferedTrips implements Trips {
    private final List<String> stringTable;
    private final StructuredBuffer buffer;

    private final static Structure TRIPS_STRUCTURE = new Structure(
            Structure.field(0, Structure.FieldType.U16),
            Structure.field(1, Structure.FieldType.U16)
    );

    public BufferedTrips(List<String> stringTable, ByteBuffer buffer) {
        this.stringTable = List.copyOf(stringTable);
        this.buffer = new StructuredBuffer(TRIPS_STRUCTURE, buffer);
    }


    @Override
    public int routeId(int id){
        checkIndex(id);
        return buffer.getU16(0, id);
    }

    @Override
    public String destination(int id){
        checkIndex(id);
        int destinationId = buffer.getU16(1, id);
        return stringTable.get(destinationId);
    }

    @Override
    public int size(){
        return buffer.size();
    }

    private void checkIndex(int id) {
        if (id < 0 || id >= size()) {
            throw new IndexOutOfBoundsException("The id isn't valid ");
        }
    }
}