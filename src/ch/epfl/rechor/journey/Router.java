package ch.epfl.rechor.journey;

import ch.epfl.rechor.timetable.TimeTable;

import java.time.LocalDate;

public record Router(TimeTable timeTable) {

    public Profile profile(LocalDate date, int destStation) {
    }

}