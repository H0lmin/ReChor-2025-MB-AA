package ch.epfl.rechor.journey;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import static ch.epfl.rechor.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

/**
 * Represents a journey consisting of multiple legs (segments of the journey).
 *
 * @param legs The list of legs that make up the journey.
 */
public record Journey(List<Leg> legs) {

    /**
     * Represents a leg in the journey. A leg can either be a transport (e.g., a bus or train ride),
     * a foot journey (walking), or an intermediate stop.
     */
    public sealed interface Leg {

        /**
         * Represents an intermediate stop during a transport or foot journey.
         */
        record IntermediateStop(Stop stop, LocalDateTime arrTime, LocalDateTime depTime) implements Leg{

            /**
             * Creates an IntermediateStop.
             *
             * @param stop The stop where the intermediate stop occurs.
             * @param arrTime The arrival time at the intermediate stop.
             * @param depTime The departure time from the intermediate stop.
             * @throws NullPointerException if stop is null.
             * @throws IllegalArgumentException if depTime is before arrTime.
             */
            public IntermediateStop {
                requireNonNull(stop, "The stop cannot be null.");
                checkArgument(!depTime.isBefore(arrTime));
            }

            @Override
            public Stop depStop() {
                return null;
            }

            @Override
            public Stop arrStop() {
                return null;
            }

            @Override
            public List<IntermediateStop> intermediateStops() {
                return List.of();
            }
        }

        /**
         * Represents a transport leg (e.g., bus, train, etc.) of the journey.
         * @param depStop The departure stop.
         * @param depTime The departure time.
         * @param arrStop The arrival stop.
         * @param arrTime The arrival time.
         * @param intermediateStops The list of intermediate stops.
         * @param vehicle The vehicle used for transport.
         * @param route The route taken.
         * @param destination The final destination.
         */
        record Transport(Stop depStop, LocalDateTime depTime, Stop arrStop, LocalDateTime arrTime,
                         List<IntermediateStop> intermediateStops, Vehicle vehicle, String route, String destination)
                implements Leg {

            /**
             * Creates a Transport leg.
             *
             * @throws NullPointerException if any argument is null.
             * @throws IllegalArgumentException if depTime is after arrTime.
             */
            public Transport {
                requireNonNull(depStop);
                requireNonNull(arrStop);
                requireNonNull(depTime);
                requireNonNull(arrTime);
                requireNonNull(vehicle);
                requireNonNull(intermediateStops);
                requireNonNull(route);
                requireNonNull(destination);

                checkArgument(!depTime.isAfter(arrTime));
                intermediateStops = List.copyOf(intermediateStops);
            }
        }

        /**
         * Represents a foot leg (walking) of the journey.
         *
         * @param depStop The departure stop.
         * @param depTime The departure time.
         * @param arrStop The arrival stop.
         * @param arrTime The arrival time.
         */
        record Foot(Stop depStop, LocalDateTime depTime, Stop arrStop, LocalDateTime arrTime) implements Leg {

            /**
             * Creates a Foot leg.
             *
             * @throws NullPointerException if any argument is null.
             * @throws IllegalArgumentException if depTime is after arrTime.
             */
            public Foot {
                requireNonNull(depStop);
                requireNonNull(arrStop);
                requireNonNull(depTime);
                requireNonNull(arrTime);

                checkArgument(!depTime.isAfter(arrTime));
            }

            /**
             * Returns an empty list of intermediate stops as foot legs have no intermediate stops.
             *
             * @return An empty list of intermediate stops.
             */
            public List<IntermediateStop> intermediateStops() {
                return List.of();
            }

            /**
             * Determines whether this foot journey is a transfer between the same stop.
             *
             * @return True if the departure and arrival stops are the same, indicating a transfer.
             */
            public boolean isTransfer() {
                return depStop.name().equals(arrStop.name());
            }
        }

        /**
         * @return The departure stop for the leg.
         */
        Stop depStop();

        /**
         * @return The departure time for the leg.
         */
        LocalDateTime depTime();

        /**
         * @return The arrival stop for the leg.
         */
        Stop arrStop();

        /**
         * @return The arrival time for the leg.
         */
        LocalDateTime arrTime();

        /**
         * @return The list of intermediate stops for the leg.
         */
        List<IntermediateStop> intermediateStops();

        /**
         * Calculates the duration of the leg.
         *
         * @return The duration of the leg.
         */
        default Duration duration() {
            return Duration.between(depTime(), arrTime());
        }
    }

    /**
     * Creates a Journey.
     *
     * @param legs The list of legs that constitute the journey.
     * @throws NullPointerException if the legs list is null.
     * @throws IllegalArgumentException if the legs list is empty or if there are invalid leg transitions.
     */
    public Journey {
        requireNonNull(legs, "The legs list cannot be null.");
        checkArgument(!legs.isEmpty());

        String lastStepType = null;
        for (Leg step : legs) {
            String actualStepType = step.getClass().getSimpleName();
            if (!actualStepType.equals("IntermediateStop")) {
                if (lastStepType != null) {
                    checkArgument(!actualStepType.equals(lastStepType));
                }
                lastStepType = actualStepType;
            }
        }

        for (int i = 1; i < legs.size(); i++) {
            checkArgument(!legs.get(i).depTime().isBefore(legs.get(i - 1).arrTime()));
            checkArgument(legs.get(i).depStop().equals(legs.get(i - 1).arrStop()));
        }

        legs = List.copyOf(legs);
    }

    /**
     * @return The departure stop of the journey.
     */
    public Stop depStop() {
        return legs.getFirst().depStop();
    }

    /**
     * @return The arrival stop of the journey.
     */
    public Stop arrStop() {
        return legs.getLast().arrStop();
    }

    /**
     * @return The departure time of the journey.
     */
    public LocalDateTime depTime() {
        return legs.getFirst().depTime();
    }

    /**
     * @return The arrival time of the journey.
     */
    public LocalDateTime arrTime() {
        return legs.getLast().arrTime();
    }

    /**
     * @return The total duration of the journey.
     */
    public Duration duration() {
        return Duration.between(depTime(), arrTime());
    }
}
