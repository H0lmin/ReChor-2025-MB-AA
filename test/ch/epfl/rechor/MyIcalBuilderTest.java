package ch.epfl.rechor;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

public class MyIcalBuilderTest {

    /**
     * Test 1: Normal iCalendar construction.
     * <p>
     * Purpose: To verify that the IcalBuilder can correctly build a simple iCalendar event.
     * We begin a VCALENDAR, add VERSION and PRODID, then begin a VEVENT, add a UID and DTSTAMP,
     * and finally close the VEVENT and VCALENDAR. The resulting string is compared to the expected output.
     */
    @Test
    public void testSimpleIcalBuild() {
        IcalBuilder builder = new IcalBuilder();
        builder.begin(IcalBuilder.Component.VCALENDAR)
                .add(IcalBuilder.Name.VERSION, "2.0")
                .add(IcalBuilder.Name.PRODID, "TestProdID")
                .begin(IcalBuilder.Component.VEVENT)
                .add(IcalBuilder.Name.UID, "test-uid")
                .add(IcalBuilder.Name.DTSTAMP, "20250216T182750")
                .end()
                .end();
        String output = builder.build();

        // Expected string:
        String expected = ""
                + "BEGIN:VCALENDAR\n"
                + "VERSION:2.0\n"
                + "PRODID:TestProdID\n"
                + "BEGIN:VEVENT\n"
                + "UID:test-uid\n"
                + "DTSTAMP:20250216T182750\n"
                + "END:VEVENT\n"
                + "END:VCALENDAR\n";

        Assertions.assertEquals(expected, output, "The built iCalendar string does not match the expected " +
                "output.");
    }

    /**
     * Test 2: Line folding.
     * <p>
     * Purpose: To ensure that when a line exceeds 75 characters, it is properly folded.
     * We add a SUMMARY with a long value (80 characters) and check that the output contains a newline
     * followed by a space (which indicates that folding was applied).
     */
    @Test
    public void testLineFolding() {
        IcalBuilder builder = new IcalBuilder();
        // Create a string of 80 characters (which exceeds the 75-character limit).
        String longValue = "A".repeat(140);
        builder.add(IcalBuilder.Name.SUMMARY, longValue);
        String output = builder.build();

        // Check that the folded line contains a newline followed by a space.
        Assertions.assertTrue(output.contains("\n "), "The line was not properly folded when exceeding " +
                "75 characters.");
    }

    /**
     * Test 3: Adding a null value.
     * <p>
     * Purpose: To verify that adding a null value via add(Name, String) throws an IllegalArgumentException.
     */
    @Test
    public void testAddNullValueThrowsException() {
        IcalBuilder builder = new IcalBuilder();
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            builder.add(IcalBuilder.Name.SUMMARY, (String) null);
        }, "Expected add() to throw an exception when the value is null.");
    }

    /**
     * Test 4: Ending a component without beginning one.
     * <p>
     * Purpose: To ensure that calling end() when no component is open throws an exception.
     */
    @Test
    public void testEndWithoutBeginThrowsException() {
        IcalBuilder builder = new IcalBuilder();
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            builder.end();
        }, "Expected end() to throw an exception when no component has been begun.");
    }

    /**
     * Test 5: Building with an open component.
     * <p>
     * Purpose: To verify that calling build() when one or more components are still open (not ended)
     * throws an exception.
     */
    @Test
    public void testBuildWithOpenComponentThrowsException() {
        IcalBuilder builder = new IcalBuilder();
        builder.begin(IcalBuilder.Component.VCALENDAR);
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            builder.build();
        }, "Expected build() to throw an exception when components are still open.");
    }

    /**
     * Test the add(LocalDateTime) method.
     *
     * Purpose:
     * - To ensure that the add(Name, LocalDateTime) method correctly formats the LocalDateTime value using ISO_DATE_TIME.
     * - A known LocalDateTime is used and its string representation is checked in the built output.
     */
    @Test
    public void testAddLocalDateTime() {
        IcalBuilder builder = new IcalBuilder();
        LocalDateTime dt = LocalDateTime.of(2023, 3, 30, 12, 34, 56);
        builder.add(IcalBuilder.Name.DTSTAMP, dt);
        String result = builder.build();

        // ISO_DATE_TIME should produce "2023-03-30T12:34:56" (plus fractional seconds if any, but ISO standard without fraction defaults to none).
        Assertions.assertTrue(result.contains("2023-03-30T12:34:56"), "The date-time was not formatted correctly.");
    }

    /**
     * Test proper component ordering.
     *
     * Purpose:
     * - To verify that the begin() and end() methods manage the component stack correctly.
     * - The test begins two components and then closes them in the reverse order, verifying that the built output
     *   reflects the proper nesting.
     */
    @Test
    public void testComponentOrdering() {
        IcalBuilder builder = new IcalBuilder();
        builder.begin(IcalBuilder.Component.VCALENDAR)
                .begin(IcalBuilder.Component.VEVENT)
                .add(IcalBuilder.Name.UID, "uid123")
                .end()  // Should close VEVENT.
                .end(); // Should close VCALENDAR.
        String result = builder.build();

        String expected = ""
                + "BEGIN:VCALENDAR\n"
                + "BEGIN:VEVENT\n"
                + "UID:uid123\n"
                + "END:VEVENT\n"
                + "END:VCALENDAR\n";

        Assertions.assertEquals(expected, result, "The component ordering in the built string is incorrect.");
    }
}
