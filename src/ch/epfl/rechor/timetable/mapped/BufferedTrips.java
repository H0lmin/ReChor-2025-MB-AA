package ch.epfl.rechor.timetable.mapped;

import ch.epfl.rechor.timetable.Trips;

import java.nio.ByteBuffer;
import java.util.List;

/**
 * Provides access to flattened data about a table of trips.
 *
 * @author Amine AMIRA (393410)
 * @author Malak Berrada (379791)
 */
public class BufferedTrips implements Trips {

    private final static int ROUTE_ID = 0;
    private final static int DESTINATION_ID = 1;

    private final static Structure TRIPS_STRUCTURE = new Structure(
            Structure.field(ROUTE_ID, Structure.FieldType.U16),
            Structure.field(DESTINATION_ID, Structure.FieldType.U16)
    );

    private final List<String> stringTable;
    private final StructuredBuffer buffer;

    public BufferedTrips(List<String> stringTable, ByteBuffer buffer) {
        this.stringTable = List.copyOf(stringTable);
        this.buffer = new StructuredBuffer(TRIPS_STRUCTURE, buffer);
    }

    /**
     * Returns the index of the route to which the indexed trip belongs
     *
     * @param id the index of the trip
     * @throws IndexOutOfBoundsException if the index is invalid (id < 0 or id >= size())
     */
    @Override
    public int routeId(int id) {
        return buffer.getU16(ROUTE_ID, id);
    }

    /**
     * Returns the name of the destination
     *
     * @param id the index of the trip
     * @throws IndexOutOfBoundsException if the index is invalid (id < 0 or id >= size())
     */
    @Override
    public String destination(int id) {
        int destinationId = buffer.getU16(DESTINATION_ID, id);
        return stringTable.get(destinationId);
    }

    /**
     * Returns the number of trips stored in the buffer.
     *
     * @return the number of trips.
     */
    @Override
    public int size() {
        return buffer.size();
    }

}