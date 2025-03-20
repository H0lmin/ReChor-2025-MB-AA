package ch.epfl.rechor.timetable.mapped;

import static ch.epfl.rechor.Preconditions.checkArgument;

/**
 * Describes the layout of a record in a flattened timetable.
 * <p>
 * A Structure defines the order, type, and byte offset of each field in a record.
 * </p>
 * <p>
 * Fields are provided in order (index 0 for the first field, 1 for the second, etc.). Supported
 * field types are:
 * <ul>
 *   <li><strong>U8</strong> – unsigned 8-bit (1 byte)</li>
 *   <li><strong>U16</strong> – unsigned 16-bit (2 bytes)</li>
 *   <li><strong>S32</strong> – signed 32-bit (4 bytes)</li>
 * </ul>
 * The class precomputes the offset of each field and the total record size.
 * </p>
 *
 * @author Amine AMIRA (393410)
 * @author Malak Berrada (379791)
 */
public final class Structure {

    private final int[] fieldOffsets;
    private final int totalSize;

    /**
     * Constructs a new {@code Structure} with the specified fields.
     * <p>
     * The fields must be provided in order. In particular, the field at position <em>i</em> must have an index of
     * <em>i</em>.
     * </p>
     * @param fields the fields that define the record layout.
     * @throws IllegalArgumentException if the fields are not provided in order (i.e., if a field's index does not match its position).
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
     * Creates a new {@code Field} instance with the given index and type.
     *
     * @param index the index of the field in the structure .
     * @param type  the type of the field; must not be {@code null}.
     * @return a new {@code Field} instance.
     */
    public static Field field(int index, FieldType type) {
        return new Field(index, type);
    }

    /**
     * Returns the total size in bytes of one record defined by this structure.
     *
     * @return the total number of bytes in a record.
     */
    public int totalSize() {
        return totalSize;
    }

    /**
     * Returns the offset (in bytes) of a specific field for the element at the given index in a binary array.
     *
     * @param fieldIndex   the index of the field in the structure.
     * @param elementIndex the index of the record within the binary array.
     * @return the byte offset of the specified field.
     */
    public int offset(int fieldIndex, int elementIndex) {
        return elementIndex * totalSize + fieldOffsets[fieldIndex];
    }

    /**
     * Enumerates the supported field types.
     */
    public enum FieldType {
        U8,
        U16,
        S32
    }

    /**
     * Represents a field in a flattened record.
     *
     * @param index the index of the field within the record.
     * @param type  the type of the field; must not be {@code null}.
     */
    public record Field(int index, FieldType type) {
        /**
         * Constructs a new {@code Field} with the specified index and type.
         *
         * @param index the index of the field.
         * @param type  the type of the field.
         * @throws NullPointerException if {@code type} is {@code null}.
         */
        public Field {
            if(type == null){
                throw new NullPointerException("the type cannot be null");
            }
        }
    }
}
