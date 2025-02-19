package ch.epfl.rechor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static ch.epfl.rechor.Preconditions.checkArgument;

public final class IcalBuilder {
    public enum Component {
        VCALENDAR,
        VEVENT
    }

    public enum Name {
        BEGIN,
        END,
        PRODID,
        VERSION,
        UID,
        DTSTAMP,
        DTSTART,
        DTEND,
        SUMMARY,
        DESCRIPTION
    }

    private final List<Component> componentsInProgress = new ArrayList<>();

    private final StringBuilder calendarContent = new StringBuilder();

    /**
     * Adds a name-value pair to the iCalendar event, folding lines so that
     * no line exceeds 75 characters.
     *
     * @param name  The name of the calendar attribute.
     * @param value The value of the calendar attribute.
     * @return The current IcalBuilder instance for method chaining.
     */
    public IcalBuilder add(Name name, String value) {

        checkArgument(value != null);

        String fullLine = name.name() + ":" + value;

        String foldedLine = foldLine(fullLine, 75);

        calendarContent.append(foldedLine).append("\n");
        return this;
    }

    /**
     * Folds a line so that no segment exceeds 'maxLength' characters.
     * If the line is too long, insert a newline + one space to continue.
     *
     * @param line       The full line to fold.
     * @param maxLength  The maximum number of characters allowed per line.
     * @return A single string with embedded newlines for folding.
     */
    private String foldLine(String line, int maxLength) {
        StringBuilder sb = new StringBuilder();
        int start = 0;
        int length = line.length();

        while (start < length) {
            // End index for this segment
            int end = Math.min(start + maxLength, length);

            // Extract the segment
            sb.append(line, start, end);

            // If we haven't reached the end, fold: insert newline + space
            if (end < length) {
                sb.append("\n ");
            }

            // Move to the next segment
            start = end;
        }
        return sb.toString();
    }

    /**
     * Adds a name-datetime pair to the iCalendar event.
     *
     * @param name     The name of the calendar attribute.
     * @param dateTime The date-time value to add.
     * @return The current IcalBuilder instance for method chaining.
     */
    public IcalBuilder add(Name name, LocalDateTime dateTime) {
        String dateTimeFormatted = dateTime.format(java.time.format.DateTimeFormatter.ISO_DATE_TIME);
        return add(name, dateTimeFormatted);
    }

    /**
     * Begins a new component (VCALENDAR or VEVENT).
     *
     * @param component The component to begin (VCALENDAR or VEVENT).
     * @return The current IcalBuilder instance for method chaining.
     */
    public IcalBuilder begin(Component component) {
        componentsInProgress.add(component);
        calendarContent.append("BEGIN: ").append(component.name()).append("\n");
        return this;
    }

    /**
     * Ends the last begun component.
     *
     * @return The current IcalBuilder instance for method chaining.
     * @throws IllegalArgumentException If no component was started before.
     */
    public IcalBuilder end() {
        checkArgument(!componentsInProgress.isEmpty());
        Component component = componentsInProgress.removeLast();
        calendarContent.append("END: ").append(component.name()).append("\n");
        return this;
    }

    /**
     * Builds the final iCalendar string.
     *
     * @return The iCalendar string.
     */
    public String build() {
        checkArgument(componentsInProgress.isEmpty());
        return calendarContent.toString();
    }

}
