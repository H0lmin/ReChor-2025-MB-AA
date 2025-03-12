package ch.epfl.rechor.timetable;

public interface Platforms extends Indexed {

    String name(int id);

    int stationId(int id);
}
