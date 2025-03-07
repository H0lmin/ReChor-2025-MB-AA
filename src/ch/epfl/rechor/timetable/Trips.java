package ch.epfl.rechor.timetable;

public interface Trips extends Indexed {

    /**
     * @throws IndexOutOfBoundsException if {@code id} is out of range
     *                                                (ie. {@code id} < 0 or
     *                                                   {@code id} >= size ()
     */
    int routeId(int id);

    /**
     * @throws IndexOutOfBoundsException if {@code id} is out of range
     *                                                (ie. {@code id} < 0 or
     *                                                   {@code id} >= size ()
     */
    String destination(int id);
}
