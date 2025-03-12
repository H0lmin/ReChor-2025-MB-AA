package ch.epfl.rechor.timetable;

public interface StationAliases extends Indexed {

    String alias(int id);

    String stationName(int id);
}
