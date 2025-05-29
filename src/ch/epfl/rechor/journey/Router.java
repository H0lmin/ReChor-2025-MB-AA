package ch.epfl.rechor.journey;

import ch.epfl.rechor.Bits32_24_8;
import ch.epfl.rechor.PackedRange;
import ch.epfl.rechor.timetable.Connections;
import ch.epfl.rechor.timetable.TimeTable;
import ch.epfl.rechor.timetable.Transfers;

import java.time.LocalDate;
import java.util.Arrays;

import static ch.epfl.rechor.journey.PackedCriteria.*;

/**
 * Computes Pareto-optimal {@link Profile}s for reaching a given destination on a specific date,
 * based on connections and transfers in a {@link TimeTable}.
 *
 * @author Amine AMIRA (393410)
 * @author Malak Berrada (379791)
 */
public record Router(TimeTable timeTable) {

    /**
     * Builds a {@link Profile} of all optimal journeys arriving at {@code destStation} on
     * {@code date}.
     */
    public Profile profile(LocalDate date, int destStation) {
        Transfers transfers = timeTable.transfers();
        int[] walkingTimes = computeWalkingTimes(transfers, destStation);

        Connections connections = timeTable.connectionsFor(date);
        Profile.Builder builder = new Profile.Builder(timeTable, date, destStation);

        for (int connectionId = 0; connectionId < connections.size(); connectionId++) {
            processConnection(builder, connections, transfers, walkingTimes, connectionId);
        }

        return builder.build();
    }

    /**
     * Calculates, for each station, minutes to walk to the destination (or –1 if none).
     */
    private int[] computeWalkingTimes(Transfers transfers, int destStation) {
        int stationCount = timeTable.stations().size();
        int[] times = new int[stationCount];
        Arrays.fill(times, -1);

        try {
            int interval = transfers.arrivingAt(destStation);
            int startIndex = PackedRange.startInclusive(interval);
            int endIndex = PackedRange.endExclusive(interval);
            for (int i = startIndex; i < endIndex; i++) {
                int origin = transfers.depStationId(i);
                times[origin] = transfers.minutes(i);
            }
        } catch (IndexOutOfBoundsException ignored) {
        }

        return times;
    }

    /**
     * Handles all three routing-options for a single connection.
     */
    private void processConnection(Profile.Builder builder,
                                  Connections connections,
                                  Transfers transfers,
                                  int[] walkingTimes,
                                  int connectionId) {
        int depStop = connections.depStopId(connectionId);
        int arrStop = connections.arrStopId(connectionId);
        int depTime = connections.depMins(connectionId);
        int arrTime = connections.arrMins(connectionId);
        int tripId = connections.tripId(connectionId);
        int payload = Bits32_24_8.pack(connectionId, 0);

        int depStation = timeTable.stationId(depStop);
        int arrStation = timeTable.stationId(arrStop);

        ParetoFront.Builder frontier = new ParetoFront.Builder();

        addWalkOption(frontier, walkingTimes, arrStation, arrTime, payload);
        addContinueOption(frontier, builder, tripId);
        addChangeOption(frontier, builder, arrStation, arrTime, payload);

        if (frontier.isEmpty()) return;

        updateTripFrontier(builder, tripId, frontier);
        updateStationFrontiers(builder, frontier, transfers, connections, depStation, depTime,
                connectionId);
    }

    /**
     * Option 1: walk from arrival station to destination if possible.
     */
    private void addWalkOption(ParetoFront.Builder frontier,
                               int[] walkingTimes,
                               int arrivalStation,
                               int arrivalTime,
                               int payload) {
        int walk = walkingTimes[arrivalStation];
        if (walk != -1) {
            frontier.add(arrivalTime + walk, 0, payload);
        }
    }

    /**
     * Option 2: stay on the same trip.
     */
    private void addContinueOption(ParetoFront.Builder frontier,
                                   Profile.Builder builder,
                                   int tripId) {
        ParetoFront.Builder tripFront = builder.forTrip(tripId);
        if (tripFront != null) {
            frontier.addAll(tripFront);
        }
    }

    /**
     * Option 3: change at this arrival station.
     */
    private void addChangeOption(ParetoFront.Builder frontier,
                                 Profile.Builder builder,
                                 int arrivalStation,
                                 int arrivalTime,
                                 int payload) {
        if (builder.forStation(arrivalStation) == null) {
            builder.setForStation(arrivalStation, new ParetoFront.Builder());
        }
        ParetoFront.Builder stationFront = builder.forStation(arrivalStation);
        stationFront.forEach(tuple -> {
            if (hasDepMins(tuple) && depMins(tuple) >= arrivalTime) {
                frontier.add(withPayload(withoutDepMins(withAdditionalChange(tuple)), payload));
            }
        });
    }

    /**
     * Merges this connection’s frontier into the trip-specific frontier.
     */
    private void updateTripFrontier(Profile.Builder builder,
                                    int tripId,
                                    ParetoFront.Builder frontier) {
        ParetoFront.Builder tripFront = builder.forTrip(tripId);
        if (tripFront == null) {
            tripFront = new ParetoFront.Builder();
            builder.setForTrip(tripId, tripFront);
        }
        tripFront.addAll(frontier);
    }

    /**
     * Propagates new options backwards through all transfers from the departure station of this
     * connection.
     */
    private void updateStationFrontiers(Profile.Builder builder,
                                        ParetoFront.Builder frontier,
                                        Transfers transfers,
                                        Connections connections,
                                        int departureStation,
                                        int departureTime,
                                        int connectionId) {
        int interval = transfers.arrivingAt(departureStation);
        int startIndex = PackedRange.startInclusive(interval);
        int endIndex = PackedRange.endExclusive(interval);

        for (int i = startIndex; i < endIndex; i++) {
            int originStation = transfers.depStationId(i);
            int transferTime = transfers.minutes(i);
            int newDepTime = departureTime - transferTime;

            if (builder.forStation(originStation) == null) {
                builder.setForStation(originStation, new ParetoFront.Builder());
            } else if (builder.forStation(originStation).fullyDominates(frontier, newDepTime)) {
                continue;
            }

            ParetoFront.Builder stationFront = builder.forStation(originStation);
            frontier.forEach(tuple -> {
                int oldPayload = payload(tuple);
                int prevConnId = Bits32_24_8.unpack24(oldPayload);
                int stopsCount = connections.tripPos(prevConnId) - connections.tripPos(connectionId);
                int newPayload = Bits32_24_8.pack(connectionId, stopsCount);
                long newTuple = withPayload(withDepMins(tuple, newDepTime), newPayload);
                stationFront.add(newTuple);
            });
        }
    }
}