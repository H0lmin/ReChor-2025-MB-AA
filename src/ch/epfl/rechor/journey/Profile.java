package ch.epfl.rechor.journey;

import ch.epfl.rechor.timetable.Connections;
import ch.epfl.rechor.timetable.TimeTable;
import ch.epfl.rechor.timetable.Trips;

import java.time.LocalDate;
import java.util.List;

/**
 * Record representing a Profile
 *
 * @param timeTable    the timetable associated with this profile
 * @param date         the date for which the profile is valid
 * @param arrStationId the identifier of the arrival station
 * @param stationFront the list of ParetoFront objects, one for each station
 * @author Amine AMIRA (393410)
 * @author Malak Berrada (379791)
 */

public record Profile(TimeTable timeTable,
                      LocalDate date,
                      int arrStationId,
                      List<ParetoFront> stationFront) {

    /**
     * Compact constructor for {@code Profile}.
     * <p>
     * This constructor defensively copies the provided list of {@link ParetoFront} objects using
     * {@code List.copyOf()} to guarantee that the {@code stationFront} field is immutable.
     * </p>
     */
    public Profile {
        stationFront = List.copyOf(stationFront);
    }

    /**
     * Returns the Connections corresponding to the Profile
     */
    public Connections connections() {
        return timeTable.connectionsFor(date);
    }

    /**
     * Returns the Trips corresponding to the Profile
     */
    public Trips trips() {
        return timeTable.tripsFor(date);
    }

    /**
     * Returns the ParetoFront of the station indexed
     *
     * @param stationId the id of the station corresponding to the ParetoFront
     * @throws IndexOutOfBoundsException if the stationId is not valid
     */
    public ParetoFront forStation(int stationId) {
        return stationFront.get(stationId);
    }

    /**
     * Represents a builder of the record Profile destined to be used to find optimal trips
     */
    public final static class Builder {
        private final TimeTable timeTable;
        private final LocalDate date;
        private final int arrStationId;
        private final ParetoFront.Builder[] stationBuilders;
        private final ParetoFront.Builder[] tripBuilders;

        /**
         * Constructs a new {@code Builder} for creating a {@code Profile}.
         * <p>
         * This constructor initializes the builder with the specified {@link TimeTable},
         * {@link LocalDate}, and arrival station identifier. It also creates arrays to hold
         * {@link ParetoFront.Builder} instances for each station and for each trip.
         * </p>
         *
         * @param timeTable    the timetable associated with the profile
         * @param date         the date for which the profile is valid
         * @param arrStationId the identifier of the arrival station
         */
        public Builder(TimeTable timeTable, LocalDate date, int arrStationId) {
            this.timeTable = timeTable;
            this.date = date;
            this.arrStationId = arrStationId;

            int stationCount = timeTable.stations().size();
            int tripCount = timeTable.tripsFor(date).size();
            this.stationBuilders = new ParetoFront.Builder[stationCount];
            this.tripBuilders = new ParetoFront.Builder[tripCount];
        }

        /**
         * Returns the builder of the ParetoFront corresponding to the station indexed
         *
         * @param stationId the id of the station
         * @return null if there was no call to the method setForStation for this station
         * @throws IndexOutOfBoundsException if the stationId is not valid
         */
        public ParetoFront.Builder forStation(int stationId) {
            if (stationId < 0 || stationId >= stationBuilders.length) {
                throw new IndexOutOfBoundsException("Invalid stationId: " + stationId);
            }
            return stationBuilders[stationId];
        }

        /**
         * Associates the builder of the ParetoFront given to the station indexed
         *
         * @param stationId the id of the station
         * @param builder   the builder of the ParetoFront associated
         * @throws IndexOutOfBoundsException if the stationId is not valid
         */
        public void setForStation(int stationId, ParetoFront.Builder builder) {
            if (stationId < 0 || stationId >= stationBuilders.length) {
                throw new IndexOutOfBoundsException("Invalid stationId: " + stationId);
            }
            stationBuilders[stationId] = builder;
        }

        /**
         * Returns the builder of the ParetoFront corresponding to the trip indexed
         *
         * @param tripId the id of the trip given
         * @return null if there was no call to the method setForTrip for this trip
         * @throws IndexOutOfBoundsException if the tripId is not valid
         */
        public ParetoFront.Builder forTrip(int tripId) {
            if (tripId < 0 || tripId >= tripBuilders.length) {
                throw new IndexOutOfBoundsException("Invalid tripId: " + tripId);
            }
            return tripBuilders[tripId];
        }

        /**
         * Associates the builder of the ParetoFront given to the trip indexed
         *
         * @param tripId  the id of the station
         * @param builder the builder of the ParetoFront associated
         * @throws IndexOutOfBoundsException if the tripId is not valid
         */
        public void setForTrip(int tripId, ParetoFront.Builder builder) {
            if (tripId < 0 || tripId >= tripBuilders.length) {
                throw new IndexOutOfBoundsException("Invalid tripId: " + tripId);
            }
            tripBuilders[tripId] = builder;
        }

        /**
         * Returns the Profile without the ParetoFront corresponding to the trips
         */
        public Profile build() {
            ParetoFront[] finalFronts = new ParetoFront[stationBuilders.length];
            for (int i = 0; i < stationBuilders.length; i++) {
                finalFronts[i] = (stationBuilders[i] == null)
                        ? ParetoFront.EMPTY
                        : stationBuilders[i].build();
            }
            return new Profile(timeTable, date, arrStationId, List.of(finalFronts));
        }
    }
}
