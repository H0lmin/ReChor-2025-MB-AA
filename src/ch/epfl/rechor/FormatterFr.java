package ch.epfl.rechor;

import ch.epfl.rechor.journey.Journey;
import ch.epfl.rechor.journey.Stop;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;

/**
 * Utility class for formatting various journey-related information in French.
 * This class provides methods to format durations, times, stop platform names, and journey legs in French.
 * <p>
 * The class cannot be instantiated.
 */
public final class FormatterFr {

    private FormatterFr() {
    }

    /**
     * Formats a duration in the format of hours and minutes, or minutes only if no hours are present.
     *
     * @param duration The duration to format.
     * @return The formatted duration as a string.
     */
    public static String formatDuration(Duration duration) {
        long hours = duration.toHours();
        long minutes = duration.toMinutesPart();

        if (hours > 0) {
            return String.format("%d h %d min", hours, minutes);
        } else {
            return String.format("%d min", minutes);
        }
    }

    /**
     * Formats a {@link LocalDateTime} object to a time string in the format "HHhMM".
     *
     * @param dateTime The {@link LocalDateTime} object to format.
     * @return The formatted time string.
     */
    public static String formatTime(LocalDateTime dateTime) {
        DateTimeFormatter formatter = new DateTimeFormatterBuilder()
                .appendValue(ChronoField.HOUR_OF_DAY)
                .appendLiteral('h')
                .appendValue(ChronoField.MINUTE_OF_HOUR, 2)
                .toFormatter();
        return dateTime.format(formatter);
    }

    /**
     * Formats the platform name of a stop in French, prefixing it with either "voie" or "quai".
     *
     * @param stop The stop whose platform name needs to be formatted.
     * @return The formatted platform name, or an empty string if there is no platform name.
     */
    public static String formatPlatformName(Stop stop) {
        String platformName = stop.platformName();

        if (stop.platformName() == null || stop.platformName().isEmpty()) {
            return "";
        }
        return Character.isDigit(platformName.charAt(0)) ? "voie " + platformName : "quai " + platformName;
    }

    /**
     * Formats a foot leg of the journey, indicating whether it is a transfer or a walking journey.
     *
     * @param footLeg The foot leg to format.
     * @return The formatted foot leg as a string.
     */
    public static String formatLeg(Journey.Leg.Foot footLeg) {
        String type = footLeg.isTransfer() ? "changement" : "trajet à pied";
        return String.format("%s (%s)", type, formatDuration(footLeg.duration()));
    }

    /**
     * Formats a transport leg of the journey, including the departure and arrival stops, times, and platforms.
     *
     * @param leg The transport leg to format.
     * @return The formatted transport leg as a string.
     */
    public static String formatLeg(Journey.Leg.Transport leg) {
        StringBuilder sb = new StringBuilder();
        sb.append(formatTime(leg.depTime()))
                .append(" ")
                .append(leg.depStop().name());

        String depPlatform = formatPlatformName(leg.depStop());
        if (!depPlatform.isEmpty()) {
            sb.append(" (").append(depPlatform).append(")");
        }

        sb.append(" → ")
                .append(leg.arrStop().name())
                .append(" (arr. ")
                .append(formatTime(leg.arrTime()));

        String arrPlatform = formatPlatformName(leg.arrStop());
        if (!arrPlatform.isEmpty()) {
            sb.append(" ").append(arrPlatform);
        }
        sb.append(")");

        return sb.toString();
    }

    /**
     * Formats the route and destination of a transport leg.
     *
     * @param transportLeg The transport leg to format.
     * @return The formatted route and destination as a string.
     */
    public static String formatRouteDestination(Journey.Leg.Transport transportLeg) {
        return transportLeg.route() + " Direction " + transportLeg.destination();
    }
}
