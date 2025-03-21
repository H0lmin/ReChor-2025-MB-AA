package ch.epfl.rechor.timetable;

/**
 * Interface representing indexed public transport trips.
 */
public interface Trips extends Indexed {
    /**
     * Returns the index of the route to which the indexed trip belongs
     * @param id the index of the trip
     * @throws IndexOutOfBoundsException if the index is invalid (id < 0 or id >= size())
     */
    int routeId(int id);

    /**
     * Returns the name of the destination
     * @param id the index of the trip
     * @throws IndexOutOfBoundsException if the index is invalid (id < 0 or id >= size())
     */
    String destination(int id);
}
