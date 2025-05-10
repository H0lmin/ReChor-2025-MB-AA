package ch.epfl.rechor.journey;

import ch.epfl.rechor.Json;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import static java.util.Objects.requireNonNull;

public final class JourneyGeoJsonConverter {
    private static final double COORDINATE_SCALE = 1e5;

    private JourneyGeoJsonConverter() {
    }

    /**
     * Rounds a geographic coordinate value to five decimal places.
     *
     * @param value raw coordinate in degrees
     * @return coordinate rounded to five decimal places
     */
    private static double round5(double value) {
        return Math.round(value * COORDINATE_SCALE) / COORDINATE_SCALE;
    }

    /**
     * Converts the provided Journey into a GeoJSON document (as a String) representing a
     * LineString.
     *
     * @param journey the journey to convert.
     * @return a compact JSON string (with no extra whitespace) representing the journey route.
     * @throws NullPointerException if journey is null.
     */
    public static Json toGeoJson(Journey journey) {
        requireNonNull(journey, "journey must not be null");
        List<Stop> stops = new ArrayList<>();
        List<Journey.Leg> legs = journey.legs();

        for (Journey.Leg leg : legs) {
            stops.add(leg.depStop());
            stops.addAll(leg.intermediateStops().stream()
                    .map(Journey.Leg.IntermediateStop::stop)
                    .toList());
            stops.add(leg.arrStop());
        }

        return getJson(stops);
    }

    public static Json getJson(List<Stop> stops) {
        // Build coordinates, skipping consecutive duplicates
        List<Json> coordinates = new ArrayList<>();
        double previousLon = 0, previousLat = 0;
        boolean isFirstPoint = true;

        for (Stop stop : stops) {
            double lon = round5(stop.longitude()), lat = round5(stop.latitude());

            if (!isFirstPoint
                    && Double.compare(lon, previousLon) == 0
                    && Double.compare(lat, previousLat) == 0) {
                continue;
            }

            coordinates.add(new Json.JArray(List.of(
                    new Json.JNumber(lon),
                    new Json.JNumber(lat)
            )));

            previousLon = lon;
            previousLat = lat;
            isFirstPoint = false;
        }

        // Assemble GeoJSON object with rotation order
        LinkedHashMap<String, Json> geoJsonMap = new LinkedHashMap<>();
        geoJsonMap.put("type", new Json.JString("LineString"));
        geoJsonMap.put("coordinates", new Json.JArray(coordinates));

        return new Json.JObject(geoJsonMap);
    }
}