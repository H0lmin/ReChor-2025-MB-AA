package ch.epfl.rechor.timetable;

import ch.epfl.rechor.journey.Vehicle;

public interface Routes extends Indexed {
    /**
     * Returns the type of vehicle serving the route at the specified index.
     *
     * @param id the index of the route
     * @return the vehicle serving the route at the given index
     * @throws IndexOutOfBoundsException if id < 0 or id >= size()
     */
    Vehicle vehicle(int id);

    /**
     * Returns the name of the route at the specified index.
     *
     * @param id the index of the route
     * @return the name of the route at the given index
     * @throws IndexOutOfBoundsException if id < 0 or id >= size()
     */
    String name(int id);
}
