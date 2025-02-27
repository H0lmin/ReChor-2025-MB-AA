package ch.epfl.rechor.timetable.mapped;

import static ch.epfl.rechor.Preconditions.checkArgument;

public final class Structure {

    private final Field[] fields;
    private final int[] fieldOffsets;
    private final int totalSize;

    public Structure(Field... fields) {

        for (int i = 0; i < fields.length; i++) {
            checkArgument(fields[i].index() == i);
        }
        this.fields = fields.clone();

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

    public static Field field(int index, FieldType type) {
        return new Field(index, type);
    }

    public int totalSize() {
        return totalSize;
    }

    public int offset(int fieldIndex, int elementIndex) {
        return elementIndex * totalSize + fieldOffsets[fieldIndex];
    }

    public enum FieldType {
        U8,
        U16,
        S32
    }

    public record Field(int index, FieldType type) {
        public Field {
            if(type == null){
                throw new NullPointerException("the type cannot be null");
            }
        }
    }
}
