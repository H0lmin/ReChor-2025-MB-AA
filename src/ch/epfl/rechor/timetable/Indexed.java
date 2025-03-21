package ch.epfl.rechor.timetable;

/**
 * Interface representing indexed data, conceptually stored in an array
 * Elements are identified by an index ranging from 0 included to the size of the array excluded
 */
public interface Indexed {
    /**
     * Returns the size of the data, the number of elements
     */
    int size();
}
