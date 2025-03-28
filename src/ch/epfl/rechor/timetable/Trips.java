package ch.epfl.rechor.timetable;

/**
 * Interface representing indexed public transport trips.
 *
 * @author Amine AMIRA (393410)
 * @author Malak Berrada (379791)
 */
public interface Trips extends Indexed {
    /**
     * Returns the index of the route to which the indexed trip belongs
     *
     * @param id the index of the trip
     * @throws IndexOutOfBoundsException if the index is invalid (id < 0 or id >= size())
     * @return the index of the route
     */
    int routeId(int id);

    /**
     * Returns the name of the destination
     * @param id the index of the trip
     * @throws IndexOutOfBoundsException if the index is invalid (id < 0 or id >= size())
     * @return the destination's name
     */
    String destination(int id);
}
