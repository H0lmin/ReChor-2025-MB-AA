package ch.epfl.rechor.timetable;

import java.util.NoSuchElementException;

/**
 * Interface representing indexed transfers between stations.
 *
 * @author Amine AMIRA (393410)
 * @author Malak Berrada (379791)
 */
public interface Transfers extends Indexed {
    /**
     * Returns the index of the departure station for the transfer at the given index.
     *
     * @param id the index of the transfer
     * @throws IndexOutOfBoundsException if the index is invalid (id < 0 or id >= size())
     * @return the index of the departure station
     */
    int depStationId (int id);

    /**
     * Returns the duration, in minutes, of the transfer at the given index.
     *
     * @param id the index of the transfer
     * @throws IndexOutOfBoundsException if the index is invalid (id < 0 or id >= size())
     * @return the duration of the transfer
     */
    int minutes (int id);

    /**
     * Returns the packed interval of transfer indices whose arrival station is the one at the given index
     *
     * @param stationId the index of the arrival station
     * @throws IndexOutOfBoundsException if the index is invalid (stationId < 0 or stationId >= size())
     * @return the interval of transfer indices
     */
    int arrivingAt (int stationId);

    /**
     * Returns the duration of the transfer between the two stations at the given indices
     *
     * @param depStationId the index of the departure station
     * @param arrStationId the index of the arrival station
     * @throws IndexOutOfBoundsException if either index is invalid
     * @throws NoSuchElementException    if no changes are possible between the two stations
     * @return the duration of change between the departure station and station of arrival
     */
    int minutesBetween (int depStationId, int arrStationId);
}
