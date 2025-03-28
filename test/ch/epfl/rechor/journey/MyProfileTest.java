package ch.epfl.rechor.journey;

import ch.epfl.rechor.timetable.Connections;
import ch.epfl.rechor.timetable.TimeTable;
import ch.epfl.rechor.timetable.Trips;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the Profile record, covering normal and edge cases.
 */
class MyProfileTest {

    @Test
    void constructorWorksWithValidArguments() {
        TimeTable tt = new DummyTimeTable();
        LocalDate date = LocalDate.of(2025, 3, 18);
        int arrStationId = 10;

        // Suppose we have 3 stations, each with an empty frontier for simplicity.
        List<ParetoFront> fronts = List.of(ParetoFront.EMPTY, ParetoFront.EMPTY, ParetoFront.EMPTY);
        Profile p = new Profile(tt, date, arrStationId, fronts);

        assertSame(tt, p.timeTable());
        assertEquals(date, p.date());
        assertEquals(arrStationId, p.arrStationId());
        assertEquals(3, p.stationFront().size());
        // The list was copied, so it should be immutable (copyOf).
        assertThrows(UnsupportedOperationException.class, () -> p.stationFront().add(ParetoFront.EMPTY));
    }

    @Test
    void constructorAllowsEmptyStationFront() {
        TimeTable tt = new DummyTimeTable();
        LocalDate date = LocalDate.of(2025, 3, 18);
        Profile p = new Profile(tt, date, 0, List.of());
        assertTrue(p.stationFront().isEmpty());
        // forStation(0) should fail with IndexOutOfBounds.
        assertThrows(IndexOutOfBoundsException.class, () -> p.forStation(0));
    }

    @Test
    void forStationThrowsWithNegativeIndex() {
        TimeTable tt = new DummyTimeTable();
        LocalDate date = LocalDate.of(2025, 3, 18);
        Profile p = new Profile(tt, date, 0, List.of(ParetoFront.EMPTY));
        assertThrows(IndexOutOfBoundsException.class, () -> p.forStation(-1));
    }

    @Test
    void forStationThrowsWithIndexTooLarge() {
        TimeTable tt = new DummyTimeTable();
        LocalDate date = LocalDate.of(2025, 3, 18);
        // Suppose we have exactly 2 frontiers.
        Profile p = new Profile(tt, date, 1, List.of(ParetoFront.EMPTY, ParetoFront.EMPTY));
        assertThrows(IndexOutOfBoundsException.class, () -> p.forStation(2));
    }

    @Test
    void connectionsAndTripsDelegateToTimeTable() {
        DummyTimeTable tt = new DummyTimeTable();
        LocalDate date = LocalDate.of(2025, 3, 18);
        Profile p = new Profile(tt, date, 0, List.of(ParetoFront.EMPTY));

        Connections c = p.connections();
        assertNotNull(c);
        assertEquals(1, c.size());
        Trips t = p.trips();
        assertNotNull(t);
        assertEquals(1, t.size());
    }

//    @Test
//    void forStationReturnsCorrectFrontier() {
//        TimeTable tt = new DummyTimeTable();
//        LocalDate date = LocalDate.of(2025, 3, 18);
//
//        ParetoFront pf1 = ParetoFront.EMPTY;
//        var pf2 = new ParetoFront.Builder();
//        List<ParetoFront> fronts = List.of(pf1, pf2);
//        Profile p = new Profile(tt, date, 999, fronts);
//
//        assertSame(pf1, p.forStation(0));
//        assertSame(pf2, p.forStation(1));
//    }

    // A simple dummy TimeTable that always returns the same dummy Connections and Trips.
    private static final class DummyTimeTable implements TimeTable {
        @Override
        public ch.epfl.rechor.timetable.Stations stations() {
            return null;
        }

        @Override
        public ch.epfl.rechor.timetable.StationAliases stationAliases() {
            return null;
        }

        @Override
        public ch.epfl.rechor.timetable.Platforms platforms() {
            return null;
        }

        @Override
        public ch.epfl.rechor.timetable.Routes routes() {
            return null;
        }

        @Override
        public ch.epfl.rechor.timetable.Transfers transfers() {
            return null;
        }

        @Override
        public Trips tripsFor(LocalDate date) {
            return new Trips() {
                @Override
                public int routeId(int id) {
                    return 42;
                }

                @Override
                public String destination(int id) {
                    return "DummyDestination";
                }

                @Override
                public int size() {
                    return 1;
                }
            };
        }

        @Override
        public Connections connectionsFor(java.time.LocalDate date) {
            return new Connections() {
                @Override
                public int depStopId(int id) {
                    return 0;
                }

                @Override
                public int depMins(int id) {
                    return 100;
                }

                @Override
                public int arrStopId(int id) {
                    return 1;
                }

                @Override
                public int arrMins(int id) {
                    return 110;
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
                    return -1;
                }

                @Override
                public int size() {
                    return 1;
                }
            };
        }
    }
}


