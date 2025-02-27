package ch.epfl.rechor.timetable;

public interface Platforms extends Indexed {
    /**
     * Returns the name of the track or platform at the specified index.
     *
     * @param id the index of the track/platform name
     * @return the name of the track/platform at the given index
     * @throws IndexOutOfBoundsException if id < 0 or id >= size()
     */
    String name(int id);

    /**
     * Returns the index of the station to which the track or platform at
     * the specified index belongs.
     *
     * @param id the index of the station
     * @return the station index corresponding to the given track/platform index
     * @throws IndexOutOfBoundsException if id < 0 or id >= size()
     */
    int stationId(int id);
}
