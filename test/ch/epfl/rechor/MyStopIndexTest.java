package ch.epfl.rechor;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

class MyStopIndexTest {

    /**
     * Creates a StopIndex instance from a list of main stop names and a map of
     * alternative names to main stop names.
     * The main stop list includes:
     *  - "Lausanne"
     *  - "Mézières FR, village"
     *  - "Mézières VD, village"
     *  - "Mézery-près-Donneloye, village"
     *  - "Charleville-Mézières"
     *
     * The alternative names map provides:
     *  - "Losanna" → "Lausanne"
     *  - "Mezieres" → "Mézières FR, village"  (for accent/case flexibility)
     */
    private StopIndex createSampleIndex() {
        List<String> mainStops = List.of(
                "Lausanne",
                "Mézières FR, village",
                "Mézières VD, village",
                "Mézery-près-Donneloye, village",
                "Charleville-Mézières"
        );
        Map<String, String> altNames = new HashMap<>();
        altNames.put("Losanna", "Lausanne");
        altNames.put("Mezieres", "Mézières FR, village");
        return new StopIndex(mainStops, altNames);
    }

    /**
     * Test that passing a null query throws a NullPointerException.
     */
    @Test
    void testNullQuery() {
        StopIndex index = createSampleIndex();
        assertThrows(NullPointerException.class, () -> {
            index.stopsMatching(null, 10);
        });
    }

    /**
     * Test that a query which does not match any stops returns an empty list.
     */
    @Test
    void testNoMatchQuery() {
        StopIndex index = createSampleIndex();
        List<String> results = index.stopsMatching("xyz", 10);
        assertTrue(results.isEmpty(), "Unknown query should return an empty list");
    }

    /**
     * Test that a query exactly matching a main stop (or a part of it) returns that stop.
     */
    @Test
    void testDirectMatch() {
        StopIndex index = createSampleIndex();
        // Query "Lausanne" should match directly.
        List<String> results = index.stopsMatching("Lausanne", 10);
        assertFalse(results.isEmpty());
        assertTrue(results.contains("Lausanne"));
    }

    /**
     * Test that an alternative name query returns the corresponding main name.
     * For example, the query "Losanna" (all lowercase) should return "Lausanne".
     */
    @Test
    void testAlternativeName() {
        StopIndex index = createSampleIndex();
        // "Losanna" is an alternative for "Lausanne"
        List<String> results = index.stopsMatching("losanna", 10);
        assertFalse(results.isEmpty());
        assertEquals("Lausanne", results.get(0));
    }

    /**
     * Test that case-insensitivity works when the query contains no uppercase letter.
     */
    @Test
    void testCaseInsensitiveMatching() {
        StopIndex index = createSampleIndex();
        // Query in all lowercase should match even if the stop name contains uppercase accented letters.
        List<String> results = index.stopsMatching("mezieres", 10);
        // Since "Mezieres" is transformed (via regex with accent expansions) it should match "Mézières FR, village".
        assertFalse(results.isEmpty());
        assertTrue(results.contains("Mézières FR, village"));
    }

    /**
     * Test that accent handling works: a query using a plain vowel should match a stop name
     * containing the accented version.
     */
    @Test
    void testAccentHandling() {
        StopIndex index = createSampleIndex();
        // Query "mezieres" (without accent) should match "Mézières FR, village"
        List<String> results = index.stopsMatching("mezieres", 10);
        assertFalse(results.isEmpty());
        assertTrue(results.contains("Mézières FR, village"));
    }

    /**
     * Test scoring and sorting: using the provided guidelines, for the query "mez vil",
     * the scores for stops should be (for example) 120 for "Mézières FR, village" and "Mézières VD, village",
     * 80 for "Mézery-près-Donneloye, village", and 75 for "Charleville-Mézières".
     * The sorted order should then have the two Mézières stops first.
     */
    @Test
    void testScoringAndSorting() {
        StopIndex index = createSampleIndex();
        // The query "mez vil" should match stops that contain both substrings "mez" and "vil".
        List<String> results = index.stopsMatching("mez vil", 10);
        // Expected ordering (highest relevance first):
        // "Mézières FR, village" and "Mézières VD, village" should appear before
        // "Mézery-près-Donneloye, village" and "Charleville-Mézières".
        assertFalse(results.isEmpty());
        // We check that the first two results are one of the Mézières stops.
        String first = results.get(0);
        String second = results.get(1);
        assertTrue(first.contains("Mézières FR") || first.contains("Mézières VD"),
                "First result should be one of the Mézières stops");
        assertTrue(second.contains("Mézières FR") || second.contains("Mézières VD"),
                "Second result should be one of the Mézières stops");
    }

    /**
     * Test duplicate removal in a multi-word query:
     * If a stop appears both as an alternative name and as the main name match,
     * the returned result list should not contain duplicates.
     */
    @Test
    void testNoDuplicatesInResults() {
        StopIndex index = createSampleIndex();
        // Using a query that could match both a main name and an alternative name:
        List<String> results = index.stopsMatching("mezieres", 10);
        // "Mézières FR, village" should appear only once.
        long count = results.stream().filter(s -> s.equals("Mézières FR, village")).count();
        assertEquals(1, count, "The main stop should appear only once in the results");
    }
}
