package ch.epfl.rechor.gui;

import ch.epfl.rechor.journey.Vehicle;
import javafx.scene.image.Image;

import java.util.EnumMap;
import java.util.Map;

public final class VehicleIcons {
    private static final Map<Vehicle, Image> CACHE = new EnumMap<>(Vehicle.class);

    private VehicleIcons() {}

    public static Image iconFor(Vehicle vehicle) {
        return CACHE.computeIfAbsent(vehicle, v -> new Image(v.name() + ".png"));
    }
}
