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
 * Extracts journeys using the method journeys
 *
 * @author Amine AMIRA (393410)
 * @author Malak Berrada (379791)
 */

public final class JourneyExtractor {
    private JourneyExtractor() {
    }
    /**
     * Packs the arrival time, number of changes, and payload into a 64-bit long.
     * @param profile the Profile of the journeys
     * @param depStationId the id of the station of departure
     * @return a List<Journey> of the journeys extracted
     */
    public static List<Journey> journeys(Profile profile, int depStationId) {
        List<Journey> journeys = new ArrayList<>();

        profile.forStation(depStationId).
                forEach(criteria ->
                        journeys.add(extractJourney(profile, depStationId, criteria))
                );
        journeys.sort(Comparator.comparing(Journey::depTime).
                thenComparing(Journey::arrTime));
        return journeys;
    }

    /**
     * private method to extract a journey using the profile, the id of the station of departure
     * and the criteria extracted in the public method journeys
     */
    private static Journey extractJourney(Profile profile, int depStationId, long criteria) {
        TimeTable tt = profile.timeTable();
        Connections conns = profile.connections();
        LocalDate date = profile.date();

        int initialDepMins = PackedCriteria.depMins(criteria);
        int targetArrMins = PackedCriteria.arrMins(criteria);
        int remainingChanges = PackedCriteria.changes(criteria);

        LocalDateTime currentTime = toDateTime(date, initialDepMins);
        int currentStation = tt.stationId(depStationId);
        List<Journey.Leg> legs = new ArrayList<>();

        for (int i = 0; i <= remainingChanges ; i++) {
            long tuple = profile.forStation(currentStation).get(targetArrMins, remainingChanges - i);

            int connId = Bits32_24_8.unpack24(PackedCriteria.payload(tuple));
            int numInterStops = Bits32_24_8.unpack8(PackedCriteria.payload(tuple));

            ConnectionInfo nextConn = extractConnectionInfo(conns, tt, date, connId);

            addFootLeg(legs, tt, currentStation, tt.stationId(nextConn.depStopId()), currentTime);
            LegExtractionResult result = buildTransportLeg(conns, tt, date, profile, nextConn , numInterStops);
            legs.add(result.leg());

            currentTime = result.newCurrentTime();
            currentStation = tt.stationId(result.newCurrentStation());
        }

        if (currentStation != profile.arrStationId())
            addFootLeg(legs, tt, currentStation, profile.arrStationId(), currentTime);

        return new Journey(legs);
    }

    private static LegExtractionResult buildTransportLeg(Connections conns,
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
                    current.arrStop(),
                    current.arrTime(),
                    next.depTime()
            ));
            current = next;
        }

        int routeId = profile.trips().routeId(current.tripId());
        String route = String.valueOf(routeId);  // Keep raw route ID
        String destination = profile.trips().destination(current.tripId());

        Journey.Leg.Transport leg = new Journey.Leg.Transport(
                start.depStop(),
                start.depTime(),
                current.arrStop(),
                current.arrTime(),
                interStops,
                tt.routes().vehicle(routeId),
                route,
                destination
        );
        return new LegExtractionResult(leg, current.arrTime(), current.arrStopId());
    }


    private static ConnectionInfo extractConnectionInfo(Connections conns, TimeTable tt, LocalDate date, int connId) {
        int depStopId = conns.depStopId(connId);
        int arrStopId = conns.arrStopId(connId);
        int depMins = conns.depMins(connId);
        int arrMins = conns.arrMins(connId);
        int tripId = conns.tripId(connId);

        return new ConnectionInfo(
                connId,
                depStopId,
                stopOf(tt, tt.stationId(depStopId)),
                toDateTime(date, depMins),
                arrStopId,
                stopOf(tt, tt.stationId(arrStopId)),
                toDateTime(date, arrMins),
                tripId
        );
    }

    private static LocalDateTime toDateTime(LocalDate date, int minutes) {
        return date.atStartOfDay().plusMinutes(minutes);
    }

    private static void addFootLeg(List<Journey.Leg> legs,
                                   TimeTable tt,
                                   int fromStationId,
                                   int toStationId,
                                   LocalDateTime currentTime) {

        int walkingMinutes = tt.transfers().minutesBetween(fromStationId, toStationId);

        Stop fromStop = stopOf(tt, fromStationId);
        Stop toStop = stopOf(tt, toStationId);

        LocalDateTime footArrTime = currentTime.plus(Duration.ofMinutes(walkingMinutes));
        Journey.Leg.Foot footLeg = new Journey.Leg.Foot(fromStop, currentTime, toStop, footArrTime);

        legs.add(footLeg);
    }

    private static Stop stopOf(TimeTable tt, int stopId) {
        int stationId = tt.stationId(stopId);
        String name = tt.stations().name(stationId);
        String rawPlatform = tt.platformName(stopId);
        double lon = tt.stations().longitude(stationId);
        double lat = tt.stations().latitude(stationId);

        return new Stop(name, rawPlatform, lon, lat);
    }

    private record ConnectionInfo(int connectionId, int depStopId, Stop depStop, LocalDateTime depTime,
                                  int arrStopId, Stop arrStop, LocalDateTime arrTime, int tripId) {
    }

    private record LegExtractionResult(Journey.Leg.Transport leg, LocalDateTime newCurrentTime, int newCurrentStation) {
    }
}
