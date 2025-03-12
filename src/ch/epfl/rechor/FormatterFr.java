package ch.epfl.rechor;

import ch.epfl.rechor.journey.Journey;
import ch.epfl.rechor.journey.Stop;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;

public final class FormatterFr {

    private FormatterFr() {
    }

    public static String formatDuration(Duration duration) {
        long hours = duration.toHours();
        long minutes = duration.toMinutesPart();

        if (hours > 0) {
            return String.format("%d h %d min", hours, minutes);
        } else {
            return String.format("%d min", minutes);
        }
    }

    public static String formatTime(LocalDateTime dateTime) {
        DateTimeFormatter formatter = new DateTimeFormatterBuilder()
                .appendValue(ChronoField.HOUR_OF_DAY)
                .appendLiteral('h')
                .appendValue(ChronoField.MINUTE_OF_HOUR, 2)
                .toFormatter();
        return dateTime.format(formatter);
    }

    public static String formatPlatformName(Stop stop) {
        String platformName = stop.platformName();

        if (stop.platformName() == null || stop.platformName().isEmpty()) {
            return "";
        }
        return Character.isDigit(platformName.charAt(0)) ? "voie " + platformName : "quai " + platformName;
    }

    public static String formatLeg(Journey.Leg.Foot footLeg) {
        String type = footLeg.isTransfer() ? "changement" : "trajet à pied";
        return String.format("%s (%s)", type, formatDuration(footLeg.duration()));
    }

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

    public static String formatRouteDestination(Journey.Leg.Transport transportLeg) {
        return transportLeg.route() + " Direction " + transportLeg.destination();
    }
}
