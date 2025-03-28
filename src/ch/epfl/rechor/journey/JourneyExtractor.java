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


public class JourneyExtractor {
    private JourneyExtractor () {
    }

    public static List<Journey> journeys (Profile profile, int depStationId) {
        List<Journey> journeys = new ArrayList<>();

        profile.forStation(depStationId).
                forEach(criteria ->
                        journeys.add(extractJourney(profile, depStationId, criteria))
                );
        journeys.sort(Comparator
                .comparing(Journey::depTime)
                .thenComparing(Journey::arrTime));
        return journeys;
    }

    private static Journey extractJourney (Profile profile, int depStationId, long criteria) {
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
            long tuple = profile.forStation(currentStationId).get(targetArrMins, remainingChanges - i);
            int connId = Bits32_24_8.unpack24(PackedCriteria.payload(tuple));
            int numInterStops = Bits32_24_8.unpack8(PackedCriteria.payload(tuple));

            ConnectionInfo nextConn = extractConnectionInfo(conns, tt, date, connId);

            if(i != 0 || tt.stationId(currentStopId) != tt.stationId(conns.depStopId(connId))){
                addFootLeg(legs, tt, currentStopId, nextConn.depStopId(), currentTime);
            }

            LegExtractionResult result = addTransportLeg(conns, tt, date, profile, nextConn, numInterStops);
            legs.add(result.leg());

            currentTime = result.newCurrentTime();
            currentStopId = result.newCurrentStation();
            currentStationId = tt.stationId(currentStopId);
        }

        if (currentStationId != profile.arrStationId())
            addFootLeg(legs, tt, currentStopId, profile.arrStationId(), currentTime);

        System.out.println(legs);

        return new Journey(legs);
    }


    private static LegExtractionResult addTransportLeg (Connections conns,
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
        String route = String.valueOf(routeId);
        String destination = profile.trips().destination(current.tripId());

        Journey.Leg.Transport leg = new Journey.Leg.Transport(
                start.depStop,
                start.depTime(),
                current.arrStop,
                current.arrTime(),
                interStops,
                tt.routes().vehicle(routeId),
                route,
                destination
        );
        return new LegExtractionResult(leg, current.arrTime(), current.arrStopId());
    }


    private static ConnectionInfo extractConnectionInfo (Connections conns, TimeTable tt, LocalDate date, int connId) {
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


    private static LocalDateTime toDateTime (LocalDate date, int minutes) {
        return date.atStartOfDay().plusMinutes(minutes);
    }

    private static void addFootLeg (List<Journey.Leg> legs,
                                    TimeTable tt,
                                    int fromStopId,
                                    int toStopId,
                                    LocalDateTime currentTime) {
        int fromStation = tt.stationId(fromStopId);
        int toStation = tt.stationId(toStopId);
        int walkingMinutes = tt.transfers().minutesBetween(fromStation, toStation);

        Stop fromStop = stopOf(tt, fromStopId);
        Stop toStop = stopOf(tt, toStopId);

        LocalDateTime footArrTime = currentTime.plus(Duration.ofMinutes(walkingMinutes));
        Journey.Leg.Foot footLeg = new Journey.Leg.Foot(fromStop, currentTime, toStop, footArrTime);
        legs.add(footLeg);
    }


    private static Stop stopOf (TimeTable tt, int stopId) {
        int stationId = tt.stationId(stopId);
        String name = tt.stations().name(stationId);
        String platformName = tt.platformName(stopId);
        double lon = tt.stations().longitude(stationId);
        double lat = tt.stations().latitude(stationId);

        return new Stop(name, platformName, lon, lat);
    }

    private record ConnectionInfo(int connectionId, int depStopId, Stop depStop, LocalDateTime depTime,
                                  int arrStopId, Stop arrStop, LocalDateTime arrTime, int tripId) {
    }

    private record LegExtractionResult(Journey.Leg.Transport leg, LocalDateTime newCurrentTime, int newCurrentStation) {
    }
}