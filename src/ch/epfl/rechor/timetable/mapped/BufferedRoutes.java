package ch.epfl.rechor.timetable.mapped;

import java.nio.ByteBuffer;
import java.util.List;

import ch.epfl.rechor.journey.Vehicle;
import ch.epfl.rechor.timetable.Routes;

import static ch.epfl.rechor.journey.Vehicle.ALL;

public final class BufferedRoutes implements Routes {
    private final List<String> stringTable;
    private final ByteBuffer buffer;

    public BufferedRoutes(List<String> stringTable, ByteBuffer buffer) {
        this.stringTable = List.copyOf(stringTable);
        this.buffer = buffer.duplicate().asReadOnlyBuffer();
    }
    @Override
    public Vehicle vehicle(int id){
        int offset = buffer.getInt(4 + id * 4);
        byte b = buffer.get(offset);
        return ALL.get(b);
    }

    @Override
    public String name(int id){
        int offset = buffer.getInt(4 + id * 4);
        return stringTable.get(offset);
    }

    @Override
    public int size(){
        return stringTable.size();
    }

}
