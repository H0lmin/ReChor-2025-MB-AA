package ch.epfl.rechor.timetable;

public interface Connections extends Indexed {

    int depStopId(int id);

    int depMins(int id);

    int arrStopId(int id);

    int arrMins(int id);

    int tripId(int id);

    int tripPos(int id);

    int nextConnectionId(int id);
}
