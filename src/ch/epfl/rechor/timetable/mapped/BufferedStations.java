package ch.epfl.rechor.timetable.mapped;

import ch.epfl.rechor.timetable.Stations;

import java.nio.ByteBuffer;
import java.util.List;

import static java.lang.Math.scalb;

/**
 * Implements the {@code Stations} interface to provide access to flattened station data.
 * <p>
 * Each station record contains:
 *   <li>A U16 field for the index into the string table for the station name,</li>
 *   <li>An S32 field for the station's longitude, and</li>
 *   <li>An S32 field for the station's latitude.</li>
 * </p>
 *
 * @author Amine AMIRA (393410)
 * @author Malak Berrada (379791)
 */
public final class BufferedStations implements Stations {

    private static final double UNIT_TO_DEGREES = scalb(360, -32);

    private static final int NAME_ID = 0;
    private static final int LON = 1;
    private static final int LAT = 2;

    private static final Structure STATION_STRUCTURE =
            new Structure(
                    Structure.field(NAME_ID, Structure.FieldType.U16),
                    Structure.field(LON, Structure.FieldType.S32),
                    Structure.field(LAT, Structure.FieldType.S32)
            );

    private final List<String> stringTable;
    private final StructuredBuffer buffer;


    /**
     * Constructs a {@code BufferedStations} instance from the given string table and byte buffer.
     *
     * @param stringTable the list of strings used for station names.
     * @param buffer      the byte buffer containing the flattened station data.
     */
    public BufferedStations(List<String> stringTable, ByteBuffer buffer) {
        this.stringTable = List.copyOf(stringTable);
        this.buffer = new StructuredBuffer(STATION_STRUCTURE, buffer);
    }

    /**
     * Returns the name of the station corresponding to the given station identifier.
     *
     * @param id the station identifier.
     * @return the station name.
     * @throws IndexOutOfBoundsException if {@code id} is invalid.
     */
    @Override
    public String name(int id) {
        int stringIndex = buffer.getU16(NAME_ID, id);
        return stringTable.get(stringIndex);
    }

    /**
     * Returns the longitude of the station in degrees.
     *
     * @param id the station identifier.
     * @return the longitude in degrees.
     * @throws IndexOutOfBoundsException if {@code id} is invalid.
     */
    @Override
    public double longitude(int id) {
        int rawLongitude = buffer.getS32(LON, id);
        return rawLongitude * UNIT_TO_DEGREES;
    }

    /**
     * Returns the latitude of the station in degrees.
     *
     * @param id the station identifier.
     * @return the latitude in degrees.
     * @throws IndexOutOfBoundsException if {@code id} is invalid.
     */
    @Override
    public double latitude(int id) {
        int rawLatitude = buffer.getS32(LAT, id);
        return rawLatitude * UNIT_TO_DEGREES;
    }

    /**
     * Returns the total number of stations in the buffer.
     *
     * @return the number of stations.
     */
    @Override
    public int size() {
        return buffer.size();
    }
}
