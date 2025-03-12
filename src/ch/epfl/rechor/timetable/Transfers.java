package ch.epfl.rechor.timetable;

import java.util.NoSuchElementException;

public interface Transfers extends Indexed {

    int depStationId(int id);

    int minutes(int id);

    int arrivingAt(int stationId);

    int minutesBetween(int depStationId, int arrStationId);

}
