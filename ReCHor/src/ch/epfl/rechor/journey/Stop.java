package ch.epfl.rechor.journey;

import java.util.Objects;

import static ch.epfl.rechor.Preconditions.checkArgument;

public record Stop(String name, String platformName, double longitude, double latitude) {
    public Stop{
        Objects.requireNonNull(name, "The name cannot be null.");

        checkArgument(longitude >= -180.0 && longitude <= 180.0);
        checkArgument(latitude >= -90.0 && latitude <= 90.0);

    }
}
