package ch.epfl.rechor.timetable.mapped;

import ch.epfl.rechor.Bits32_24_8;
import ch.epfl.rechor.timetable.Connections;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

/**
 * @author Amine AMIRA (393410)
 * @author Malak Berrada (379791)
 */
public class BufferedConnections implements Connections {

    private final static int DEP_STOP_ID = 0;
    private final static int DEP_MINUTES = 1;
    private final static int ARR_STOP_ID = 2;
    private final static int ARR_MINUTES = 3;
    private final static int TRIP_POS_ID = 4;

    private final StructuredBuffer structuredBuffer;
    private final IntBuffer nextBuffer;

    private final static Structure CONNECTIONS_STRUCTURE = new Structure(
            Structure.field(DEP_STOP_ID, Structure.FieldType.U16),
            Structure.field(DEP_MINUTES, Structure.FieldType.U16),
            Structure.field(ARR_STOP_ID, Structure.FieldType.U16),
            Structure.field(ARR_MINUTES, Structure.FieldType.U16),
            Structure.field(TRIP_POS_ID, Structure.FieldType.S32)
    );

    public BufferedConnections (ByteBuffer buffer, ByteBuffer succBuffer){
        this.structuredBuffer = new StructuredBuffer(CONNECTIONS_STRUCTURE, buffer) ;
        this.nextBuffer = succBuffer.slice().asIntBuffer();
    }

    @Override
    public int depStopId(int id) {
        return structuredBuffer.getU16(DEP_STOP_ID, id);
    }

    @Override
    public int depMins(int id) {
        return structuredBuffer.getU16(DEP_MINUTES, id);
    }

    @Override
    public int arrStopId(int id) {
        return structuredBuffer.getU16(ARR_STOP_ID, id);
    }

    @Override
    public int arrMins(int id) {
        return structuredBuffer.getU16(ARR_MINUTES, id);
    }

    @Override
    public int tripId(int id) {
        int packed = structuredBuffer.getS32(TRIP_POS_ID, id);
        return Bits32_24_8.unpack24(packed);
    }

    @Override
    public int tripPos(int id) {
        int packed = structuredBuffer.getS32(TRIP_POS_ID, id);
        return Bits32_24_8.unpack8(packed);
    }

    @Override
    public int nextConnectionId(int id) {
        return nextBuffer.get(id);
    }

    @Override
    public int size() {
        return structuredBuffer.size();
    }

}
