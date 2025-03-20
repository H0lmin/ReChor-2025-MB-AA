package ch.epfl.rechor.timetable.mapped;

import ch.epfl.rechor.timetable.Platforms;

import java.nio.ByteBuffer;
import java.util.List;

/**
 * Provides access to flattened platform (voie/quai) data.
 * <p>
 * This class interprets the data using a fixed structure defined by {@code PLATFORM_STRUCTURE}.
 * </p>
 */
public final class BufferedPlatforms implements Platforms {

    private static final int NAME_ID = 0;
    private static final int STATION_ID = 1;

    private static final Structure PLATFORM_STRUCTURE = new Structure(
            Structure.field(NAME_ID, Structure.FieldType.U16),
            Structure.field(STATION_ID, Structure.FieldType.U16)
    );

    private final List<String> stringTable;
    private final StructuredBuffer buffer;

    /**
     * Constructs a {@code BufferedPlatforms} instance.
     *
     * @param stringTable the list of strings used for platform names.
     * @param buffer      the byte buffer containing the flattened platform data.
     */
    public BufferedPlatforms(List<String> stringTable, ByteBuffer buffer) {
        this.stringTable = List.copyOf(stringTable);
        this.buffer = new StructuredBuffer(PLATFORM_STRUCTURE, buffer);
    }

    /**
     * Returns the name of the platform at the specified record.
     *
     * @param id the record identifier.
     * @return the platform name.
     * @throws IndexOutOfBoundsException if {@code id} is invalid.
     */
    @Override
    public String name(int id) {
        checkIndex(id);
        int stringIndex = buffer.getU16(NAME_ID, id);
        return stringTable.get(stringIndex);
    }

    /**
     * Returns the parent station identifier for the platform at the specified record.
     *
     * @param id the record identifier.
     * @return the parent station id.
     * @throws IndexOutOfBoundsException if {@code id} is invalid.
     */
    @Override
    public int stationId(int id) {
        checkIndex(id);
        return buffer.getU16(STATION_ID, id);
    }

    /**
     * Returns the number of platform records stored in the buffer.
     *
     * @return the number of platforms.
     */
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
