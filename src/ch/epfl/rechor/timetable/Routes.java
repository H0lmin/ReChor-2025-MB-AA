package ch.epfl.rechor.timetable;

import ch.epfl.rechor.journey.Vehicle;

/**
 * Interface representing indexed public transport routes.
 */
public interface Routes extends Indexed {
    /**
     * Returns the type of vehicle serving the route at the given index.
     * @param id the index of the route
     * @throws IndexOutOfBoundsException if the index is invalid (id < 0 or id >= size())
     */
    Vehicle vehicle(int id);

    /**
     * Returns the name of the route at the given index 
     * @param id the index of the route
     * @throws IndexOutOfBoundsException if the index is invalid (id < 0 or id >= size())
     */
    String name(int id);
}
