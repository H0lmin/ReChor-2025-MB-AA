package ch.epfl.rechor.timetable;

import java.time.LocalDate;

public class MockTimeTable implements TimeTable {
    private final Stations stations;
    private final Platforms platforms;

    public MockTimeTable() {
        // Create 2 stations named "Station0" and "Station1"
        this.stations = new MockStations("Station0", "Station1");

        // Create 2 platforms.
        // In our stopId space, stations occupy IDs 0 and 1.
        // Platforms will then have platform indices 0 and 1 but appear at stopIds 2 and 3.
        // Here, platform at index 0 belongs to station 0 and at index 1 to station 1.
        this.platforms = new MockPlatforms(
                new String[]{"PlatformA", "PlatformB"},
                new int[]{0, 1});
    }

    @Override
    public Stations stations() {
        return stations;
    }

    @Override
    public StationAliases stationAliases() {
        throw new UnsupportedOperationException("Not needed for this test");
    }

    @Override
    public Platforms platforms() {
        return platforms;
    }

    @Override
    public Routes routes() {
        throw new UnsupportedOperationException("Not needed for this test");
    }

    @Override
    public Transfers transfers() {
        throw new UnsupportedOperationException("Not needed for this test");
    }

    @Override
    public Trips tripsFor(LocalDate date) {
        throw new UnsupportedOperationException("Not needed for this test");
    }

    @Override
    public Connections connectionsFor(LocalDate date) {
        throw new UnsupportedOperationException("Not needed for this test");
    }
}
