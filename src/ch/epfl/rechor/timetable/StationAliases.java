package ch.epfl.rechor.timetable;

/**
 * Interface representing indexed stations' aliases
 *
 * @author Amine AMIRA (393410)
 * @author Malak Berrada (379791)
 */
public interface StationAliases extends Indexed {
    /**
     * Returns the alternative name of the given index
     *
     * @param id the index of the alternative station
     * @throws IndexOutOfBoundsException if the index is out of range
     */
    String alias (int id);

    /**
     * Returns the name of the station
     *
     * @param id the index of the primary station
     * @throws IndexOutOfBoundsException if id < 0 or id >= size()
     */
    String stationName (int id);
}
