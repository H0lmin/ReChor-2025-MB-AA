package ch.epfl.rechor.gui;

import ch.epfl.rechor.StopIndex;
import javafx.beans.value.ObservableValue;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.converter.LocalTimeStringConverter;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

import static java.util.Objects.requireNonNull;

/**
 * UI for querying journeys: departure/arrival stops, date, and time.
 *
 * @param root     the JavaFX node containing the query controls
 * @param depStopO observable providing the selected departure stop name
 * @param arrStopO observable providing the selected arrival stop name
 * @param dateO    observable providing the selected date
 * @param timeO    observable providing the selected time
 *
 * @author Amine AMIRA (393410)
 * @author Malak Berrada (379791)
 */
public record QueryUI(
        Node root,
        ObservableValue<String> depStopO,
        ObservableValue<String> arrStopO,
        ObservableValue<LocalDate> dateO,
        ObservableValue<LocalTime> timeO) {

    private static final String CSS_PATH = "query.css";
    private static final String ROOT_ID = "query";
    private static final String DATE_ID = "date";
    private static final String TIME_ID = "time";
    private static final DateTimeFormatter TIME_FORMAT_OUT = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter TIME_FORMAT_IN = DateTimeFormatter.ofPattern("H:mm");

    /**
     * Builds a QueryUI bound to the given StopIndex.
     *
     * @param index the stop index for departure/arrival fields (non-null)
     * @return configured QueryUI instance
     * @throws NullPointerException if index is null
     */
    public static QueryUI create(StopIndex index) {
        requireNonNull(index, "index must not be null");

        StopField departureField = StopField.create(index);
        StopField arrivalField = StopField.create(index);

        TextField departureText = configureStopText(departureField, "depStop",
                "Nom de l'arrêt de départ");
        TextField arrivalText = configureStopText(arrivalField, "arrStop",
                "Nom de l'arrêt d'arrivée");

        Button swapButton = createSwapButton(departureField, arrivalField);
        HBox topRow = buildTopRow(departureText, swapButton, arrivalText);

        DatePicker datePicker = new DatePicker(LocalDate.now());
        datePicker.setId(DATE_ID);

        TextFormatter<LocalTime> timeFormatter = new TextFormatter<>(
                new LocalTimeStringConverter(TIME_FORMAT_OUT, TIME_FORMAT_IN),
                LocalTime.now()
        );
        TextField timeText = new TextField();
        timeText.setId(TIME_ID);
        timeText.setTextFormatter(timeFormatter);

        HBox bottomRow = buildBottomRow(datePicker, timeText);

        VBox container = new VBox(topRow, bottomRow);
        container.setId(ROOT_ID);
        container.getStylesheets().add(CSS_PATH);

        return new QueryUI(
                container,
                departureField.selectedStopProperty(),
                arrivalField.selectedStopProperty(),
                datePicker.valueProperty(),
                timeFormatter.valueProperty()
        );
    }

    private static TextField configureStopText(StopField field, String id, String prompt) {
        TextField text = field.textField();
        text.setId(id);
        text.setPromptText(prompt);
        return text;
    }

    private static Button createSwapButton(StopField depField, StopField arrField) {
        Button swap = new Button("\uD83E\uDC58");
        swap.setOnAction(e -> {
            String temp = depField.textField().getText();
            depField.setTo(arrField.textField().getText());
            arrField.setTo(temp);
        });
        return swap;
    }

    private static HBox buildTopRow(TextField depText, Button swap, TextField arrText) {
        return new HBox(
                new Label("Départ\u202f:"),
                depText,
                swap,
                new Label("Arrivée\u202f:"),
                arrText
        );
    }

    private static HBox buildBottomRow(DatePicker datePicker, TextField timeField) {
        return new HBox(
                new Label("Date\u202f:"),
                datePicker,
                new Label("Heure\u202f:"),
                timeField
        );
    }
}