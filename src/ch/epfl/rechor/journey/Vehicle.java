package ch.epfl.rechor.journey;

import java.util.List;

/**
 * Enum representing different types of vehicles used in a journey.
 * The available vehicle types are: TRAM, METRO, TRAIN, BUS, FERRY, AERIAL_LIFT, and FUNICULAR.
 * This enum also provides a list of all vehicle types available.
 */
public enum Vehicle {
    TRAM,
    METRO,
    TRAIN,
    BUS,
    FERRY,
    AERIAL_LIFT,
    FUNICULAR;

    /**
     * A list containing all available vehicle types.
     */
    public static final List<Vehicle> ALL = List.of(values());
}
