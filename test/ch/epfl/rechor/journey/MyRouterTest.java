package ch.epfl.rechor.journey;

import ch.epfl.rechor.timetable.Stations;
import ch.epfl.rechor.timetable.TimeTable;
import ch.epfl.rechor.timetable.mapped.FileTimeTable;
import ch.epfl.rechor.timetable.CachedTimeTable;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;

public final class MyRouterTest {

    public static void main(String[] args) throws IOException {
        // Load the timetable from disk and wrap it in CachedTimeTable to take advantage of caching.
        TimeTable rawTimeTable = FileTimeTable.in(Path.of("timetable1"));
        TimeTable timeTable = new CachedTimeTable(rawTimeTable);

        // Set the travel date.
        LocalDate date = LocalDate.of(2025, 3, 18);
        // Destination: Gruyères (destStationId is 11486)
        int destStationId = 11486;

        // Create the Router and compute the Profile for the given date and destination.
        Router router = new Router(timeTable);
        Profile profile = router.profile(date, destStationId);

        // Use the provided station ID for Ecublens VD.
        Stations stations = timeTable.stations();

        int ecublensStationId = 7872;
//        for (int i = 0; i < stations.size(); i++) {
//            if (stations.name(i).contains("Ecublens VD, EPFL")) {
//                ecublensStationId = i;
//                break;
//            }
//        }

        // Print only the Pareto frontier for Ecublens VD.
        for (int i = 0; i < stations.size(); i++) {
            System.out.println("Pareto frontier for station "
                    + stations.name(ecublensStationId)
                    + " (ID " + 7872 + ") for destination (ID " + destStationId + "):");
            ParetoFront front = profile.forStation(ecublensStationId);

            front.forEach(tuple -> {
                int arrMins = PackedCriteria.arrMins(tuple);
                int changes = PackedCriteria.changes(tuple);
                int payload = PackedCriteria.payload(tuple);
                String depInfo = PackedCriteria.hasDepMins(tuple)
                        ?
                        PackedCriteria.depMins(tuple) / 60 + "h" + PackedCriteria.depMins(tuple) % 60
                        : "n/a";
//            if (PackedCriteria.depMins(tuple) > 950 && PackedCriteria.depMins(tuple) < 1080){
                System.out.println("  (dep: " + depInfo
                        + ", arr: " + arrMins
                        + ", changes: " + changes
                        + ", payload: " + payload
                        + ")  [hex: " + Long.toHexString(tuple) + "]");
            });
            System.out.println();
        }
    }

    public static void printProfile (Profile profile) {
        Stations stations = profile.timeTable().stations();
        for (int i = 0; i < stations.size(); i++) {
            String stationName = stations.name(i);
            ParetoFront front = profile.forStation(i);
            System.out.println("Station " + stationName + " (ID " + i + ") has " + front.size() + " tuple(s):");
            front.forEach(tuple -> {
                int arrMins = PackedCriteria.arrMins(tuple);
                int changes = PackedCriteria.changes(tuple);
                int payload = PackedCriteria.payload(tuple);
                String depInfo = PackedCriteria.hasDepMins(tuple)
                        ?
                        PackedCriteria.depMins(tuple) / 60 + "h" + PackedCriteria.depMins(tuple) % 60
                        : "n/a";
                System.out.println("  (dep: " + depInfo
                        + ", arr: " + arrMins / 60 + "h" + arrMins % 60
                        + ", changes: " + changes);
//                        + ", payload: " + payload + ")");
            });
            System.out.println();
        }
    }
}
