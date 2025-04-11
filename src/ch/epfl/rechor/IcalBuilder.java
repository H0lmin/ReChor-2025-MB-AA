package ch.epfl.rechor;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import static ch.epfl.rechor.Preconditions.checkArgument;

/**
 * A builder for creating iCalendar (.ics) content.
 *
 * @author Amine AMIRA (393410)
 * @author Malak Berrada (379791)
 */
public final class IcalBuilder {
    private final StringBuilder calendarContent = new StringBuilder();
    private final List<Component> componentsInProgress = new ArrayList<>();

    /**
     * Folds a line to fit iCalendar's 75-character limit.
     */
    private String foldLine(String value) {
        StringBuilder sb = new StringBuilder();
        int start = 0;
        int length = value.length();

        while (start < length) {
            int end = Math.min(start + 74, length);
            sb.append(value, start, end);
            if (end < length) sb.append("\r\n ");
            start = end;
        }
        return sb.toString();
    }

    /**
     * Formats a LocalDateTime to iCalendar's date-time format.
     */
    private String formatIcalTime(LocalDateTime dateTime) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss");
        return dateTime.format(fmt);
    }

    /**
     * Adds a property with a string value to the calendar.
     */
    public IcalBuilder add(Name name, String value) {
        calendarContent.append(foldLine(name.name() + ":" + value)).append("\r\n");
        return this;
    }

    /**
     * Adds a property with a LocalDateTime value to the calendar.
     */
    public IcalBuilder add(Name name, LocalDateTime dateTime) {
        return add(name, formatIcalTime(dateTime));
    }

    /**
     * Begins a new iCalendar component (e.g., VCALENDAR, VEVENT).
     */
    public IcalBuilder begin(Component component) {
        componentsInProgress.add(component);
        calendarContent.append("BEGIN:").append(component.name()).append("\r\n");
        return this;
    }

    /**
     * Ends the current iCalendar component.
     */
    public IcalBuilder end() {
        checkArgument(!componentsInProgress.isEmpty());
        Component component = componentsInProgress.removeLast();
        calendarContent.append("END:").append(component.name()).append("\r\n");
        return this;
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
