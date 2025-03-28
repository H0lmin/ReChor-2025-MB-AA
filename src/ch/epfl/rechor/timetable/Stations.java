package ch.epfl.rechor.timetable;

/**
 * Interface representing indexed stations
 *
 * @author Amine AMIRA (393410)
 * @author Malak Berrada (379791)
 */
public interface Stations extends Indexed {
    /**
     * Returns the name of the given indexed station
     *
     * @param id the station index
     * @throws IndexOutOfBoundsException if id < 0 or id >= size()
     * @return the name of the station
     **/
    String name (int id);

    /**
     * Returns the longitude in degrees of the given indexed station
     *
     * @param id the station index
     * @throws IndexOutOfBoundsException if id < 0 or id >= size()
     * @return the longitude
     */
    double longitude (int id);

    /**
     * Returns the latitude in degrees of the given indexed station
     *
     * @param id the station index
     * @throws IndexOutOfBoundsException if id < 0 or id >= size()
     * @return the latitude
     */
    double latitude (int id);
}
