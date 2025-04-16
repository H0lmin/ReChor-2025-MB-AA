package ch.epfl.rechor.journey;

import ch.epfl.rechor.timetable.CachedTimeTable;
import ch.epfl.rechor.timetable.Stations;
import ch.epfl.rechor.timetable.TimeTable;
import ch.epfl.rechor.timetable.mapped.FileTimeTable;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Test class for the Router implementation.
 * This class tests the computation of optimal journeys and validates that
 * the implementation correctly handles cases where no transfer exists between stations.
 */
public final class MyCachedTTest {

    /**
     * Helper method to find a station ID by name.
     *
     * @param stations the timetable containing station information
     * @param stationName the name of the station to find
     * @return the ID of the station with the given name
     * @throws NoSuchElementException if no station with the given name is found
     */
    private static int stationId(Stations stations, String stationName) {
        for (int i = 0; i < stations.size(); i++) {
            if (stations.name(i).equals(stationName)) {
                return i;
            }
        }
        throw new NoSuchElementException("Station not found: " + stationName);
    }

    public static void main(String[] args) throws IOException {
        long tStart = System.nanoTime();

        TimeTable rawTimeTable = FileTimeTable.in(Path.of("timetable"));
        TimeTable timeTable = new CachedTimeTable(rawTimeTable);
        Stations stations = timeTable.stations();
        LocalDate date = LocalDate.of(2025, Month.APRIL, 1);
        int depStationId = stationId(stations, "Ecublens VD, EPFL");
        int arrStationId = stationId(stations, "Gruyères");
        Router router = new Router(timeTable);
        try{
            for (int i = arrStationId; i < arrStationId + 7 ; i++) {
                Profile profile = router.profile(date, i);
                List<Journey> js = JourneyExtractor.journeys(profile, depStationId);
                for (int l = 0; l < js.toArray().length; l++) {
                    String j = JourneyIcalConverter.toIcalendar(js.get(l));
                    System.out.println(j);
                }
            }
        } catch (IndexOutOfBoundsException e){
            System.out.println("Tfou ma khedamch");
        }

        double elapsed = (System.nanoTime() - tStart) * 1e-9;
        System.out.printf("Temps écoulé : %.3f s%n", elapsed);
    }
}
