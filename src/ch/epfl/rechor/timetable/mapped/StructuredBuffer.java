package ch.epfl.rechor.timetable.mapped;

import java.nio.ByteBuffer;

import static ch.epfl.rechor.Preconditions.checkArgument;

/**
 * A {@code StructuredBuffer} provides a structured view over a byte array that stores flattened timetable data.
 * <p>
 * The layout of a single record is defined by a {@link Structure} which specifies
 * the order, type, and byte size of each field.
 * </p>
 *
 * @author Amine AMIRA (393410)
 * @author Malak Berrada (379791)
 */
public final class StructuredBuffer {
    private final Structure structure;
    private final ByteBuffer buffer;
    private final int size;

    /**
     * Constructs a {@code StructuredBuffer} with the specified structure and byte buffer.
     *
     * @param structure the {@link Structure} describing the layout of a record.
     * @param buffer    the {@link ByteBuffer} containing the flattened data.
     * @throws IllegalArgumentException if the buffer's capacity is not a multiple of the record size.
     */
    public StructuredBuffer (Structure structure, ByteBuffer buffer) {
        this.structure = structure;
        this.buffer = buffer.slice();

        int elementSize = structure.totalSize();
        int capacity = this.buffer.capacity();

        checkArgument(capacity % elementSize == 0);
        this.size = capacity / elementSize;
    }

    /**
     * Returns the number of records stored in the buffer.
     *
     * @return the number of elements.
     */
    public int size () {
        return size;
    }

    /**
     * Returns the unsigned 8-bit value from the specified field and record.
     *
     * @param fieldIndex   the index of the field in the structure.
     * @param elementIndex the index of the record.
     * @return the unsigned 8-bit integer (0 ≤ value < 256).
     */
    public int getU8 (int fieldIndex, int elementIndex) {
        int offset = structure.offset(fieldIndex, elementIndex);
        return Byte.toUnsignedInt(buffer.get(offset));
    }

    /**
     * Returns the unsigned 16-bit value from the specified field and record.
     *
     * @param fieldIndex   the index of the field in the structure.
     * @param elementIndex the index of the record.
     * @return the unsigned 16-bit integer (0 ≤ value < 65 536).
     */
    public int getU16 (int fieldIndex, int elementIndex) {
        int offset = structure.offset(fieldIndex, elementIndex);
        return Short.toUnsignedInt(buffer.getShort(offset));
    }

    /**
     * Returns the signed 32-bit value from the specified field and record.
     *
     * @param fieldIndex   the index of the field in the structure.
     * @param elementIndex the index of the record.
     * @return the signed 32-bit integer.
     */
    public int getS32 (int fieldIndex, int elementIndex) {
        int offset = structure.offset(fieldIndex, elementIndex);
        return buffer.getInt(offset);
    }
}
