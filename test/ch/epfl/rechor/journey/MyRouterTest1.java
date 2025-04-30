package ch.epfl.rechor.journey;

import java.io.IOError;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import java.util.NoSuchElementException;

import ch.epfl.rechor.PackedRange;
import ch.epfl.rechor.timetable.Transfers;
import org.junit.jupiter.api.Test;

import ch.epfl.rechor.timetable.CachedTimeTable;
import ch.epfl.rechor.timetable.Stations;
import ch.epfl.rechor.timetable.TimeTable;
import ch.epfl.rechor.timetable.mapped.FileTimeTable;

public class MyRouterTest1 {
    static int stationId(Stations stations, String stationName) {

        for(int i = 0; i < stations.size(); i++){

            if(stations.name(i).equals(stationName)) {
                return i;
            }
        }

        return -1;
    }

    @Test
    void testProfile() throws IOException{
        long tStart = System.nanoTime();

        TimeTable timeTable =
                new CachedTimeTable(FileTimeTable.in(Path.of("timetable12")));
        //new CachedTimeTable(FileTimeTable.in(Path.of("ReCHor/timetable")));

        Stations stations = timeTable.stations();
        LocalDate date = LocalDate.of(2025, Month.MARCH, 18);
        int depStationId = stationId(stations, "Ecublens VD, EPFL");
        int arrStationId = stationId(stations, "Gruyères");
        Router router = new Router(timeTable);
        Profile profile = router.profile(date, arrStationId);

        List<Journey> journeys = JourneyExtractor.journeys(profile, depStationId);

        Journey j = journeys.get(32);

        System.out.println(JourneyIcalConverter.toIcalendar(j));

        double elapsed = (System.nanoTime() - tStart) * 1e-9;
        System.out.printf("Temps écoulé : %.3f s%n", elapsed);

    }
}

    /**
package ch.epfl.rechor.journey;

import java.io.IOError;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import java.util.NoSuchElementException;

import ch.epfl.rechor.PackedRange;
import ch.epfl.rechor.timetable.Transfers;
import org.junit.jupiter.api.Test;

import ch.epfl.rechor.timetable.CachedTimeTable;
import ch.epfl.rechor.timetable.Stations;
import ch.epfl.rechor.timetable.TimeTable;
import ch.epfl.rechor.timetable.mapped.FileTimeTable;


    public class MyRouterTest1 {
        static int stationId(Stations stations, String stationName) {

            for(int i = 0; i < stations.size(); i++){

                if(stations.name(i).equals(stationName)) {
                    return i;
                }
            }

            return -1;
        }

        @Test
        void testProfile() throws IOException{
            long tStart = System.nanoTime();

            TimeTable timeTable =
                    new CachedTimeTable(FileTimeTable.in(Path.of("timetable1")));
            //new CachedTimeTable(FileTimeTable.in(Path.of("ReCHor/timetable")));

            Stations stations = timeTable.stations();
            LocalDate date = LocalDate.of(2025, Month.MARCH, 18);
            int depStationId = stationId(stations, "Ecublens VD, EPFL");
            assert depStationId != -1 : "depStationId not found!";
            int arrStationId = stationId(stations, "Gruyères");
            Router router = new Router(timeTable);
            Profile profile = router.profile(date, arrStationId);

            System.out.println("depStationId = " + depStationId);
            System.out.println("arrStationId = " + arrStationId);

            List<Journey> journeys = JourneyExtractor.journeys(profile, depStationId);
            System.out.println("Nombre de voyages trouvés = " + journeys.size());
            for (Journey journey : journeys) {
                System.out.println(JourneyIcalConverter.toIcalendar(journey));
            }

            System.out.println("Nombre de voyages trouvés : " + journeys.size());
            if (journeys.size() > 32) {
                Journey j = journeys.get(32);
                System.out.println(JourneyIcalConverter.toIcalendar(j));
            } else {
                System.out.println("Moins de 33 voyages trouvés !");
            }

            System.out.println("Quelques gares avec voyages optimaux disponibles :");

            int count = 0;
            for (int stationId = 0; stationId < stations.size(); stationId++) {
                try {
                    List<Journey> journeysFromStation = JourneyExtractor.journeys(profile, stationId);
                    if (!journeysFromStation.isEmpty()) {
                        System.out.println("- Station ID " + stationId + " : " + journeysFromStation.size() + " voyages trouvés");
                        count++;
                        if (count >= 20) break; // Limite à 20 stations pour éviter d'inonder la console
                    }
                } catch (NoSuchElementException e) {
                    // Ignore cette station, pas de transfert piéton possible
                }
            }

            double elapsed = (System.nanoTime() - tStart) * 1e-9;
            System.out.printf("Temps écoulé : %.3f s%n", elapsed);

        }

        @Test
        void listStationsAccessibleFromEcublens() throws IOException {
            TimeTable timeTable =
                    new CachedTimeTable(FileTimeTable.in(Path.of("timetable1")));

            Stations stations = timeTable.stations();
            Transfers transfers = timeTable.transfers();

            int ecublensId = stationId(stations, "Ecublens VD, EPFL");
            assert ecublensId != -1 : "Ecublens VD, EPFL not found!";

            System.out.println("Stations accessibles à pied depuis Ecublens VD, EPFL :");

            int packedRange = transfers.arrivingAt(ecublensId);
            int from = PackedRange.startInclusive(packedRange);
            int to = PackedRange.endExclusive(packedRange);

            for (int i = from; i < to; i++) {
                int fromStationId = transfers.depStationId(i);
                int duration = transfers.minutes(i);

                String stationName = stations.name(fromStationId);
                System.out.printf("- %s (durée marche : %d minutes)%n", stationName, duration);
            }
        }

    }
    */