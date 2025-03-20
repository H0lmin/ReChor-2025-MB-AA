package ch.epfl.rechor.journey;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class JourneyExtractor {
    private JourneyExtractor() {
    }

    public static List<Journey> journeys(Profile profile, int depStationId) {
        List<Journey> journeys = new ArrayList<>();
        profile.forStation(depStationId).forEach(criteria ->
                journeys.add(extractJourney(profile, depStationId, criteria))
        );
        journeys.sort(Comparator.comparing(Journey::depTime).thenComparing(Journey::arrTime));
        return journeys;
    }
}