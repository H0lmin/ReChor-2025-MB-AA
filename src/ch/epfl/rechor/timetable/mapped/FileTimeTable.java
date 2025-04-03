package ch.epfl.rechor.timetable.mapped;

import ch.epfl.rechor.timetable.*;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

/**
 * The {@code FileTimeTable} record represents a timetable backed by a file system directory.
 * <p>
 * It implements the {@link TimeTable} interface by reading various binary and text files from a
 * given directory. The directory is expected to contain files for stations, station aliases,
 * platforms, routes, and transfers, as well as subdirectories for date-specific trips and
 * connections.
 * </p>
 *
 * @author Amine AMIRA (393410)
 * @author Malak Berrada (379791)
 */
public record FileTimeTable(
        Path directory,
        List<String> stringTable,
        Stations stations,
        StationAliases stationAliases,
        Platforms platforms,
        Routes routes,
        Transfers transfers
) implements TimeTable {

    /**
     * Creates a {@link TimeTable} instance from the files in the specified directory.
     *
     * @param directory the base directory containing the timetable files.
     * @return a {@link TimeTable} instance built from the specified directory.
     * @throws IOException if an I/O error occurs while reading the required files.
     */
    public static TimeTable in(Path directory) throws IOException {
        Path stringsPath = directory.resolve("strings.txt");
        List<String> stringTable = List.copyOf(
                Files.readAllLines(stringsPath, StandardCharsets.ISO_8859_1)
        );

        BufferedStations stations = new BufferedStations(stringTable, mapFile(directory.resolve(
                "stations.bin")));
        BufferedStationAliases stationAliases = new BufferedStationAliases(stringTable,
                mapFile(directory.resolve("station-aliases.bin")));
        BufferedRoutes routes = new BufferedRoutes(stringTable, mapFile(directory
                .resolve("routes" + ".bin")));
        BufferedPlatforms platforms = new BufferedPlatforms(stringTable,
                mapFile(directory.resolve("platforms.bin")));
        BufferedTransfers transfers = new BufferedTransfers(mapFile(directory
                .resolve("transfers" + ".bin")));

        return new FileTimeTable(directory, stringTable, stations, stationAliases, platforms,
                routes, transfers);
    }

    /**
     * Maps the specified file into memory as a read-only {@link ByteBuffer}.
     *
     * @param path the path to the file to be mapped.
     * @return a read-only {@link ByteBuffer} representing the file contents.
     * @throws IOException if an I/O error occurs while opening or mapping the file.
     */
    private static ByteBuffer mapFile(Path path) throws IOException {
        try (FileChannel channel = FileChannel.open(path)) {
            return channel.map(FileChannel.MapMode.READ_ONLY, 0, channel.size());
        }
    }

    /**
     * Retrieves the trips scheduled for the specified date.
     *
     * @param date the {@link LocalDate} for which trips should be retrieved.
     * @return a {@link Trips} instance containing the trips for the given date.
     * @throws UncheckedIOException if an I/O error occurs while reading the trips file.
     */
    @Override
    public Trips tripsFor(LocalDate date) {
        Path dayFolder = directory.resolve(date.toString());
        Path tripsPath = dayFolder.resolve("trips.bin");
        try {
            ByteBuffer tripsBuffer = mapFile(tripsPath);
            return new BufferedTrips(stringTable, tripsBuffer);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Retrieves the connections scheduled for the specified date.
     *
     * @param date the {@link LocalDate} for which connections should be retrieved.
     * @return a {@link Connections} instance containing the connections for the given date.
     * @throws UncheckedIOException if an I/O error occurs while reading the connections files.
     */
    @Override
    public Connections connectionsFor(LocalDate date) {
        Path dayFolder = directory.resolve(date.toString());
        Path connectionsPath = dayFolder.resolve("connections.bin");
        Path succPath = dayFolder.resolve("connections-succ.bin");
        try {
            ByteBuffer connectionsBuffer = mapFile(connectionsPath);
            ByteBuffer succBuffer = mapFile(succPath);
            return new BufferedConnections(connectionsBuffer, succBuffer);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
