package ch.epfl.rechor.journey;

import ch.epfl.rechor.timetable.TimeTable;
import ch.epfl.rechor.timetable.Connections;
import ch.epfl.rechor.timetable.Transfers;
import ch.epfl.rechor.PackedRange;
import ch.epfl.rechor.Bits32_24_8;

import java.time.LocalDate;
import java.util.NoSuchElementException;

record Router(TimeTable timeTable) {

    public Profile profile(LocalDate date, int destStation) {
        return new CSA(date, destStation).computeProfile();
    }

    private class CSA {
        private final Connections connections;
        private final Transfers transfers;
        private final int[] walkTimes;
        private final Profile.Builder profileBuilder;

        public CSA(LocalDate date, int destStation) {
            this.connections = timeTable.connectionsFor(date);
            this.transfers = timeTable.transfers();
            int stationCount = timeTable.stations().size();
            this.walkTimes = new int[stationCount];
            for (int s = 0; s < stationCount; s++) {
                try {
                    walkTimes[s] = transfers.minutesBetween(s, destStation);
                } catch (NoSuchElementException e) {
                    walkTimes[s] = -1;
                }
            }
            this.profileBuilder = new Profile.Builder(timeTable, date, destStation);
            // Initialize frontiers for all stations.
            for (int stationId = 0; stationId < stationCount; stationId++) {
                profileBuilder.setForStation(stationId, new ParetoFront.Builder());
            }
            int tripCount = timeTable.tripsFor(date).size();
            for (int tripId = 0; tripId < tripCount; tripId++) {
                profileBuilder.setForTrip(tripId, new ParetoFront.Builder());
            }
            // Seed the destination station’s frontier properly.
            long destSeed = PackedCriteria.withDepMins(PackedCriteria.pack(1440, 0, 0), 1440);
            profileBuilder.forStation(destStation).add(destSeed);
        }

        public Profile computeProfile() {
            for (int connId = 0; connId < connections.size(); connId++) {
                processConnection(connId);
            }
            return profileBuilder.build();
        }

        private void processConnection(int connId) {
            int depStop = connections.depStopId(connId);
            int arrStop = connections.arrStopId(connId);
            int depStation = timeTable.stationId(depStop);
            int arrStation = timeTable.stationId(arrStop);
            int depTime = connections.depMins(connId);
            int arrTime = connections.arrMins(connId);
            int tripId = connections.tripId(connId);

            ParetoFront.Builder frontier = new ParetoFront.Builder();

            // Option 1: Walking option.
            int transferTime = walkTimes[arrStation];
            if (transferTime != -1) {
                int candidateArrTime = arrTime + transferTime;
                long tuple = PackedCriteria.pack(candidateArrTime, 0, Bits32_24_8.pack(connId, 0));
                tuple = PackedCriteria.withDepMins(tuple, depTime);
                frontier.add(tuple);
            }

            // Option 2: Continue with the same trip.
            ParetoFront.Builder tripBuilder = getTripBuilder(tripId);
            tripBuilder.forEach(frontier::add);

            // Option 3: Change vehicle (transfer) at the arrival station.
            ParetoFront.Builder arrivalBuilder = profileBuilder.forStation(arrStation);
            if (arrivalBuilder != null) {
                arrivalBuilder.forEach(tuple -> {
                    if (((!PackedCriteria.hasDepMins(tuple)) || PackedCriteria.depMins(tuple) >= arrTime)
                            && PackedCriteria.arrMins(tuple) >= arrTime) {
                        int candidateArrTime = PackedCriteria.arrMins(tuple);
                        int candidateChanges = PackedCriteria.changes(tuple) + 1;
                        // Use the current connection as the starting leg for the transfer.
                        int interStops = 0;
                        long newTuple = PackedCriteria.pack(candidateArrTime, candidateChanges,
                                Bits32_24_8.pack(connId, interStops));
                        newTuple = PackedCriteria.withDepMins(newTuple, depTime);
                        frontier.add(newTuple);
                    }
                });
            }

            // Option 4: Update the trip frontier.
            getTripBuilder(tripId).addAll(frontier);

            // Option 5: Update station frontiers via transfers.
            int interval = transfers.arrivingAt(depStation);
            int start = PackedRange.startInclusive(interval);
            int end = PackedRange.endExclusive(interval);
            for (int i = start; i < end; i++) {
                int transferDepStation = transfers.depStationId(i);
                int transferDuration = transfers.minutes(i);
                int newDepTime = depTime - transferDuration;
                // Only update if the new departure time is nonnegative.
                if (newDepTime >= 0) {
                    ParetoFront.Builder candidateBuilder = new ParetoFront.Builder();
                    frontier.forEach(tuple -> {
                        int candidateArrTime = PackedCriteria.arrMins(tuple);
                        int candidateChanges = PackedCriteria.changes(tuple);
                        long newTuple = PackedCriteria.pack(candidateArrTime, candidateChanges,
                                PackedCriteria.payload(tuple));
                        newTuple = PackedCriteria.withDepMins(newTuple, newDepTime);
                        candidateBuilder.add(newTuple);
                    });
                    getStationBuilder(transferDepStation).addAll(candidateBuilder);
                }
            }
// Self-transfer: update the departure station frontier.
// Adjust tuples if their departure time equals the connection's depTime to force a foot leg.
            ParetoFront.Builder adjustedFrontier = new ParetoFront.Builder();
            frontier.forEach(tuple -> {
                int candidateDep = PackedCriteria.depMins(tuple);
                if (candidateDep == depTime && depTime > 0) {
                    tuple = PackedCriteria.withDepMins(tuple, depTime - 1);
                }
                adjustedFrontier.add(tuple);
            });
            getStationBuilder(depStation).addAll(adjustedFrontier);

        }

        private ParetoFront.Builder getStationBuilder(int stationId) {
            ParetoFront.Builder builder = profileBuilder.forStation(stationId);
            if (builder == null) {
                builder = new ParetoFront.Builder();
                profileBuilder.setForStation(stationId, builder);
            }
            return builder;
        }

        private ParetoFront.Builder getTripBuilder(int tripId) {
            ParetoFront.Builder builder = profileBuilder.forTrip(tripId);
            if (builder == null) {
                builder = new ParetoFront.Builder();
                profileBuilder.setForTrip(tripId, builder);
            }
            return builder;
        }
    }
}
