package ch.epfl.rechor.journey;

import ch.epfl.rechor.Json;

import java.util.*;

public final class JourneyGeoJsonConverter {

    private JourneyGeoJsonConverter() { }

    private static double round5(double value) {
        return Math.round(value * 1e5) / 1e5;
    }

    /**
     * Converts the provided Journey into a GeoJSON document (as a String) representing a LineString.
     *
     * @param journey the journey to convert.
     * @return a compact JSON string (with no extra whitespace) representing the journey route.
     * @throws NullPointerException if journey is null.
     */
    public static Json toGeoJson(Journey journey) {

        List<Stop> stops = new ArrayList<>();
        List<Journey.Leg> legs = journey.legs();

        for (int i = 0; i < legs.size(); i++) {
            Journey.Leg leg = legs.get(i);
            if (i == 0) {
                stops.add(leg.depStop());
            }
            for (Journey.Leg.IntermediateStop inter : leg.intermediateStops()) {
                stops.add(inter.stop());
            }
            stops.add(leg.arrStop());
        }

        // Build a list of coordinate pairs, each as a Json.JArray containing the rounded [longitude, latitude].

        return getJson(stops);
    }

    private static Json getJson(List<Stop> stops) {
        List<Json> coordinatePairs = new ArrayList<>();
        boolean firstPoint = true;
        double lastLon = Double.NaN, lastLat = Double.NaN;

        for (Stop stop : stops) {
            double lon = round5(stop.longitude());
            double lat = round5(stop.latitude());

            if (!firstPoint && Double.compare(lon, lastLon) == 0 && Double.compare(lat, lastLat) == 0) {
                continue;
            }
            firstPoint = false;
            lastLon = lon;
            lastLat = lat;

            // Create a coordinate pair [lon, lat] as a Json array.
            coordinatePairs.add(new Json.JArray(List.of(
                    new Json.JNumber(lon),
                    new Json.JNumber(lat)
            )));
        }

        // Use a LinkedHashMap to preserve the insertion order.
        LinkedHashMap<String, Json> orderedMap = new LinkedHashMap<>();
        orderedMap.put("type", new Json.JString("LineString"));
        orderedMap.put("coordinates", new Json.JArray(coordinatePairs));

        return new Json.JObject(orderedMap);
    }
}
