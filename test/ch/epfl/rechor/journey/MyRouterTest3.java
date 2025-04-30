package ch.epfl.rechor.journey;

import ch.epfl.rechor.timetable.TimeTable;
import ch.epfl.rechor.timetable.Stations;
import ch.epfl.rechor.timetable.mapped.FileTimeTable;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;

class MyRouterTest3 {
    private static Path testPath(String path) {
        var maybeTestDataDir = System.getenv("RECHOR_TEST_DATA_DIR");
        return maybeTestDataDir != null
                ? Path.of(maybeTestDataDir).resolve(path)
                : Path.of(path);
    }

    private static int stationId(TimeTable timeTable, String name) {
        var stations = timeTable.stations();
        for (var i = 0; i < stations.size(); i++) {
            if (stations.name(i).equals(name))
                return i;
        }
        throw new NoSuchElementException("Nom de station inconnu : " + name);
    }

    @Test
    void routerProfileBuildsValidParetoTuples() throws IOException {
        var timeTable = FileTimeTable.in(testPath("timetable12"));
        var router = new Router(timeTable);

        var date = LocalDate.of(2025, Month.MARCH, 18);
        int depStationId = stationId(timeTable, "Ecublens VD, EPFL");
        int arrStationId = stationId(timeTable, "Gruyères");

        var profile = router.profile(date, arrStationId);
        var pareto = profile.forStation(depStationId);

        assertNotNull(pareto);

        // Vérifie que les tuples ont des valeurs cohérentes
        pareto.forEach(tuple -> {
            int arr = PackedCriteria.arrMins(tuple);
            int ch = PackedCriteria.changes(tuple);

            assertTrue(arr >= 0 && arr <= 24 * 60, "Heure d'arrivée doit être entre 0 et 1440 min");
            assertTrue(ch >= 0, "Nombre de changements doit être positif");
        });

        // Affiche proprement les tuples
        System.out.println("Tuples de Pareto pour EPFL -> Gruyères le 18 mars 2025 :");
        pareto.forEach(tuple -> {
            int dep = PackedCriteria.hasDepMins(tuple) ? PackedCriteria.depMins(tuple) : -1;
            int arr = PackedCriteria.arrMins(tuple);
            int ch = PackedCriteria.changes(tuple);
            System.out.printf("Départ: %s | Arrivée: %02dh%02d | Changements: %d%n",
                    (dep == -1 ? "N/A" : String.format("%02dh%02d", dep / 60, dep % 60)),
                    arr / 60, arr % 60,
                    ch);
        });
    }
}
