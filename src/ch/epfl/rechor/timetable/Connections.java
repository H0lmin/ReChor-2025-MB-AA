package ch.epfl.rechor.timetable;

public interface Connections extends Indexed {

    /**
     * Returns the departure stop ID for the connection at the specified index.
     * <p>
     * This stop ID may represent either a station or a platform/track, depending
     * on its value relative to the number of stations.
     *
     * @param id the index of the connection
     * @return the departure stop ID
     * @throws IndexOutOfBoundsException if {@code id} is out of range
     *                                                    (i.e. {@code id} < 0 or
     *                                                    {@code id} >= size ()
     */

    int depStopId(int id);

    /**
     * Returns the departure time of the connection at the specified index,
     * expressed in minutes after midnight.
     *
     * @param id the index of the connection
     * @return the departure time in minutes after midnight
     * @throws IndexOutOfBoundsException if {@code id} is out of range
     *                                                    (i.e. {@code id} < 0 or
     *                                                    {@code id} >= size ()
     */
    int depMins(int id);

    /**
     * Returns the arrival stop ID for the connection at the specified index.
     * <p>
     * This stop ID may represent either a station or a platform/track, depending
     * on its value relative to the number of stations.
     *
     * @param id the index of the connection
     * @return the arrival stop ID
     * @throws IndexOutOfBoundsException if {@code id} is out of range
     *                                                    (i.e. {@code id} < 0 or
     *                                                    {@code id} >= size ()
     */
    int arrStopId(int id);

    /**
     * Returns the arrival time of the connection at the specified index,
     * expressed in minutes after midnight.
     *
     * @param id the index of the connection
     * @return the arrival time in minutes after midnight
     * @throws IndexOutOfBoundsException if {@code id} is out of range
     *                                                    (i.e. {@code id} < 0 or
     *                                                    {@code id} >= size ()     */
    int arrMins(int id);

    /**
     * Returns the ID of the trip (or service) to which the connection at
     * the specified index belongs.
     * <p>
     * Multiple connections sharing the same trip ID are part of the same
     * journey or service.
     *
     * @param id the index of the connection
     * @return the trip ID for the connection
     * @throws IndexOutOfBoundsException if {@code id} is out of range
     *                                                    (ie. {@code id} < 0 or
     *                                                    {@code id} >= size ()
     */
    int tripId(int id);

    /**
     * Returns the position of the connection within its trip for the
     * connection at the specified index.
     * <p>
     * A position of 0 indicates the first connection of the trip, 1 the second,
     * and so on.
     *
     * @param id the index of the connection
     * @return the position of the connection within the trip
     * @throws IndexOutOfBoundsException if {@code id} is out of range
     *                                                    (ie. {@code id} < 0 or
     *                                                    {@code id} >= size ()     */
    int tripPos(int id);

    /**
     * Returns the index of the next connection in the same trip for
     * the connection at the specified index, or -1 if there is no subsequent
     * connection in that trip.
     *
     * @param id the index of the connection
     * @return the index of the next connection in the same trip, or -1 if none
     * @throws IndexOutOfBoundsException if {@code id} is out of range
     *                                                    (ie. {@code id} < 0 or
     *                                                    {@code id} >= size ()
     */
    int nextConnectionId(int id);
}
