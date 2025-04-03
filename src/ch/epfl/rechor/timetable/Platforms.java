package ch.epfl.rechor.timetable;

/**
 * Interface representing indexed tracks/platforms.
 *
 * @author Amine AMIRA (393410)
 * @author Malak Berrada (379791)
 */
public interface Platforms extends Indexed {
    /**
     * Returns the name of the platform
     *
     * @param id the index of the platform
     * @return the name
     * @throws IndexOutOfBoundsException if the index is invalid (id < 0 or id >= size())
     */
    String name(int id);

    /**
     * Returns the index of the station to which the platform at the given index belongs.
     *
     * @param id the index of the track/platform
     * @return the stationId
     * @throws IndexOutOfBoundsException if the index is invalid (id < 0 or id >= size())
     */
    int stationId(int id);
}
