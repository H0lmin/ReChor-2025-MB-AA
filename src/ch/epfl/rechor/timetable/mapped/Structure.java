package ch.epfl.rechor.timetable.mapped;

import static ch.epfl.rechor.Preconditions.checkArgument;

/**
 * Defines the layout of a structured record by specifying its fields and computing their byte offsets.
 *
 * <p>A {@code Structure} is defined by an ordered sequence of {@link Field fields}, each having:
 * <ul>
 *   <li>An index matching its position in the sequence,</li>
 *   <li>A type ({@code U8}, {@code U16}, or {@code S32}).</li>
 * </ul>
 * The constructor validates the ordering and computes an array of byte offsets
 * and the total size of the record.</p>
 *
 * <p>This class is immutable and thread-safe.</p>
 *
 * @author Amine AMIRA (393410)
 * @author Malak Berrada (379791)
 */
public final class Structure {

    private final int[] fieldOffsets;
    private final int totalSize;

    /**
     * Constructs a new {@code Structure} with the specified fields.
     *
     * @param fields the fields defining the record layout, in index order
     * @throws IllegalArgumentException if any field's index does not match its position
     */
    public Structure(Field... fields) {
        for (int i = 0; i < fields.length; i++) {
            checkArgument(fields[i].index() == i);
        }

        this.fieldOffsets = new int[fields.length];
        int offset = 0;
        for (int i = 0; i < fields.length; i++) {
            fieldOffsets[i] = offset;
            int fieldSize = switch (fields[i].type()) {
                case U8 -> 1;
                case U16 -> 2;
                case S32 -> 4;
            };
            offset += fieldSize;
        }
        this.totalSize = offset;
    }

    /**
     * Creates a new {@link Field} instance with the given index and type.
     *
     * @param index the index of the field in the structure
     * @param type  the type of the field; must not be {@code null}
     * @return a new {@code Field} object
     */
    public static Field field(int index, FieldType type) {
        return new Field(index, type);
    }

    /**
     * Returns the total size (in bytes) of one record defined by this structure.
     *
     * @return the number of bytes per record
     */
    public int totalSize() {
        return totalSize;
    }

    /**
     * Computes the byte offset of a given field within a specific record element.
     *
     * @param fieldIndex   the index of the field in the structure
     * @param elementIndex the index of the record within the buffer
     * @return the byte offset of the field
     */
    public int offset(int fieldIndex, int elementIndex) {
        return elementIndex * totalSize + fieldOffsets[fieldIndex];
    }

    /**
     * Supported primitive field types with fixed byte sizes.
     */
    public enum FieldType {
        /** Unsigned 8-bit integer (1 byte) */
        U8,
        /** Unsigned 16-bit integer (2 bytes) */
        U16,
        /** Signed 32-bit integer (4 bytes) */
        S32
    }

    /**
     * Represents a single field in the record layout.
     *
     * @param index the position of the field in the structure
     * @param type  the primitive type of the field; must not be {@code null}
     */
    public record Field(int index, FieldType type) {
        /**
         * Validates that {@code type} is not null upon construction.
         *
         * @throws NullPointerException if {@code type} is null
         */
        public Field {
            if (type == null) {
                throw new NullPointerException("Field type cannot be null");
            }
        }
    }
}