package ch.epfl.rechor.timetable;

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
