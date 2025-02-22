package ch.epfl.rechor.timetable;

public interface StationAliases extends Indexed {
    /**
     * returns the alternative name of the given index
     *
     * @param id the index of the alternative station
     * @return the alternative station name corresponding to the given index
     * @throws IndexOutOfBoundsException if the index is out of valid range
     *                                   (i.e., less than 0 or greater than
     *                                   or equal to the total number of aliases)
     */
    String alias(int id);

    /**
     * returns the name of the station that corresponds
     * to the alternative name at the given indexed
     *
     * @param id the index of the primary station
     * @return the primary station name corresponding to the given index
     * @throws IndexOutOfBoundsException if id < 0 or id >= size()
     */
    String stationName(int id);
}
