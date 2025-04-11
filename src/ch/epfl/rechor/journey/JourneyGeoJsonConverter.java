package ch.epfl.rechor.journey;

import ch.epfl.rechor.Json;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class JourneyGeoJsonConverter {

    private JourneyGeoJsonConverter() { }

    private static double round5(double value) {
        return Math.round(value * 1e5) / 1e5;
    }

    public static String toGeoJson(Journey journey) {
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

        Json geoJson = getJson(stops);

        return geoJson.toString();
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

            coordinatePairs.add(new Json.JArray(List.of(
                    new Json.JNumber(lon),
                    new Json.JNumber(lat)
            )));
        }

        Json geoJson = new Json.JObject(Map.of(
                "type", new Json.JString("LineString"),
                "coordinates", new Json.JArray(coordinatePairs)
        ));
        return geoJson;
    }
}
