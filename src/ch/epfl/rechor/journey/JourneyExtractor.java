package ch.epfl.rechor.journey;

import ch.epfl.rechor.Bits32_24_8;
import ch.epfl.rechor.timetable.Connections;
import ch.epfl.rechor.timetable.TimeTable;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * The {@code JourneyExtractor} class provides static methods to extract journey itineraries from a
 * given travel profile. It builds a journey composed of several legs (transport and foot segments)
 * based on departure and arrival information.
 *
 * @author Amine AMIRA (393410)
 * @author Malak Berrada (379791)
 */
public class JourneyExtractor {

    private JourneyExtractor() {
    }

    /**
     * Extracts and returns a list of journeys starting from the specified departure station
     * according to the given profile.
     *
     * @param profile      the travel profile containing timetable, connections, and criteria.
     * @param depStationId the departure station ID.
     * @return a sorted list of {@link Journey} objects based on departure and arrival times.
     */
    public static List<Journey> journeys(Profile profile, int depStationId) {
        List<Journey> journeys = new ArrayList<>();

        profile.forStation(depStationId)
                .forEach(criteria ->
                        journeys.add(extractJourney(profile, depStationId, criteria)));

        journeys.sort(Comparator.comparing(Journey::depTime)
                .thenComparing(Journey::arrTime));
        return journeys;
    }

    /**
     * Extracts a single journey based on the provided criteria.
     *
     * @param profile      the travel profile containing timetable, connections, and criteria.
     * @param depStationId the departure station ID.
     * @param criteria     a long value encoding the journey extraction criteria.
     * @return a {@link Journey} built from a series of legs (transport and foot segments).
     */
    private static Journey extractJourney(Profile profile, int depStationId, long criteria) {
        TimeTable tt = profile.timeTable();
        Connections conns = profile.connections();
        LocalDate date = profile.date();

        int initialDepMins = PackedCriteria.depMins(criteria);
        int targetArrMins = PackedCriteria.arrMins(criteria);
        int remainingChanges = PackedCriteria.changes(criteria);

        LocalDateTime currentTime = toDateTime(date, initialDepMins);
        int currentStopId = depStationId;
        int currentStationId = tt.stationId(depStationId);
        List<Journey.Leg> legs = new ArrayList<>();

        for (int i = 0; i <= remainingChanges; i++) {
            long tuple = profile.forStation(currentStationId).get(targetArrMins,
                    remainingChanges - i);
            int payload = PackedCriteria.payload(tuple);
            int connId = Bits32_24_8.unpack24(payload);
            int numInterStops = Bits32_24_8.unpack8(payload);

            ConnectionInfo nextConn = extractConnectionInfo(conns, tt, date, connId);

            // Add a foot leg if necessary before a transport leg.
            if (i != 0 || currentStationId != tt.stationId(conns.depStopId(connId))) {
                legs.add(addFootLeg(tt, currentStopId, nextConn.depStopId(), currentTime));
            }

            LegExtractionResult result = addTransportLeg(conns, tt, date, profile, nextConn,
                    numInterStops);
            legs.add(result.leg());

            currentTime = result.newCurrentTime();
            currentStopId = result.newCurrentStation();
            currentStationId = tt.stationId(currentStopId);
        }

        if (currentStationId != profile.arrStationId()) {
            legs.add(addFootLeg(tt, currentStopId, profile.arrStationId(), currentTime));
        }

        return new Journey(legs);
    }

    /**
     * Adds a transport leg to the journey, extracting intermediate stops if present.
     *
     * @param conns         the connections data from the timetable.
     * @param tt            the timetable instance.
     * @param date          the date for the journey.
     * @param profile       the travel profile containing trip information.
     * @param start         the starting connection information.
     * @param numInterStops the number of intermediate stops between the start and end of the leg.
     * @return a {@link LegExtractionResult} containing the created transport leg, the updated
     * current time, and the updated current station.
     */
    private static LegExtractionResult addTransportLeg(Connections conns,
                                                       TimeTable tt,
                                                       LocalDate date,
                                                       Profile profile,
                                                       ConnectionInfo start,
                                                       int numInterStops) {
        List<Journey.Leg.IntermediateStop> interStops = new ArrayList<>();
        ConnectionInfo current = start;

        for (int i = 0; i < numInterStops; i++) {
            int nextId = conns.nextConnectionId(current.connectionId());
            ConnectionInfo next = extractConnectionInfo(conns, tt, date, nextId);

            interStops.add(new Journey.Leg.IntermediateStop(
                    current.arrStop(), current.arrTime(), next.depTime()
            ));
            current = next;
        }

        int routeId = profile.trips().routeId(current.tripId());
        String route = String.valueOf(routeId);
        String destination = profile.trips().destination(current.tripId());

        Journey.Leg.Transport leg = new Journey.Leg.Transport(
                start.depStop(), start.depTime(),
                current.arrStop(), current.arrTime(),
                interStops,
                tt.routes().vehicle(routeId),
                route,
                destination
        );
        return new LegExtractionResult(leg, current.arrTime(), current.arrStopId());
    }

    private static ConnectionInfo extractConnectionInfo(Connections conns, TimeTable tt,
                                                        LocalDate date, int connId) {
        int depStopId = conns.depStopId(connId);
        int arrStopId = conns.arrStopId(connId);
        int depMins = conns.depMins(connId);
        int arrMins = conns.arrMins(connId);
        int tripId = conns.tripId(connId);

        return new ConnectionInfo(
                connId,
                depStopId,
                stopOf(tt, depStopId),
                toDateTime(date, depMins),
                arrStopId,
                stopOf(tt, arrStopId),
                toDateTime(date, arrMins),
                tripId
        );
    }

    /**
     * Converts a given number of minutes since the start of the day into a {@link LocalDateTime}.
     *
     * @param date    the date.
     * @param minutes the number of minutes since midnight.
     * @return a {@link LocalDateTime} representing the specified time on the given date.
     */
    private static LocalDateTime toDateTime(LocalDate date, int minutes) {
        return date.atStartOfDay().plusMinutes(minutes);
    }

    private static Journey.Leg.Foot addFootLeg(TimeTable tt,
                                               int fromStopId,
                                               int toStopId,
                                               LocalDateTime currentTime) {
        int fromStation = tt.stationId(fromStopId);
        int toStation = tt.stationId(toStopId);
        int walkingMinutes = tt.transfers().minutesBetween(fromStation, toStation);


        Stop fromStop = stopOf(tt, fromStopId);
        Stop toStop = stopOf(tt, toStopId);

        LocalDateTime footArrTime = currentTime.plus(Duration.ofMinutes(walkingMinutes));
        return new Journey.Leg.Foot(fromStop, currentTime, toStop, footArrTime);
    }

    /**
     * Retrieves a {@link Stop} object for the given stop ID from the timetable.
     */
    private static Stop stopOf(TimeTable tt, int stopId) {
        int stationId = tt.stationId(stopId);
        String name = tt.stations().name(stationId);
        String platformName = tt.platformName(stopId);
        double lon = tt.stations().longitude(stationId);
        double lat = tt.stations().latitude(stationId);

        return new Stop(name, platformName, lon, lat);
    }

    private record ConnectionInfo(int connectionId,
                                  int depStopId, Stop depStop, LocalDateTime depTime,
                                  int arrStopId, Stop arrStop, LocalDateTime arrTime,
                                  int tripId) {
    }

    /**
     * A record representing the result of extracting a transport leg, including the leg itself,
     * the new current time after the leg, and the new current station.
     *
     * @param leg               the transport leg that was extracted.
     * @param newCurrentTime    the updated time after completing the leg.
     * @param newCurrentStation the updated stop ID after completing the leg.
     */
    private record LegExtractionResult(Journey.Leg.Transport leg,
                                             LocalDateTime newCurrentTime,
                                       int newCurrentStation) {
    }
}
