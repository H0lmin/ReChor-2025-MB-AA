package ch.epfl.rechor.timetable;

import java.util.NoSuchElementException;

public interface Transfers extends Indexed {

    /**
     * @throws IndexOutOfBoundsException if {@code id} is out of range
     *                                                (ie. {@code id} < 0 or
     *                                                   {@code id} >= size ()
     */
    int depStationId(int id);

    /**
     * @throws IndexOutOfBoundsException if {@code id} is out of range
     *                                                (ie. {@code id} < 0 or
     *                                                   {@code id} >= size ()
     */
    int minutes(int id);

    /**
     * @throws IndexOutOfBoundsException if {@code stationId} is out of range
     *                                                (ie. {@code stationId} < 0 or
     *                                                   {@code stationId} >= size ()
     */
    int arrivingAt(int stationId);

    /**
     * @throws NoSuchElementException if no changes are possible between the two stations
     * @throws IndexOutOfBoundsException if {@code id} is out of range
     *                                                (ie. {@code id} < 0 or
     *                                                   {@code id} >= size ()
     */
    int minutesBetween(int depStationId, int arrStationId);

}
