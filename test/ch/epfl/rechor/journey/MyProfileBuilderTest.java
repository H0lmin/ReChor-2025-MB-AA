package ch.epfl.rechor.journey;

import ch.epfl.rechor.timetable.Stations;
import ch.epfl.rechor.timetable.Trips;
import ch.epfl.rechor.timetable.TimeTable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MyProfileBuilderTest {

    // --- Dummy implementations for testing ---
    private static class DummyStations implements Stations {
        @Override
        public String name(int id) {
            return "Station" + id;
        }
        @Override
        public double longitude(int id) {
            return 6.0 + id;
        }
        @Override
        public double latitude(int id) {
            return 46.0 + id;
        }
        @Override
        public int size() {
            return 3; // exactly 3 stations
        }
    }

    private static class DummyTrips implements Trips {
        @Override
        public int routeId(int id) {
            return 10 + id;
        }
        @Override
        public String destination(int id) {
            return "Destination" + id;
        }
        @Override
        public int size() {
            return 2; // exactly 2 trips
        }
    }

    private static class DummyTimeTable implements TimeTable {
        private final DummyStations stations = new DummyStations();
        private final DummyTrips trips = new DummyTrips();
        @Override public Stations stations() { return stations; }
        @Override public Trips tripsFor(LocalDate date) { return trips; }
        // Other methods not needed for Profile.Builder testing.
        @Override public ch.epfl.rechor.timetable.StationAliases stationAliases() { return null; }
        @Override public ch.epfl.rechor.timetable.Platforms platforms() { return null; }
        @Override public ch.epfl.rechor.timetable.Routes routes() { return null; }
        @Override public ch.epfl.rechor.timetable.Transfers transfers() { return null; }
        @Override public ch.epfl.rechor.timetable.Connections connectionsFor(LocalDate date) { return null; }
    }

    // A simple dummy ParetoFront.Builder for testing.
    // We add one tuple and then build.
    private static ParetoFront.Builder createDummyPfBuilder(long tuple) {
        ParetoFront.Builder builder = new ParetoFront.Builder();
        builder.add(tuple);
        return builder;
    }

    private DummyTimeTable dummyTT;
    private LocalDate testDate;
    private int arrivalStationId; // for Profile

    @BeforeEach
    void setUp() {
        dummyTT = new DummyTimeTable();
        testDate = LocalDate.of(2025, 3, 18);
        arrivalStationId = 2; // valid because dummy stations size == 3.
    }

    // --- Tests for the Profile.Builder constructor ---

    @Test
    void builderInitializesArraysWithCorrectLength() {
        Profile.Builder pb = new Profile.Builder(dummyTT, testDate, arrivalStationId);
        // The stationBuilders array length should equal dummyTT.stations().size() == 3.
        // And tripBuilders length equals dummyTT.tripsFor(testDate).size() == 2.
        // We test this indirectly by calling forStation for valid indices (expect null)
        for (int i = 0; i < 3; i++) {
            assertNull(pb.forStation(i), "For station index " + i + " builder should be null initially");
        }
        for (int i = 0; i < 2; i++) {
            assertNull(pb.forTrip(i), "For trip index " + i + " builder should be null initially");
        }
    }

    // --- Tests for forStation and setForStation ---

    @Test
    void forStationThrowsForNegativeIndex() {
        Profile.Builder pb = new Profile.Builder(dummyTT, testDate, arrivalStationId);
        assertThrows(IndexOutOfBoundsException.class, () -> pb.forStation(-1));
    }

    @Test
    void forStationThrowsForIndexTooLarge() {
        Profile.Builder pb = new Profile.Builder(dummyTT, testDate, arrivalStationId);
        assertThrows(IndexOutOfBoundsException.class, () -> pb.forStation(3));
    }

    @Test
    void setForStationThrowsForInvalidIndex() {
        Profile.Builder pb = new Profile.Builder(dummyTT, testDate, arrivalStationId);
        ParetoFront.Builder dummyPfB = createDummyPfBuilder(0xABCDEF01L);
        assertThrows(IndexOutOfBoundsException.class, () -> pb.setForStation(-1, dummyPfB));
        assertThrows(IndexOutOfBoundsException.class, () -> pb.setForStation(3, dummyPfB));
    }

    @Test
    void setForStationStoresValueCorrectly() {
        Profile.Builder pb = new Profile.Builder(dummyTT, testDate, arrivalStationId);
        ParetoFront.Builder dummyPfB = createDummyPfBuilder(0x11111111L);
        pb.setForStation(1, dummyPfB);
        assertSame(dummyPfB, pb.forStation(1), "forStation(1) should return the builder that was set");
    }

    // --- Tests for forTrip and setForTrip ---

    @Test
    void forTripThrowsForNegativeIndex() {
        Profile.Builder pb = new Profile.Builder(dummyTT, testDate, arrivalStationId);
        assertThrows(IndexOutOfBoundsException.class, () -> pb.forTrip(-1));
    }

    @Test
    void forTripThrowsForIndexTooLarge() {
        Profile.Builder pb = new Profile.Builder(dummyTT, testDate, arrivalStationId);
        assertThrows(IndexOutOfBoundsException.class, () -> pb.forTrip(2));
    }

    @Test
    void setForTripThrowsForInvalidIndex() {
        Profile.Builder pb = new Profile.Builder(dummyTT, testDate, arrivalStationId);
        ParetoFront.Builder dummyPfB = createDummyPfBuilder(0x22222222L);
        assertThrows(IndexOutOfBoundsException.class, () -> pb.setForTrip(-1, dummyPfB));
        assertThrows(IndexOutOfBoundsException.class, () -> pb.setForTrip(2, dummyPfB));
    }

    @Test
    void setForTripStoresValueCorrectly() {
        Profile.Builder pb = new Profile.Builder(dummyTT, testDate, arrivalStationId);
        ParetoFront.Builder dummyPfB = createDummyPfBuilder(0x33333333L);
        pb.setForTrip(0, dummyPfB);
        assertSame(dummyPfB, pb.forTrip(0), "forTrip(0) should return the builder that was set");
    }

    // --- Tests for build() method ---

    @Test
    void buildReturnsProfileWithEmptyFrontiersForUnsetStations() {
        Profile.Builder pb = new Profile.Builder(dummyTT, testDate, arrivalStationId);
        // Only set builder for station index 1.
        ParetoFront.Builder dummyPfB = createDummyPfBuilder(0x44444444L);
        pb.setForStation(1, dummyPfB);
        Profile p = pb.build();
        // Expect profile.stationFront() list size equals 3.
        assertEquals(3, p.stationFront().size());
        // For station 0 and 2, since no builder was set, they should equal ParetoFront.EMPTY.
        assertSame(ParetoFront.EMPTY, p.forStation(0), "Unset station frontier should be ParetoFront.EMPTY");
        assertSame(ParetoFront.EMPTY, p.forStation(2), "Unset station frontier should be ParetoFront.EMPTY");
        // For station 1, check that the built ParetoFront is as built by dummyPfB.
        ParetoFront built = dummyPfB.build();
    }

    @Test
    void buildReturnsProfileWithCorrectTimeTableAndDate() {
        Profile.Builder pb = new Profile.Builder(dummyTT, testDate, arrivalStationId);
        // Build without setting any station frontier.
        Profile p = pb.build();
        assertSame(dummyTT, p.timeTable());
        assertEquals(testDate, p.date());
        assertEquals(arrivalStationId, p.arrStationId());
    }

    @Test
    void buildProfileStationFrontIsImmutable() {
        Profile.Builder pb = new Profile.Builder(dummyTT, testDate, arrivalStationId);
        Profile p = pb.build();
        List<ParetoFront> fronts = p.stationFront();
        assertThrows(UnsupportedOperationException.class, () -> fronts.remove(0));
    }
}

