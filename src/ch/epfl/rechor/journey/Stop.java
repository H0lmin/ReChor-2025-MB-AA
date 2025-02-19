package ch.epfl.rechor.journey;

import java.util.Objects;

import static ch.epfl.rechor.Preconditions.checkArgument;

/**
 * Creates a Stop.
 *
 * @param name The name of the stop.
 * @param platformName The name of the platform at the stop, if applicable.
 * @param longitude The longitude of the stop, between -180.0 and 180.0.
 * @param latitude The latitude of the stop, between -90.0 and 90.0.
 */
public record Stop(String name, String platformName, double longitude, double latitude) {

    /**
     *
     * Creates a Stop
     * @throws NullPointerException if the name is null.
     * @throws IllegalArgumentException if longitude or latitude are out of bounds.
     */
    public Stop{
        Objects.requireNonNull(name, "The name cannot be null.");

        checkArgument(longitude >= -180.0 && longitude <= 180.0);
        checkArgument(latitude >= -90.0 && latitude <= 90.0);

    }
}
