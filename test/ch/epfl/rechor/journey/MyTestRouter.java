package ch.epfl.rechor.journey;

import ch.epfl.rechor.timetable.CachedTimeTable;
import ch.epfl.rechor.timetable.Stations;
import ch.epfl.rechor.timetable.TimeTable;
import ch.epfl.rechor.timetable.mapped.FileTimeTable;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Test class for the Router implementation.
 * This class tests the computation of optimal journeys and validates that
 * the implementation correctly handles cases where no transfer exists between stations.
 */
public final class MyTestRouter {

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

            TimeTable timeTable = FileTimeTable.in(Path.of("timetable"));
            Stations stations = timeTable.stations();
            LocalDate date = LocalDate.of(2025, Month.APRIL, 1);
            int depStationId = stationId(stations, "Ecublens VD, EPFL");
            int arrStationId = stationId(stations, "Gruyères");
            Router router = new Router(timeTable);
            Profile profile = router.profile(date, arrStationId);
            Journey journey = JourneyExtractor
                    .journeys(profile, depStationId)
                    .get(32);
            System.out.println(JourneyIcalConverter.toIcalendar(journey));

            double elapsed = (System.nanoTime() - tStart) * 1e-9;
            System.out.printf("Temps écoulé : %.3f s%n", elapsed);
    }

    /**
     * Tests journey extraction from a specific departure station.
     *
     * @param profile the computed profile
     * @param departureStationName the name of the departure station
     */
    private static void testJourneysFromStation(Profile profile, String departureStationName) {
        try {
            TimeTable fileTimeTable = profile.timeTable();
            TimeTable timeTable = new CachedTimeTable(fileTimeTable);

            int departureStationId = stationId(timeTable.stations(), departureStationName);

            System.out.println("\nTesting journeys from " + departureStationName + " (ID: " + departureStationId + ")");

            List<Journey> journeys = JourneyExtractor.journeys(profile, departureStationId);
            System.out.println("Number of journeys from " + departureStationName + ": " + journeys.size());

            if (!journeys.isEmpty()) {
                // Display details of the first journey
                Journey firstJourney = journeys.get(0);
                System.out.println("First journey details:");
                System.out.println("- Number of legs: " + firstJourney.legs().size());
                System.out.println("- Journey: " + firstJourney);

                // Check for any foot legs and display their walking duration (should be -1 when no transfer exists)
                firstJourney.legs().forEach(leg -> {
                    if (leg instanceof Journey.Leg.Foot footLeg) {
                        long walkingMinutes = Duration.between(footLeg.depTime(), footLeg.arrTime()).toMinutes();
                        System.out.println("Foot leg walking minutes: " + walkingMinutes);
                    }
                });

                // If there are enough journeys, display the 32nd one (as in the example)
                if (journeys.size() > 32) {
                    Journey journey32 = journeys.get(32);
                    System.out.println("\nJourney[32] details:");
                    System.out.println("- Number of legs: " + journey32.legs().size());
                    System.out.println("- Journey: " + journey32);

                    // Convert to iCalendar format
                    String icalJourney = JourneyIcalConverter.toIcalendar(journey32);
                    System.out.println("\niCalendar representation:");
                    System.out.println(icalJourney);
                }
            } else {
                System.out.println("No journeys found from " + departureStationName);
            }

        } catch (Exception e) {
            System.err.println("Error testing journeys from " + departureStationName + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
}
