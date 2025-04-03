package ch.epfl.rechor.journey;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import static ch.epfl.rechor.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

/**
 * Represents a complete journey, which is a sequence of alternating legs (walking and transport
 * legs) that a passenger can take to travel from a departure stop to an arrival stop.
 *
 * @param legs the list of journey legs that compose the journey.
 * @author Amine AMIRA (393410)
 * @author Malak Berrada (379791)
 */
public record Journey(List<Leg> legs) {
    /**
     * Constructs a Journey with the specified list of legs.
     *
     * @param legs the list of legs composing the journey.
     * @throws IllegalArgumentException if list is empty, there is two consecutive legs of the same
     *                                  type (except if they are intermediate stops, the departure
     *                                  time of each leg (except the first) is not before the
     *                                  arrival time of the previous leg or the departure stop of
     *                                  each leg (except the first) equals the arrival stop of the
     *                                  previous leg.
     */
    public Journey {
        checkArgument(!legs.isEmpty());

        for (int i = 1; i < legs.size(); i++) {
            String prevLegType = legs.get(i - 1).getClass().getSimpleName();
            String currentLegType = legs.get(i).getClass().getSimpleName();

            if (!"IntermediateStop".equals(currentLegType)) {
                checkArgument(!currentLegType.equals(prevLegType));
            }

            checkArgument(!legs.get(i).depTime().isBefore(legs.get(i - 1).arrTime()));
            checkArgument(legs.get(i).depStop().equals(legs.get(i - 1).arrStop()));
        }

        legs = List.copyOf(legs);
    }

    /**
     * Returns the departure stop of the journey.
     *
     * @return the departure stop of the journey.
     */
    public Stop depStop() {
        return legs.getFirst().depStop();
    }

    /**
     * Returns the arrival stop of the journey.
     *
     * @return the stop at which the journey ends.
     */
    public Stop arrStop() {
        return legs.getLast().arrStop();
    }

    /**
     * Returns the departure time of the journey.
     *
     * @return the time when the journey begins.
     */
    public LocalDateTime depTime() {
        return legs.getFirst().depTime();
    }

    /**
     * Returns the arrival time of the journey.
     *
     * @return the time when the journey ends.
     */
    public LocalDateTime arrTime() {
        return legs.getLast().arrTime();
    }

    /**
     * Returns the total duration of the journey.
     *
     * @return the duration between the departure time and the arrival time.
     */
    public Duration duration() {
        return Duration.between(depTime(), arrTime());
    }

    /**
     * Represents a leg of a journey.
     * <p>
     * A leg is a segment of the journey with its own departure and arrival stops and times. Legs
     * can be either a transport leg (a ride in a vehicle) or a foot leg (walking).
     * </p>
     */
    public sealed interface Leg {
        /**
         * Returns the departure stop of this leg.
         *
         * @return the departure stop.
         */
        Stop depStop();

        /**
         * Returns the departure time of this leg.
         *
         * @return the departure time.
         */
        LocalDateTime depTime();

        /**
         * Returns the arrival stop of this leg.
         *
         * @return the arrival stop.
         */
        Stop arrStop();

        /**
         * Returns the arrival time of this leg.
         *
         * @return the arrival time.
         */
        LocalDateTime arrTime();

        /**
         * Returns the list of intermediate stops (if any) for this leg.
         * <p>
         * For a transport leg, these are the stops between the departure and arrival stops. For a
         * foot leg, the list is always empty.
         * </p>
         *
         * @return an unmodifiable list of intermediate stops.
         */
        List<IntermediateStop> intermediateStops();

        /**
         * Returns the duration of this leg.
         *
         * @return the duration between the departure time and the arrival time.
         */
        default Duration duration() {
            return Duration.between(depTime(), arrTime());
        }

        /**
         * Represents an intermediate stop during a transport leg
         * <p>
         * The departure time must not be before the arrival time.
         * </p>
         *
         * @param stop    the intermediate stop.
         * @param arrTime the arrival time at the intermediate stop.
         * @param depTime the departure time from the intermediate stop.
         */
        record IntermediateStop(Stop stop, LocalDateTime arrTime, LocalDateTime depTime) {
            /**
             * Constructs an {@code IntermediateStop} instance.
             *
             * @throws NullPointerException     if {@code stop} is {@code null}.
             * @throws IllegalArgumentException if the departure time from the stop isn't before the
             *                                  arrival time at the stop
             */
            public IntermediateStop {
                requireNonNull(stop);

                checkArgument(!depTime.isBefore(arrTime));
            }
        }

        /**
         * Represents a transport leg, i.e. a ride in a vehicle.
         *
         * @param depStop           the departure stop.
         * @param depTime           the departure time.
         * @param arrStop           the arrival stop.
         * @param arrTime           the arrival time.
         * @param intermediateStops the list of intermediate stops (if any).
         * @param vehicle           the vehicle used for this leg.
         * @param route             the route identifier.
         * @param destination       the final destination of the transport leg.
         */
        record Transport(Stop depStop, LocalDateTime depTime,
                         Stop arrStop, LocalDateTime arrTime,
                         List<IntermediateStop> intermediateStops,
                         Vehicle vehicle, String route,
                         String destination)
                implements Leg {
            /**
             * Constructs a {@code Transport} instance.
             * <p>
             * Assigns an unmodifiable list of intermediate stops (if any).
             * </p>
             *
             * @throws NullPointerException     if any of the arguments are null.
             * @throws IllegalArgumentException if the arrival time at the stop isn't before the
             *                                  departure time from the stop
             */
            public Transport {
                requireNonNull(depStop);
                requireNonNull(depTime);
                requireNonNull(arrStop);
                requireNonNull(arrTime);
                requireNonNull(vehicle);
                requireNonNull(route);
                requireNonNull(destination);

                checkArgument(!arrTime.isBefore(depTime));
                intermediateStops = List.copyOf(intermediateStops);
            }
        }

        /**
         * Represents a foot leg.
         *
         * @param depStop the departure stop.
         * @param depTime the departure time.
         * @param arrStop the arrival stop.
         * @param arrTime the arrival time.
         */
        record Foot(Stop depStop, LocalDateTime depTime,
                    Stop arrStop, LocalDateTime arrTime)
                implements Leg {
            /**
             * Constructs a {@code Foot} instance.
             *
             * @throws NullPointerException     if any of the arguments are null.
             * @throws IllegalArgumentException if the arrival time at the stop isn't before the
             *                                  departure time from the stop
             */
            public Foot {
                requireNonNull(depStop);
                requireNonNull(depTime);
                requireNonNull(arrStop);
                requireNonNull(arrTime);

                checkArgument(!arrTime.isBefore(depTime));

            }

            /**
             * Returns an empty list as foot legs do not have intermediate stops.
             *
             * @return an empty list.
             */
            public List<IntermediateStop> intermediateStops() {
                return List.of();
            }

            /**
             * Determines if this foot leg represents a transfer, i.e. a walk within the same
             * station.
             *
             * @return {@code true} if the departure and arrival stops have the same name;
             * {@code false} otherwise.
             */
            public boolean isTransfer() {
                return depStop.name().equals(arrStop.name());
            }
        }
    }
}