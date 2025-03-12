package ch.epfl.rechor.timetable.mapped;

import java.nio.ByteBuffer;
import java.util.List;

import ch.epfl.rechor.journey.Vehicle;
import ch.epfl.rechor.timetable.Routes;
import ch.epfl.rechor.timetable.Trips;

public class BufferedTrips implements Trips {
    private final List<String> stringTable;
    private final ByteBuffer buffer;
    public BufferedTrips(List<String> stringTable, ByteBuffer buffer) {
        this.stringTable = List.copyOf(stringTable);
        this.buffer = buffer.duplicate().asReadOnlyBuffer();
    }


    @Override
    public int routeId(int id){

    }

    @Override
    public String destination(int id){
        int offset = buffer.getInt(4 + id * 4);
        return stringTable.get(offset);
    }

    @Override
    public int size(){
        return stringTable.size();
    }
}
