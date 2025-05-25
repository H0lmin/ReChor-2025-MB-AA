package ch.epfl.rechor.gui;

import ch.epfl.rechor.journey.Vehicle;
import javafx.scene.image.Image;

import java.util.EnumMap;
import java.util.Map;

/**
 * Utility class for loading and caching icon images for {@link Vehicle} types.
 *
 * @author Amine AMIRA (393410)
 * @author Malak Berrada (379791)
 */
public final class VehicleIcons {
    private static final Map<Vehicle, Image> CACHE = new EnumMap<>(Vehicle.class);

    private VehicleIcons() {}

    /**
     * Returns the icon image associated with the given {@link Vehicle}.
     *
     * @param vehicle the vehicle type for which to retrieve an icon
     * @return the {@link Image} corresponding to {@code vehicle}
     * @throws NullPointerException if {@code vehicle} is {@code null}
     */
    public static Image iconFor(Vehicle vehicle) {
        return CACHE.computeIfAbsent(vehicle, v -> new Image(v.name() + ".png"));
    }
}
