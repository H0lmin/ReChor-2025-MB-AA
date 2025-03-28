package ch.epfl.rechor.timetable;

/**
 * Interface representing stations of the trip.
 *
 * @author Amine AMIRA (393410)
 * @author Malak Berrada (379791)
 */

public interface Stations extends Indexed {
    /**
     * returns the name of the given indexed station
     * @param id the station index
     * @throws IndexOutOfBoundsException if id < 0 or id >= size()
     **/
    String name(int id);

    /**
     * returns the longitude in degrees of the given indexed station
     * @param id the station index
     * @throws IndexOutOfBoundsException if id < 0 or id >= size()
     */
    double longitude(int id);

    /**
     * returns the latitude in degrees of the given indexed station
     * @param id the station index
     * @throws IndexOutOfBoundsException if id < 0 or id >= size()
     */
    double latitude(int id);
}
