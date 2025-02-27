package ch.epfl.rechor.timetable.mapped;

import java.nio.ByteBuffer;
import static ch.epfl.rechor.Preconditions.checkArgument;

public final class StructuredBuffer {
    private final Structure structure;
    private final ByteBuffer buffer;
    private final int size;

    public StructuredBuffer(Structure structure, ByteBuffer buffer) {
        this.structure = structure;
        this.buffer = buffer.slice();
        int elementSize = structure.totalSize();
        int capacity = this.buffer.capacity();
        checkArgument(capacity % elementSize == 0);
        this.size = capacity / elementSize;
    }

    public int size() {
        return size;
    }

    public int getU8(int fieldIndex, int elementIndex) {
        int offset = structure.offset(fieldIndex, elementIndex);
        byte value = buffer.get(offset);
        return Byte.toUnsignedInt(value);
    }

    public int getU16(int fieldIndex, int elementIndex) {
        int offset = structure.offset(fieldIndex, elementIndex);
        short value = buffer.getShort(offset);
        return Short.toUnsignedInt(value);
    }

    public int getS32(int fieldIndex, int elementIndex) {
        int offset = structure.offset(fieldIndex, elementIndex);
        return buffer.getInt(offset);
    }
}
