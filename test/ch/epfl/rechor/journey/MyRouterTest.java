package ch.epfl.rechor.journey;

import ch.epfl.rechor.timetable.Stations;
import ch.epfl.rechor.timetable.TimeTable;
import ch.epfl.rechor.timetable.mapped.FileTimeTable;
import ch.epfl.rechor.timetable.CachedTimeTable;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.stream.IntStream;

public final class MyRouterTest {

    public static void main(String[] args) throws IOException {
        TimeTable rawTimeTable = FileTimeTable.in(Path.of("timetable1"));

        TimeTable timeTable = new CachedTimeTable(rawTimeTable);

        LocalDate date = LocalDate.of(2025, 3, 18);

        Router router = new Router(timeTable);

        Stations stations = timeTable.stations();
        int numStations = stations.size();

        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(System.out))) {
            IntStream.range(0, numStations).parallel().forEach(destStationId -> {
                Profile profile = router.profile(date, destStationId);
                StringBuilder sb = new StringBuilder();

                for (int s = 0; s < numStations; s++) {
                    StringBuilder line = new StringBuilder();
                    ParetoFront front = profile.forStation(s);
                    front.forEach(tuple -> {
                        if (!line.isEmpty()) {
                            line.append(",");
                        }
                        line.append(Long.toHexString(tuple));
                    });
                    sb.append(line).append("\n");
                }

                // Write the built string to System.out in a thread-safe way.
                synchronized(writer) {
                    try {
                        writer.write(sb.toString());
                        writer.flush();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    }
}
