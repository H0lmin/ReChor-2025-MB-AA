package ch.epfl.rechor.journey;

import ch.epfl.rechor.Bits32_24_8;
import ch.epfl.rechor.FormatterFr;
import ch.epfl.rechor.timetable.Connections;
import ch.epfl.rechor.timetable.TimeTable;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;

public class JourneyExtractor {
    private JourneyExtractor() {
    }

    public static List<Journey> journeys(Profile profile, int depStationId) {
        List<Journey> journeys = new ArrayList<>();
        profile.forStation(depStationId).forEach(criteria ->
                journeys.add(extractJourney(profile, depStationId, criteria))
        );
        journeys.sort(Comparator.comparing(Journey::depTime).thenComparing(Journey::arrTime));
        return journeys;
    }

    private static Journey extractJourney(Profile profile, int depStationId, long criteria) {
        TimeTable tt = profile.timeTable();
        Connections conns = profile.connections();
        LocalDate date = profile.date();

        int initialDepMins = PackedCriteria.depMins(criteria);
        int targetArrMins = PackedCriteria.arrMins(criteria);
        int remainingChanges = PackedCriteria.changes(criteria);

        LocalDateTime currentTime = toDateTime(date, initialDepMins);
        int currentStation = depStationId;
        List<Journey.Leg> legs = new ArrayList<>();

        while (remainingChanges > 0 && currentStation != profile.arrStationId()) {
            long tuple;
            try {
                tuple = profile.forStation(currentStation).get(targetArrMins, remainingChanges);
            } catch (NoSuchElementException e) {
                addFootLeg(legs, tt, currentStation, profile.arrStationId(), currentTime);
                break;
            }

            int connId = Bits32_24_8.unpack24(PackedCriteria.payload(tuple));
            int numInterStops = Bits32_24_8.unpack8(PackedCriteria.payload(tuple));

            ConnectionInfo firstInfo = extractConnectionInfo(conns, tt, date, connId);
            if (currentStation != tt.stationId(firstInfo.depStopId()))
                addFootLeg(legs, tt, currentStation, tt.stationId(firstInfo.depStopId()), currentTime);

            LegExtractionResult result = buildTransportLeg(conns, tt, date, profile, firstInfo, numInterStops);
            legs.add(result.leg());

            currentTime = result.newCurrentTime();
            currentStation = result.newCurrentStation();
            remainingChanges--;
        }

        if (currentStation != profile.arrStationId())
            addFootLeg(legs, tt, currentStation, profile.arrStationId(), currentTime);

        return new Journey(legs);
    }

    private static LegExtractionResult buildTransportLeg(Connections conns, TimeTable tt, LocalDate date,
                                                         Profile profile, ConnectionInfo start, int numInterStops) {
        List<Journey.Leg.IntermediateStop> interStops = new ArrayList<>();
        ConnectionInfo current = start;

        for (int i = 0; i < numInterStops; i++) {
            int nextId = conns.nextConnectionId(current.connectionId());
            ConnectionInfo next = extractConnectionInfo(conns, tt, date, nextId);
            interStops.add(new Journey.Leg.IntermediateStop(current.arrStop(), current.arrTime(), next.depTime()));
            current = next;
        }

        int routeId = profile.trips().routeId(current.tripId());
        String destination = profile.trips().destination(current.tripId());

        Journey.Leg.Transport tempLeg = new Journey.Leg.Transport(
                start.depStop(),
                start.depTime(),
                current.arrStop(),
                current.arrTime(),
                interStops,
                tt.routes().vehicle(routeId),
                String.valueOf(routeId),
                destination
        );

        String formattedRoute = FormatterFr.formatRouteDestination(tempLeg);

        Journey.Leg.Transport leg = new Journey.Leg.Transport(
                start.depStop(),
                start.depTime(),
                current.arrStop(),
                current.arrTime(),
                interStops,
                tt.routes().vehicle(routeId),
                formattedRoute, destination
        );
        return new LegExtractionResult(leg, current.arrTime(), tt.stationId(current.arrStopId()));
    }

    private static ConnectionInfo extractConnectionInfo(Connections conns, TimeTable tt, LocalDate date, int connId) {
        int depStopId = conns.depStopId(connId);
        int arrStopId = conns.arrStopId(connId);
        int depMins = conns.depMins(connId);
        int arrMins = conns.arrMins(connId);
        int tripId = conns.tripId(connId);

        return new ConnectionInfo(connId, depStopId, stopOf(tt, depStopId), toDateTime(date, depMins),
                arrStopId, stopOf(tt, arrStopId), toDateTime(date, arrMins), tripId);
    }

    private static LocalDateTime toDateTime(LocalDate date, int minutes) {
        return date.atStartOfDay().plusMinutes(minutes);
    }

    private static void addFootLeg(List<Journey.Leg> legs, TimeTable tt, int fromStationId, int toStationId, LocalDateTime currentTime) {
        if (fromStationId == toStationId) return;

        int walkingMinutes;
        try {
            walkingMinutes = tt.transfers().minutesBetween(fromStationId, toStationId);
        } catch (NoSuchElementException e) {
            walkingMinutes = 5;
        }

        Stop fromStop = stopOf(tt, fromStationId);
        Stop toStop = stopOf(tt, toStationId);

        LocalDateTime footArrTime = currentTime.plus(Duration.ofMinutes(walkingMinutes));
        Journey.Leg.Foot footLeg = new Journey.Leg.Foot(fromStop, currentTime, toStop, footArrTime);

        if (!footLeg.isTransfer() || !fromStop.equals(toStop))
            legs.add(footLeg);
    }

    private static Stop stopOf(TimeTable tt, int stopId) {
        int stationId = tt.stationId(stopId);
        String name = tt.stations().name(stationId);
        String rawPlatform = tt.platformName(stopId);
        double lon = tt.stations().longitude(stationId);
        double lat = tt.stations().latitude(stationId);

        Stop tempStop = new Stop(name, rawPlatform, lon, lat);
        String formattedPlatform = FormatterFr.formatPlatformName(tempStop);

        return new Stop(name, formattedPlatform, lon, lat);
    }

    private record ConnectionInfo(int connectionId, int depStopId, Stop depStop, LocalDateTime depTime,
                                  int arrStopId, Stop arrStop, LocalDateTime arrTime, int tripId) {
    }

    private record LegExtractionResult(Journey.Leg.Transport leg, LocalDateTime newCurrentTime, int newCurrentStation) {
    }
}