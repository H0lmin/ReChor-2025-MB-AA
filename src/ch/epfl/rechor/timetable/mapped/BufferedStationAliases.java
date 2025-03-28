package ch.epfl.rechor.timetable.mapped;

import ch.epfl.rechor.timetable.StationAliases;

import java.nio.ByteBuffer;
import java.util.List;

/**
 * Provides access to flattened station aliases data.
 *
 * @author Amine AMIRA (393410)
 * @author Malak Berrada (379791)
 */
public final class BufferedStationAliases implements StationAliases {

    private static final int ALIAS_ID = 0;
    private static final int STATION_NAME_ID = 1;

    private static final Structure ALIASES_STRUCTURE = new Structure(
            Structure.field(ALIAS_ID, Structure.FieldType.U16),
            Structure.field(STATION_NAME_ID, Structure.FieldType.U16)
    );

    private final List<String> stringTable;
    private final StructuredBuffer buffer;

    /**
     * Constructs a BufferedStationAliases instance.
     *
     * @param stringTable the list of strings for alias and station names.
     * @param buffer      the byte buffer containing the flattened alias data.
     */
    public BufferedStationAliases (List<String> stringTable, ByteBuffer buffer) {
        this.stringTable = List.copyOf(stringTable);
        this.buffer = new StructuredBuffer(ALIASES_STRUCTURE, buffer);
    }

    /**
     * Returns the alias name for the station alias record with the given identifier.
     *
     * @param id the identifier of the alias record.
     * @return the station alias name.
     * @throws IndexOutOfBoundsException if {@code id} is invalid.
     */
    @Override
    public String alias (int id) {
        int stringIndex = buffer.getU16(ALIAS_ID, id);
        return stringTable.get(stringIndex);
    }

    /**
     * Returns the station name associated with the alias record with the given identifier.
     *
     * @param id the identifier of the alias record.
     * @return the  station name.
     * @throws IndexOutOfBoundsException if {@code id} is invalid.
     */
    @Override
    public String stationName (int id) {
        int stringIndex = buffer.getU16(STATION_NAME_ID, id);
        return stringTable.get(stringIndex);
    }

    /**
     * Returns the total number of alias records.
     *
     * @return the number of alias records.
     */
    @Override
    public int size () {
        return buffer.size();
    }

}
