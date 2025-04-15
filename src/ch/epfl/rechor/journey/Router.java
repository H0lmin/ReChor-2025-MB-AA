package ch.epfl.rechor.journey;

import ch.epfl.rechor.Bits32_24_8;
import ch.epfl.rechor.PackedRange;
import ch.epfl.rechor.timetable.Connections;
import ch.epfl.rechor.timetable.TimeTable;
import ch.epfl.rechor.timetable.Transfers;
import ch.epfl.rechor.timetable.Trips;

import java.time.LocalDate;
import java.util.Arrays;

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
                int depStation = transfers.depStationId(i);
                int time = transfers.minutes(i);
                walkingTimes[depStation] = time;
            }
        } catch (IndexOutOfBoundsException ignored) {
        }

        Trips trips = timeTable.tripsFor(date);
        Connections conns = timeTable.connectionsFor(date);

        ParetoFront.Builder[] stationBuilders = new ParetoFront.Builder[numStations];
        ParetoFront.Builder[] tripBuilders = new ParetoFront.Builder[trips.size()];

        for (int i = 0; i < conns.size(); i++) {
            int depStop = conns.depStopId(i);
            int arrStop = conns.arrStopId(i);
            int h_dep = conns.depMins(i);
            int h_arr = conns.arrMins(i);
            int tripId = conns.tripId(i);

            ParetoFront.Builder f = new ParetoFront.Builder();

            int stationArr = timeTable.stationId(arrStop);
            int walkTime = walkingTimes[stationArr];
            if (walkTime >= 0) {
                int candidateTime = h_arr + walkTime;
                int payload = Bits32_24_8.pack(i, 0);
                f.add(candidateTime, 0, payload);
            }

            if (tripBuilders[tripId] != null) {
                tripBuilders[tripId].forEach(f::add);
            }

            if (stationBuilders[stationArr] != null) {
                int finalI = i;
                stationBuilders[stationArr].forEach(t -> {
                    if (PackedCriteria.hasDepMins(t)) {
                        int t_dep = PackedCriteria.depMins(t);
                        if (t_dep >= h_arr) {
                            int newChanges = PackedCriteria.changes(t) + 1;
                            int payload = Bits32_24_8.pack(finalI, 0);
                            int arrCandidate = PackedCriteria.arrMins(t);
                            f.add(arrCandidate, newChanges, payload);
                        }
                    }
                });
            }

            if (f.isEmpty()) continue;

            if (tripBuilders[tripId] == null) {
                tripBuilders[tripId] = new ParetoFront.Builder();
            }
            tripBuilders[tripId].addAll(f);

            int stationDep = timeTable.stationId(depStop);
            int interval = transfers.arrivingAt(stationDep);
            int start = PackedRange.startInclusive(interval);
            int end = PackedRange.endExclusive(interval);
            for (int j = start; j < end; j++) {
                int originStation = transfers.depStationId(j);
                int transferTime = transfers.minutes(j);
                int d = h_dep - transferTime;
                if (stationBuilders[originStation] == null) {
                    stationBuilders[originStation] = new ParetoFront.Builder();
                }
                f.forEach(t -> {
                    long newTuple = PackedCriteria.withDepMins(t, d);
                    stationBuilders[originStation].add(newTuple);
                });
            }
        }

        Profile.Builder profileBuilder = new Profile.Builder(timeTable, date, destStation);
        for (int s = 0; s < numStations; s++) {
            if (stationBuilders[s] != null) {
                profileBuilder.setForStation(s, stationBuilders[s]);
            }
        }
        return profileBuilder.build();
    }
}