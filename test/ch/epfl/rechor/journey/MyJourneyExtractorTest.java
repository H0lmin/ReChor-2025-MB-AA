package ch.epfl.rechor.journey;

import ch.epfl.rechor.timetable.TimeTable;
import ch.epfl.rechor.timetable.mapped.FileTimeTable;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.Month;
import java.util.List;

class MyJourneyExtractorTest {

    public static void main(String[] args) {
        try {
            TimeTable t = FileTimeTable.in(Path.of("timetable"));
            LocalDate date = LocalDate.of(2025, Month.MARCH, 18);
            Profile p = readProfile(t, date, 11486);
            List<Journey> js = JourneyExtractor.journeys(p, 7872);
            String j = JourneyIcalConverter.toIcalendar(js.get(32));
            System.out.println(j);
            List<Journey> js1 = JourneyExtractor.journeys(p, 7800);
            String j1 = JourneyIcalConverter.toIcalendar(js1.get(32));
            System.out.println(j1);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static Profile readProfile(TimeTable timeTable,
                        LocalDate date,
                        int arrStationId) throws IOException {
        Path path =
                Path.of("profile_" + date + "_" + arrStationId + ".txt");
        try (BufferedReader r = Files.newBufferedReader(path)) {
            Profile.Builder profileB =
                    new Profile.Builder(timeTable, date, arrStationId);
            int stationId = -1;
            String line;
            while ((line = r.readLine()) != null) {
                stationId += 1;
                if (line.isEmpty()) continue;
                ParetoFront.Builder frontB = new ParetoFront.Builder();
                for (String t : line.split(","))
                    frontB.add(Long.parseLong(t, 16));
                profileB.setForStation(stationId, frontB);
            }
            return profileB.build();
        }
    }
}
