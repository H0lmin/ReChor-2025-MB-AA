package ch.epfl.rechor.timetable;

import ch.epfl.rechor.journey.Vehicle;

public interface Routes extends Indexed {

    Vehicle vehicle(int id);

    String name(int id);
}
