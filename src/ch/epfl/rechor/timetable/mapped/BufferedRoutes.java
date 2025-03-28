package ch.epfl.rechor.timetable.mapped;

import ch.epfl.rechor.journey.Vehicle;
import ch.epfl.rechor.timetable.Routes;

import java.nio.ByteBuffer;
import java.util.List;

import static ch.epfl.rechor.journey.Vehicle.ALL;

/**
 * The BufferedRoutes class provides a buffered implementation for managing
 * route data in a public transport timetable.
 *
 * @author Amine AMIRA (393410)
 * @author Malak Berrada (379791)
 */
public class BufferedRoutes implements Routes {

    private final static int NAME_ID = 0;
    private final static int KIND = 1;

    private final static Structure ROUTES_STRUCTURE = new Structure(
            Structure.field(NAME_ID, Structure.FieldType.U16),
            Structure.field(KIND, Structure.FieldType.U8)
    );
    private final List<String> stringTable;
    private final StructuredBuffer buffer;

    /**
     * Constructs a new BufferedRoutes instance.
     *
     * @param stringTable a list of route names used for lookup based on indices.
     * @param buffer a ByteBuffer containing the raw route data arranged according to the defined structure.
     */
    public BufferedRoutes (List<String> stringTable, ByteBuffer buffer) {
        this.stringTable = List.copyOf(stringTable);
        this.buffer = new StructuredBuffer(ROUTES_STRUCTURE, buffer);
    }

    /**
     * Returns the number of route entries stored in the buffer.
     *
     * @return the total count of routes.
     */
    @Override
    public int size () {
        return buffer.size();
    }

    /**
     * Retrieves the vehicle type associated with the route at the given index.
     *
     * @param id the index of the route.
     * @return the {@link Vehicle} corresponding to the route's kind.
     */
    @Override
    public Vehicle vehicle (int id) {
        int kind = buffer.getU8(KIND, id);
        return ALL.get(kind);
    }

    /**
     * Retrieves the name of the route at the specified index.
     *
     * @param id the index of the route.
     * @return the route name as a String, obtained from the string table.
     */
    @Override
    public String name (int id) {
        int nameIndex = buffer.getU16(NAME_ID, id);
        return stringTable.get(nameIndex);
    }

}
