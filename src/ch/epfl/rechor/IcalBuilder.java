package ch.epfl.rechor;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import static ch.epfl.rechor.Preconditions.checkArgument;

/**
 * A builder for creating iCalendar content.
 *
 * @author Amine AMIRA (393410)
 * @author Malak Berrada (379791)
 */
public final class IcalBuilder {
    private static final int MAX_LINE_LENGTH = 75;
    private static final DateTimeFormatter ICAL_DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss");

    private final StringBuilder calendarContent = new StringBuilder();
    private final List<Component> componentsInProgress = new ArrayList<>();

    /**
     * Folds a line to fit iCalendar's 75-character limit.
     * inserting CRLF and a single space at the start of each continued line.
     *
     * @param value the logical line to fold
     * @return the folded line string
     */
    private String foldLine(String value) {
        StringBuilder sb = new StringBuilder();
        int start = 0;
        int length = value.length();
        while (start < length) {
            int end = Math.min(start + MAX_LINE_LENGTH - 1, length);
            sb.append(value, start, end);
            if (end < length) {
                sb.append("\r\n ");
            }
            start = end;
        }
        return sb.toString();
    }

    /**
     * Adds a property with a string value to the calendar.
     */
    public IcalBuilder add(Name name, String value) {
        String line = name.name() + ":" + value;
        calendarContent.append(foldLine(line)).append("\r\n");
        return this;
    }

    /**
     * Adds a property with a LocalDateTime value to the calendar.
     */
    public IcalBuilder add(Name name, LocalDateTime dateTime) {
        return add(name, dateTime.format(ICAL_DATE_TIME_FORMATTER));
    }

    /**
     * Begins a new iCalendar component (e.g., VCALENDAR, VEVENT).
     */
    public IcalBuilder begin(Component component) {
        componentsInProgress.add(component);
        return add(Name.BEGIN, component.name());
    }

    /**
     * Ends the current iCalendar component.
     */
    public IcalBuilder end() {
        checkArgument(!componentsInProgress.isEmpty());
        Component component = componentsInProgress.removeLast();
        return add(Name.END, component.name());
    }

    /**
     * Finalizes and returns the iCalendar content.
     */
    public String build() {
        checkArgument(componentsInProgress.isEmpty());
        return calendarContent.toString();
    }

    /**
     * Represents iCalendar components.
     */
    public enum Component {
        VCALENDAR,
        VEVENT
    }

    /**
     * Represents iCalendar property names.
     */
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
}
