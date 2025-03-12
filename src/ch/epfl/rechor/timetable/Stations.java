package ch.epfl.rechor.timetable;

public interface Stations extends Indexed {

    String name(int id);

    double longitude(int id);

    double latitude(int id);
}
