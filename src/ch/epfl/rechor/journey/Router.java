package ch.epfl.rechor.journey;

import ch.epfl.rechor.Bits32_24_8;
import ch.epfl.rechor.PackedRange;
import ch.epfl.rechor.timetable.Connections;
import ch.epfl.rechor.timetable.TimeTable;
import ch.epfl.rechor.timetable.Transfers;

import java.time.LocalDate;
import java.util.Arrays;

import static ch.epfl.rechor.journey.PackedCriteria.*;

public record Router(TimeTable timeTable) {

    public Profile profile(LocalDate date, int destStation) {
        Transfers transfers = timeTable.transfers();
        int numStations = timeTable.stations().size();
        int[] walkingTimes = new int[numStations];

        Arrays.fill(walkingTimes, -1);
        try {
            int interval = transfers.arrivingAt(destStation);
            int start = PackedRange.startInclusive(interval);
            int end = PackedRange.endExclusive(interval);
            for (int i = start; i < end; i++) {
                walkingTimes[transfers.depStationId(i)] = transfers.minutes(i);
            }
        } catch (IndexOutOfBoundsException ignored) {
        }

        Connections conns = timeTable.connectionsFor(date);

        Profile.Builder profileBuilder = new Profile.Builder(timeTable, date, destStation);

        for (int connId = 0; connId < conns.size(); connId++) {
            int depStop = conns.depStopId(connId);
            int arrStop = conns.arrStopId(connId);
            int h_dep = conns.depMins(connId);
            int h_arr = conns.arrMins(connId);
            int tripId = conns.tripId(connId);
            int finalConnId = connId;
            int payload = Bits32_24_8.pack(connId, 0);

            ParetoFront.Builder f = new ParetoFront.Builder();

            // Option 1: walk from arrival to destination
            int arrStationId = timeTable.stationId(arrStop);
            int walkTime = walkingTimes[arrStationId];
            if (walkTime != -1) {
                int candidateTime = h_arr + walkTime;
                f.add(candidateTime, 0, payload);
            }

            // Option 2: continue on same trip
            if (profileBuilder.forTrip(tripId) != null) {
                f.addAll(profileBuilder.forTrip(tripId));
            }

            // Option 3: change at station
            if (profileBuilder.forStation(arrStationId) == null) {
                profileBuilder.setForStation(arrStationId, new ParetoFront.Builder());
            }

            profileBuilder.forStation(arrStationId).forEach(tuple -> {
                if (PackedCriteria.hasDepMins(tuple)) {
                    int t_dep = PackedCriteria.depMins(tuple);
                    if (t_dep >= h_arr) {
                        f.add(withPayload(withoutDepMins(withAdditionalChange(tuple)), payload));
                    }
                }
            });

            if (f.isEmpty()) continue;

            // Update trip frontier
            ParetoFront.Builder tripFront = profileBuilder.forTrip(tripId);
            if (tripFront == null) {
                tripFront = new ParetoFront.Builder();
                profileBuilder.setForTrip(tripId, tripFront);
            }
            tripFront.addAll(f);

            // Update station frontiers with full-dominance optimization
            int depStationId = timeTable.stationId(depStop);
            int interval = transfers.arrivingAt(depStationId);
            int start = PackedRange.startInclusive(interval);
            int end = PackedRange.endExclusive(interval);
            for (int j = start; j < end; j++) {
                int originStation = transfers.depStationId(j);
                int transferTime = transfers.minutes(j);
                int newDep = h_dep - transferTime;

                if (profileBuilder.forStation(originStation) == null) {
                    profileBuilder.setForStation(originStation, new ParetoFront.Builder());
                } else if (profileBuilder.forStation(originStation).fullyDominates(f, newDep)) {
                    continue;
                }

                f.forEach(t -> {
                    int packedPayload = PackedCriteria.payload(t);
                    int tupleConnId = Bits32_24_8.unpack24(packedPayload);
                    int numInterStops = conns.tripPos(tupleConnId) - conns.tripPos(finalConnId);
                    int effectivePayload = Bits32_24_8.pack(finalConnId, numInterStops);
                    long newTuple = PackedCriteria.withPayload(
                            PackedCriteria.withDepMins(t, newDep), effectivePayload
                    );
                    profileBuilder.forStation(originStation).add(newTuple);
                });
            }
        }

        return profileBuilder.build();
    }
}