package ch.epfl.rechor.timetable.mapped;

import ch.epfl.rechor.Bits32_24_8;
import ch.epfl.rechor.timetable.Connections;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

/**
 * Provides access to flattened data about a table of connections.
 *
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
    /**
     * Constructs a {@code BufferedConnections} instance.
     *
     * @param buffer      the byte buffer containing the flattened platform data.
     * @param succBuffer  the byte buffer containing the next flattened data
     */
    public BufferedConnections (ByteBuffer buffer, ByteBuffer succBuffer){
        this.structuredBuffer = new StructuredBuffer(CONNECTIONS_STRUCTURE, buffer) ;
        this.nextBuffer = succBuffer.slice().asIntBuffer();
    }

    /**
     * Returns the departure stop ID for the connection at the specified index.
     * @param id the index of the connection
     * @throws IndexOutOfBoundsException if the id is out of range
     */
    @Override
    public int depStopId(int id) {
        return structuredBuffer.getU16(DEP_STOP_ID, id);
    }

    /**
     * Returns the departure time of the connection at the specified index
     * @param id the index of the connection
     * @throws IndexOutOfBoundsException if the id is out of range
     */
    @Override
    public int depMins(int id) {
        return structuredBuffer.getU16(DEP_MINUTES, id);
    }

    /**
     * Returns the arrival stop ID for the connection at the specified index
     * @param id the index of the connection
     * @throws IndexOutOfBoundsException if the id is out of range
     */
    @Override
    public int arrStopId(int id) {
        return structuredBuffer.getU16(ARR_STOP_ID, id);
    }

    /**
     * Returns the arrival time of the connection at the specified index
     * @param id the index of the connection
     * @throws IndexOutOfBoundsException if the id is out of range
     */
    @Override
    public int arrMins(int id) {
        return structuredBuffer.getU16(ARR_MINUTES, id);
    }

    /**
     * Returns the ID of the trip
     * @param id the index of the connection
     * @throws IndexOutOfBoundsException if the id is out of range
     */
    @Override
    public int tripId(int id) {
        int packed = structuredBuffer.getS32(TRIP_POS_ID, id);
        return Bits32_24_8.unpack24(packed);
    }

    /**
     * Returns the position of the connection within its trip for the
     * connection at the specified index.
     * @param id the index of the connection
     * @throws IndexOutOfBoundsException if the id is out of range
     */
    @Override
    public int tripPos(int id) {
        int packed = structuredBuffer.getS32(TRIP_POS_ID, id);
        return Bits32_24_8.unpack8(packed);
    }

    /**
     * Returns the index of the next connection in the same trip
     * @param id the index of the connection
     * @throws IndexOutOfBoundsException if the id is out of range
     */
    @Override
    public int nextConnectionId(int id) {
        return nextBuffer.get(id);
    }

    /**
     * Returns the number of connections stored in the buffer.
     *
     * @return the number of connections.
     */
    @Override
    public int size() {
        return structuredBuffer.size();
    }

}
