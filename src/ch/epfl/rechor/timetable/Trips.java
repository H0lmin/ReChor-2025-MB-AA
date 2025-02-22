package ch.epfl.rechor.timetable;

public interface Trips extends Indexed {

    int routeId(int id);

    String destination(int id);
}
