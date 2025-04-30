package ch.epfl.rechor.journey;

import ch.epfl.rechor.timetable.Stations;
import ch.epfl.rechor.timetable.TimeTable;
import ch.epfl.rechor.timetable.mapped.FileTimeTable;
import ch.epfl.rechor.timetable.CachedTimeTable;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.StringJoiner;

public final class MyRouterTest {
    private static final Path OUTPUT_FILE = Path.of("ProfileTest1.txt");

    public static void main(String[] args) {
        try {
            TimeTable timeTable = new CachedTimeTable(
                    FileTimeTable.in(Path.of("timetable1")));
            LocalDate date = LocalDate.of(2025, 3, 18);
            Router router = new Router(timeTable);
            Stations stations = timeTable.stations();
            int numStations = stations.size();

            try (BufferedWriter writer = Files.newBufferedWriter(
                    OUTPUT_FILE, StandardCharsets.UTF_8)) {
                for (int destStationId = 0; destStationId < numStations; destStationId++) {
                    Profile profile = router.profile(date, destStationId);
                    writeProfile(writer, profile, numStations);
                }
            }
        } catch (IOException e) {
            System.err.println("Error writing profiles: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void writeProfile(BufferedWriter writer,
                                     Profile profile,
                                     int stationCount) throws IOException {
        for (int s = 0; s < stationCount; s++) {
            ParetoFront front = profile.forStation(s);
            StringJoiner joiner = new StringJoiner(",");
            front.forEach(tuple -> joiner.add(Long.toHexString(tuple)));
            writer.write(joiner.toString());
            writer.newLine();
        }
    }
}
