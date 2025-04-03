package ch.epfl.rechor.timetable;

/**
 * Interface representing connections between stations.
 *
 * @author Amine AMIRA (393410)
 * @author Malak Berrada (379791)
 */
public interface Connections extends Indexed {

    /**
     * Returns the departure stop ID for the connection at the specified index.
     *
     * @param id the index of the connection
     * @throws IndexOutOfBoundsException if the id is out of range
     */

    int depStopId(int id);

    /**
     * Returns the departure time of the connection at the specified index
     *
     * @param id the index of the connection
     * @return the departure time
     * @throws IndexOutOfBoundsException if the id is out of range
     */
    int depMins(int id);

    /**
     * Returns the arrival stop ID for the connection at the specified index
     *
     * @param id the index of the connection
     * @return the arrival stop ID
     * @throws IndexOutOfBoundsException if the id is out of range
     */
    int arrStopId(int id);

    /**
     * Returns the arrival time of the connection at the specified index
     *
     * @param id the index of the connection
     * @return the arrival time
     * @throws IndexOutOfBoundsException if the id is out of range
     */
    int arrMins(int id);

    /**
     * Returns the ID of the trip
     *
     * @param id the index of the connection
     * @return the tripId
     * @throws IndexOutOfBoundsException if the id is out of range
     */
    int tripId(int id);

    /**
     * Returns the position of the connection within its trip for the connection at the specified
     * index.
     *
     * @param id the index of the connection
     * @return the position of the trip
     * @throws IndexOutOfBoundsException if the id is out of range
     */
    int tripPos(int id);

    /**
     * Returns the index of the next connection in the same trip
     *
     * @param id the index of the connection
     * @return the nexConnectionId
     * @throws IndexOutOfBoundsException if the id is out of range
     */
    int nextConnectionId(int id);
}
