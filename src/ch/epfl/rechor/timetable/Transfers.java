package ch.epfl.rechor.timetable;

public interface Transfers extends Indexed {

    int depStationId(int id);

    int minutes(int id);

    int arrivingAt(int stationId);

    int minutesBetween(int depStationId, int arrStationId);

}
