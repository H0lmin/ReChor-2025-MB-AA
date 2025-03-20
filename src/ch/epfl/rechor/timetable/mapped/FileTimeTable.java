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


public record FileTimeTable(
        Path directory,
        List<String> stringTable,
        Stations stations,
        StationAliases stationAliases,
        Platforms platforms,
        Routes routes,
        Transfers transfers
) implements TimeTable {

    public static TimeTable in(Path directory) throws IOException {
        Path stringsPath = directory.resolve("strings.txt");
        List<String> stringTable = List.copyOf(
                Files.readAllLines(stringsPath, StandardCharsets.ISO_8859_1)
        );

        BufferedStations stations = new BufferedStations(stringTable, mapFile(directory.resolve("stations.bin")));
        BufferedStationAliases stationAliases = new BufferedStationAliases(stringTable,
                mapFile(directory.resolve("station-aliases.bin")));
        BufferedRoutes routes = new BufferedRoutes(stringTable, mapFile(directory.resolve("routes.bin")));
        BufferedPlatforms platforms = new BufferedPlatforms(stringTable,
                mapFile(directory.resolve("platforms.bin")));
        BufferedTransfers transfers = new BufferedTransfers(mapFile(directory.resolve("transfers.bin")));

        return new FileTimeTable(directory, stringTable, stations, stationAliases, platforms, routes, transfers);
    }

    private static ByteBuffer mapFile(Path path) throws IOException {
        try (FileChannel channel = FileChannel.open(path)) {
            return channel.map(FileChannel.MapMode.READ_ONLY, 0, channel.size());
        }
    }

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
