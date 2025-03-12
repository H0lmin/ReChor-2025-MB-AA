package ch.epfl.rechor.timetable.mapped;

import ch.epfl.rechor.Bits32_24_8;
import ch.epfl.rechor.timetable.Connections;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

public class BufferedConnections implements Connections {
    private final StructuredBuffer structuredBuffer;

    private final IntBuffer nextBuffer;

    private final static Structure CONNECTIONS_STRUCTURE = new Structure(
            Structure.field(0, Structure.FieldType.U16),
            Structure.field(1, Structure.FieldType.U16),
            Structure.field(2, Structure.FieldType.U16),
            Structure.field(3, Structure.FieldType.U16),
            Structure.field(4, Structure.FieldType.S32)
    );

    public BufferedConnections (ByteBuffer buffer, ByteBuffer succBuffer){
        this.structuredBuffer = new StructuredBuffer(CONNECTIONS_STRUCTURE, buffer) ;
        this.nextBuffer = succBuffer.slice().asIntBuffer();
    }

    @Override
    public int depStopId(int id) {
        checkIndex(id);
        return structuredBuffer.getU16(0, id);
    }

    @Override
    public int depMins(int id) {
        checkIndex(id);
        return structuredBuffer.getU16(1, id);
    }

    @Override
    public int arrStopId(int id) {
        checkIndex(id);
        return structuredBuffer.getU16(2, id);
    }

    @Override
    public int arrMins(int id) {
        checkIndex(id);
        return structuredBuffer.getU16(3, id);
    }

    @Override
    public int tripId(int id) {
        checkIndex(id);
        int packed = structuredBuffer.getS32(4, id);
        return Bits32_24_8.unpack24(packed);
    }

    @Override
    public int tripPos(int id) {
        checkIndex(id);
        int packed = structuredBuffer.getS32(4, id);
        return Bits32_24_8.unpack8(packed);
    }

    @Override
    public int nextConnectionId(int id) {
        checkIndex(id);
        return nextBuffer.get(id);
    }

    @Override
    public int size() {
        return structuredBuffer.size();
    }

    private void checkIndex(int id) {
        if (id < 0 || id >= size()) {
            throw new IndexOutOfBoundsException("The id isn't valid ");
        }
    }
}
