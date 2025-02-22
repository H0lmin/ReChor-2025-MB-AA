package ch.epfl.rechor.journey;
import ch.epfl.rechor.FormatterFr;
import ch.epfl.rechor.IcalBuilder;

import java.time.LocalDateTime;
import java.util.StringJoiner;
import java.util.UUID;

import static ch.epfl.rechor.FormatterFr.formatTime;

public final class JourneyIcalConverter {

    private JourneyIcalConverter() {
        throw new AssertionError();
    }

    public static String toIcalendar(Journey journey) {
        IcalBuilder builder = new IcalBuilder();

        StringJoiner description = new StringJoiner("\n");
        for (Journey.Leg leg : journey.legs()) {
            switch (leg) {
                case Journey.Leg.Foot foot -> description.add(FormatterFr.formatLeg(foot));
                case Journey.Leg.Transport transport -> description.add(FormatterFr.formatLeg(transport));
                default -> throw new IllegalStateException("Unexpected value: " + leg);
            }
        }

        return builder
                .begin(IcalBuilder.Component.VCALENDAR)
                .add(IcalBuilder.Name.VERSION, "2.0")
                .add(IcalBuilder.Name.PRODID, "ReCHor")
                .begin(IcalBuilder.Component.VEVENT)
                .add(IcalBuilder.Name.UID, UUID.randomUUID().toString())
                .add(IcalBuilder.Name.DTSTAMP, formatTime(LocalDateTime.now()))
                .add(IcalBuilder.Name.DTSTART, journey.depTime())
                .add(IcalBuilder.Name.DTEND, journey.arrTime())
                .add(IcalBuilder.Name.SUMMARY, journey.depStop().name() + " → " + journey.arrStop().name())
                .add(IcalBuilder.Name.DESCRIPTION, description.toString())
                .end()
                .end()
                .build();
    }
}