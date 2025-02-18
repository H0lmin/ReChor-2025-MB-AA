package ch.epfl.rechor.journey;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import static ch.epfl.rechor.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

public record Journey(List<Leg> legs) {

    public sealed interface Leg {
        record IntermediateStop(Stop stop, LocalDateTime arrTime, LocalDateTime depTime) {
            public IntermediateStop {
                requireNonNull(stop, "The stop cannot be null.");
                checkArgument(!depTime.isBefore(arrTime));
            }
        }

        record Transport(Stop depStop, LocalDateTime depTime, Stop arrStop, LocalDateTime arrTime,
                         List<IntermediateStop> intermediateStops, Vehicle vehicle, String route, String destination)
                implements Leg {

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

        record Foot(Stop depStop, LocalDateTime depTime, Stop arrStop, LocalDateTime arrTime) implements Leg {
            public Foot {
                requireNonNull(depStop);
                requireNonNull(arrStop);
                requireNonNull(depTime);
                requireNonNull(arrTime);

                checkArgument(!depTime.isAfter(arrTime));
            }

            public List<IntermediateStop> intermediateStops() {
                return List.of();
            }

            public boolean isTransfer() {
                return depStop.name().equals(arrStop.name());
            }
        }

        Stop depStop();
        LocalDateTime depTime();
        Stop arrStop();
        LocalDateTime arrTime();
        List<IntermediateStop> intermediateStops();

        default Duration duration() {
            return Duration.between(depTime(), arrTime());
        }
    }

    public Journey {
        requireNonNull(legs, "The legs list cannot be null.");
        checkArgument(!legs.isEmpty());

        String lastStepType = null;
        for (Leg step : legs) {
            String actualStepType = step.getClass().getSimpleName();
            if (!actualStepType.equals("IntermediateStop")) {
                if (lastStepType == null) {
                    lastStepType = actualStepType;
                } else {
                    checkArgument(!actualStepType.equals(lastStepType));
                    lastStepType = actualStepType;
                }
            }
        }

        for (int i = 1; i < legs.size(); i++) {
            checkArgument(!legs.get(i).depTime().isBefore(legs.get(i - 1).arrTime()));
            checkArgument(legs.get(i).depStop().equals(legs.get(i - 1).arrStop()));
        }

        legs = List.copyOf(legs);
    }

    public Stop depStop() {
        return legs.getFirst().depStop();
    }

    public Stop arrStop() {
        return legs.getLast().arrStop();
    }

    public LocalDateTime depTime() {
        return legs.getFirst().depTime();
    }

    public LocalDateTime arrTime() {
        return legs.getLast().arrTime();
    }

    public Duration duration() {
        return Duration.between(depTime(), arrTime());
    }
}
