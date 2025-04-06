package ch.epfl.rechor.journey;

import ch.epfl.rechor.Bits32_24_8;
import ch.epfl.rechor.timetable.*;

import java.time.LocalDate;

/**
 * A Router provides the method to compute a Profile from a TimeTable, a date, and an arrival station.
 *
 * @author Amine AMIRA (393410)
 * @author Malak Berrada (379791)
 */
public record Router(TimeTable timeTable) {
    private static final int UNDEFINED = -1;

    /**
     * Computes a Profile with optimal journeys to the given arrival station on the given date.
     *
     * @param date the date of the journey
     * @param arrStationId the id of the arrival station
     * @return the computed Profile
     */
    public Profile profile(LocalDate date, int arrStationId) {
        var transfers = timeTable.transfers();
        var connections = timeTable.connectionsFor(date);
        var trips = timeTable.tripsFor(date);

        var profileBuilder = new Profile.Builder(timeTable, date, arrStationId);


        int stationCount = timeTable.stations().size();
        for (int stationId = 0; stationId < stationCount; stationId++) {
            profileBuilder.setForStation(stationId, new ParetoFront.Builder());
        }

        int tripCount = trips.size();
        for (int tripId = 0; tripId < tripCount; tripId++) {
            profileBuilder.setForTrip(tripId, new ParetoFront.Builder());
        }


        profileBuilder.forStation(arrStationId).add(0L);


        for (int i = connections.size() - 1; i >= 0; i--) {
            int connId = i;

            int depStopId = connections.depStopId(connId);
            int arrStopId = connections.arrStopId(connId);
            int depMins = connections.depMins(connId);
            int arrMins = connections.arrMins(connId);
            int tripId = connections.tripId(connId);

            int depStationId = timeTable.stationId(depStopId);
            int arrStationIdCon = timeTable.stationId(arrStopId);

            long newCriterion = UNDEFINED;

            ParetoFront.Builder tripBuilder = profileBuilder.forTrip(tripId);
            long continuation = UNDEFINED;
            if (tripBuilder != null) {
                final long[] candidate = {UNDEFINED};  // Use an array to allow mutation in lambda
                tripBuilder.build().forEach(criterion -> {
                    if (PackedCriteria.arrMins(criterion) >= arrMins) {
                        if (candidate[0] == UNDEFINED ||
                                PackedCriteria.arrMins(criterion) < PackedCriteria.arrMins(candidate[0]) ||
                                (PackedCriteria.arrMins(criterion) == PackedCriteria.arrMins(candidate[0]) && PackedCriteria.changes(criterion) < PackedCriteria.changes(candidate[0]))) {
                            candidate[0] = criterion;
                        }
                    }
                });
                continuation = candidate[0];
            }
            if (continuation != UNDEFINED) {
                int arrMinsBest = PackedCriteria.arrMins(continuation);
                int changes = PackedCriteria.changes(continuation);
                long criterionWithoutDepmins = PackedCriteria.pack(arrMinsBest, changes, Bits32_24_8.pack(connId, 0));
                newCriterion=PackedCriteria.withDepMins(criterionWithoutDepmins, depMins);
            }


            ParetoFront.Builder arrStationBuilder = profileBuilder.forStation(arrStationIdCon);
            long change = UNDEFINED;
            if (arrStationBuilder != null) {
                final long[] candidate = {UNDEFINED};  // Use an array to allow mutation in lambda
                arrStationBuilder.build().forEach(criterion -> {
                    if (PackedCriteria.arrMins(criterion) >= arrMins) {
                        if (candidate[0] == UNDEFINED ||
                                PackedCriteria.arrMins(criterion) < PackedCriteria.arrMins(candidate[0]) ||
                                (PackedCriteria.arrMins(criterion) == PackedCriteria.arrMins(candidate[0]) && PackedCriteria.changes(criterion) < PackedCriteria.changes(candidate[0]))) {
                            candidate[0] = criterion;
                        }
                    }
                });
                change = candidate[0];
            }
            if (change != UNDEFINED) {
                int arrMinsBest = PackedCriteria.arrMins(change);
                int changes = PackedCriteria.changes(change);
                long criterionWithoutDepmins = PackedCriteria.pack(arrMinsBest, changes + 1, Bits32_24_8.pack(connId, 0));
                long criterionWithChange = PackedCriteria.withDepMins(criterionWithoutDepmins, depMins);
                newCriterion = selectBetterCriterion(newCriterion, criterionWithChange);
            }


            if (newCriterion != UNDEFINED) {
                profileBuilder.forStation(depStationId).add(newCriterion);
                profileBuilder.setForTrip(tripId, tripBuilder);
                profileBuilder.forTrip(tripId).add(newCriterion);

                for (int transferId = 0; transferId < transfers.size(); transferId++) {
                    if (transfers.depStationId(transferId) == depStationId) {
                        int toStationId = transfers.arrivingAt(transferId);
                        int duration = transfers.minutes(transferId);

                        int newDepMins = depMins - duration;
                        if (newDepMins >= 0) {
                            long CriterionWithoutDepMins = PackedCriteria.pack(
                                    PackedCriteria.arrMins(newCriterion),
                                    PackedCriteria.changes(newCriterion),
                                    PackedCriteria.payload(newCriterion));
                            long transferCriterion=PackedCriteria.withDepMins(CriterionWithoutDepMins,newDepMins);
                            profileBuilder.forStation(toStationId).add(transferCriterion);
                        }
                    }
                }
            }
        }

        return profileBuilder.build();
    }

    private static long selectBetterCriterion(long c1, long c2) {
        if (c1 == UNDEFINED) return c2;
        if (c2 == UNDEFINED) return c1;

        int arr1 = PackedCriteria.arrMins(c1);
        int arr2 = PackedCriteria.arrMins(c2);
        if (arr1 != arr2) return arr1 < arr2 ? c1 : c2;

        int changes1 = PackedCriteria.changes(c1);
        int changes2 = PackedCriteria.changes(c2);
        return changes1 <= changes2 ? c1 : c2;
    }
}
