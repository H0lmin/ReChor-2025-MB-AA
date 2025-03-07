package ch.epfl.rechor.timetable;

import ch.epfl.rechor.journey.Vehicle;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class TimeTableTest {
    @Test
    void timeTableIsStationIdWorks() {
        var s = new FakeStations(10);
        var p = new FakePlatforms(5);
        var tt = new FakeTimeTable(p, s);
        for (int i = 0; i < s.size(); i += 1) assertTrue(tt.isStationId(i));
        for (int i = 0; i < p.size(); i += 1) assertFalse(tt.isStationId(i + s.size()));
    }

    @Test
    void timeTableIsPlatformIdWorks() {
        var s = new FakeStations(10);
        var p = new FakePlatforms(5);
        var tt = new FakeTimeTable(p, s);
        for (int i = 0; i < s.size(); i += 1) assertFalse(tt.isPlatformId(i));
        for (int i = 0; i < p.size(); i += 1) assertTrue(tt.isPlatformId(i + s.size()));
    }

    @Test
    void timeTableStationIdWorks() {
        var s = new FakeStations(10);
        var p = new FakePlatforms(5);
        var tt = new FakeTimeTable(p, s);
        for (int i = 0; i < s.size(); i += 1) assertEquals(i, tt.stationId(i));
        for (int i = 0; i < p.size(); i += 1) assertEquals(i, tt.stationId(i + s.size()));
    }

    @Test
    void timeTablePlatformNameWorks() {
        var s = new FakeStations(10);
        var p = new FakePlatforms(5);
        var tt = new FakeTimeTable(p, s);
        for (int i = 0; i < s.size(); i += 1)
            assertNull(tt.platformName(i));
        for (int i = 0; i < p.size(); i += 1)
            assertEquals(FakePlatforms.fakePlatformName(i), tt.platformName(i + s.size()));
    }

    private record FakeTimeTable(Platforms platforms, Stations stations) implements TimeTable {

        @Override
            public StationAliases stationAliases() {
                return new StationAliases() {
                    @Override
                    public String alias(int id) {
                        return "";
                    }

                    @Override
                    public String stationName(int id) {
                        return "";
                    }

                    @Override
                    public int size() {
                        return 0;
                    }
                };
            }

            @Override
            public Transfers transfers() {
                return new Transfers() {
                    @Override
                    public int depStationId(int id) {
                        return 0;
                    }

                    @Override
                    public int minutes(int id) {
                        return 0;
                    }

                    @Override
                    public int arrivingAt(int stationId) {
                        return 0;
                    }

                    @Override
                    public int minutesBetween(int depStationId, int arrStationId) {
                        return 0;
                    }

                    @Override
                    public int size() {
                        return 0;
                    }
                };
            }

            @Override
            public Routes routes() {
                return new Routes() {
                    @Override
                    public Vehicle vehicle(int id) {
                        return null;
                    }

                    @Override
                    public String name(int id) {
                        return "";
                    }

                    @Override
                    public int size() {
                        return 0;
                    }
                };
            }

            @Override
            public Connections connectionsFor(LocalDate date) {
                return new Connections() {
                    @Override
                    public int depStopId(int id) {
                        return 0;
                    }

                    @Override
                    public int depMins(int id) {
                        return 0;
                    }

                    @Override
                    public int arrStopId(int id) {
                        return 0;
                    }

                    @Override
                    public int arrMins(int id) {
                        return 0;
                    }

                    @Override
                    public int tripId(int id) {
                        return 0;
                    }

                    @Override
                    public int tripPos(int id) {
                        return 0;
                    }

                    @Override
                    public int nextConnectionId(int id) {
                        return 0;
                    }

                    @Override
                    public int size() {
                        return 0;
                    }
                };
            }

            @Override
            public Trips tripsFor(LocalDate date) {
                return new Trips() {
                    @Override
                    public int routeId(int id) {
                        return 0;
                    }

                    @Override
                    public String destination(int id) {
                        return "";
                    }

                    @Override
                    public int size() {
                        return 0;
                    }
                };
            }
        }

    private record FakeStations(int size) implements Stations {

        @Override
            public String name(int id) {
                return "";
            }

            @Override
            public double longitude(int id) {
                return 0;
            }

            @Override
            public double latitude(int id) {
                return 0;
            }
        }

    private record FakePlatforms(int size) implements Platforms {
            public static String fakePlatformName(int id) {
                return String.valueOf((char) ('A' + id));
            }

        @Override
            public String name(int id) {
                return fakePlatformName(id);
            }

            @Override
            public int stationId(int id) {
                return id;
            }
        }
}