package ch.epfl.rechor;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import static ch.epfl.rechor.Preconditions.checkArgument;

public final class IcalBuilder {
    private final StringBuilder calendarContent = new StringBuilder();
    private final List<Component> componentsInProgress = new ArrayList<>();

    private String foldLine(String value) {
        StringBuilder sb = new StringBuilder();
        int start = 0;
        int length = value.length();

        while (start < length) {
            int end = Math.min(start + 74, length);

            sb.append(value, start, end);

            if (end < length) {
                sb.append("\r\n ");
            }

            start = end;
        }

        return sb.toString();
    }

    private String formatIcalTime(LocalDateTime dateTime) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss");
        return dateTime.format(fmt);
    }

    public IcalBuilder add(Name name, String value) {
        calendarContent.append(foldLine(name.name() + ":" + value)).append("\r\n");
        return this;
    }

    public IcalBuilder add(Name name, LocalDateTime dateTime) {
        String dateTimeFormatted = formatIcalTime(dateTime);
        return add(name, dateTimeFormatted);
    }

    public IcalBuilder begin(Component component) {
        componentsInProgress.add(component);
        calendarContent.append("BEGIN:").append(component.name()).append("\r\n");
        return this;
    }

    public IcalBuilder end() {
        checkArgument(!componentsInProgress.isEmpty());
        Component component = componentsInProgress.removeLast();
        calendarContent.append("END:").append(component.name()).append("\r\n");
        return this;
    }

    public String build() {
        checkArgument(componentsInProgress.isEmpty());
        return calendarContent.toString();
    }

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
}
