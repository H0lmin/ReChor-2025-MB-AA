package ch.epfl.rechor.journey;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import java.util.List;
import ch.epfl.rechor.journey.Journey.Leg;

public class MyJourneyIcalConverterTest {

    /**
     * Test a simple journey composed of a single Foot leg.
     * This verifies that all required iCalendar fields are present and that the SUMMARY
     * correctly displays the departure and arrival stop names.
     */
    @Test
    public void testSingleLegFootJourneyConversion() {
        Stop stopA = new Stop("StopA", "Platform1", 7.0, 46.0);
        Stop stopB = new Stop("StopB", "Platform2", 7.1, 46.1);
        LocalDateTime depTime = LocalDateTime.of(2025, 2, 18, 8, 0);
        LocalDateTime arrTime = LocalDateTime.of(2025, 2, 18, 8, 30);

        // Create a Foot leg journey.
        Leg.Foot footLeg = new Leg.Foot(stopA, depTime, stopB, arrTime);
        Journey journey = new Journey(List.of(footLeg));

        String ical = JourneyIcalConverter.toIcalendar(journey);

        // Check that the output has the basic iCalendar structure.
        assertTrue(ical.contains("BEGIN:VCALENDAR"), "Should contain VCALENDAR start");
        assertTrue(ical.contains("END:VCALENDAR"), "Should contain VCALENDAR end");
        assertTrue(ical.contains("BEGIN:VEVENT"), "Should contain VEVENT start");

        // Ensure that UID, DTSTAMP, DTSTART, DTEND fields are present.
        assertTrue(ical.contains("UID:"), "UID must be present");
        assertTrue(ical.contains("DTSTAMP:"), "DTSTAMP must be present");
        assertTrue(ical.contains("DTSTART:"), "DTSTART must be present");
        assertTrue(ical.contains("DTEND:"), "DTEND must be present");

        // Check that the SUMMARY field correctly concatenates the stop names.
        assertTrue(ical.contains("SUMMARY:StopA → StopB"), "SUMMARY should be 'StopA → StopB'");

        // Check that the DESCRIPTION field is present (it should include the formatted leg).
        assertTrue(ical.contains("DESCRIPTION:"), "DESCRIPTION field must be present");
    }

    /**
     * Test a journey with multiple legs: first a Transport leg followed by a Foot leg.
     * This test checks that the overall journey summary is built from the first leg's departure
     * and the last leg's arrival stops, and that the description contains one line per leg.
     */
    @Test
    public void testMultiLegJourneyConversion() {
        Stop stopA = new Stop("StopA", "Platform1", 7.0, 46.0);
        Stop stopB = new Stop("StopB", "Platform2", 7.1, 46.1);
        Stop stopC = new Stop("StopC", "Platform3", 7.2, 46.2);

        LocalDateTime time1 = LocalDateTime.of(2025, 2, 18, 8, 0);
        LocalDateTime time2 = LocalDateTime.of(2025, 2, 18, 8, 30);
        LocalDateTime time3 = LocalDateTime.of(2025, 2, 18, 9, 0);

        // Create a Transport leg from StopA to StopB.
        Leg.Transport transportLeg = new Leg.Transport(
                stopA, time1, stopB, time2,
                List.of(), // no intermediate stops for simplicity
                Vehicle.BUS, "Route1", "Destination1"
        );

        // Create a Foot leg from StopB to StopC.
        Leg.Foot footLeg = new Leg.Foot(
                stopB, time2, stopC, time3
        );

        Journey journey = new Journey(List.of(transportLeg, footLeg));

        String ical = JourneyIcalConverter.toIcalendar(journey);

        // Verify basic iCalendar structure.
        assertTrue(ical.contains("BEGIN:VCALENDAR"), "Should contain VCALENDAR start");
        assertTrue(ical.contains("END:VCALENDAR"), "Should contain VCALENDAR end");
        assertTrue(ical.contains("BEGIN:VEVENT"), "Should contain VEVENT start");

        // The overall journey should start at StopA and end at StopC.
        assertTrue(ical.contains("SUMMARY:StopA → StopC"), "SUMMARY should be 'StopA → StopC'");

        // Verify that the DESCRIPTION contains two lines (one per leg).
        int descIndex = ical.indexOf("DESCRIPTION:");
        assertTrue(descIndex >= 0, "DESCRIPTION field must exist");
        String descriptionPart = ical.substring(descIndex);
        // Here we assume that the formatter inserts a newline between each leg.
        int newlineCount = descriptionPart.split("\n").length - 1;
        assertTrue(newlineCount >= 1, "DESCRIPTION should have at least one newline for multiple legs");
    }

    /**
     * Test an edge case where the journey represents a transfer at the same stop.
     * The SUMMARY should show the same stop for both departure and arrival.
     */
    @Test
    public void testSameStopTransferJourney() {
        Stop stopA = new Stop("StopA", "Platform1", 7.0, 46.0);
        LocalDateTime time1 = LocalDateTime.of(2025, 2, 18, 10, 0);
        LocalDateTime time2 = LocalDateTime.of(2025, 2, 18, 10, 5);

        // Create a Foot leg where the departure and arrival stops are the same.
        Leg.Foot transferLeg = new Leg.Foot(stopA, time1, stopA, time2);
        Journey journey = new Journey(List.of(transferLeg));

        String ical = JourneyIcalConverter.toIcalendar(journey);

        // SUMMARY should reflect a transfer at the same stop.
        assertTrue(ical.contains("SUMMARY:StopA → StopA"), "SUMMARY should be 'StopA → StopA'");
    }

    /**
     * Test that the Journey record is immutable.
     * Attempts to modify the list returned by journey.legs() should throw an exception.
     */
    @Test
    public void testImmutabilityOfJourneyLegs() {
        Stop stopA = new Stop("StopA", "Platform1", 7.0, 46.0);
        Stop stopB = new Stop("StopB", "Platform2", 7.1, 46.1);
        LocalDateTime depTime = LocalDateTime.of(2025, 2, 18, 8, 0);
        LocalDateTime arrTime = LocalDateTime.of(2025, 2, 18, 8, 30);
        Leg.Foot footLeg = new Leg.Foot(stopA, depTime, stopB, arrTime);
        List<Leg> legsList = List.of(footLeg);
        Journey journey = new Journey(legsList);

        // The list returned by journey.legs() should be immutable.
        assertThrows(UnsupportedOperationException.class, () -> {
            journey.legs().add(footLeg);
        }, "The legs list should be immutable");
    }
}
