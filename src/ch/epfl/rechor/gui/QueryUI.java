package ch.epfl.rechor.gui;

import ch.epfl.rechor.StopIndex;
import javafx.beans.value.ObservableValue;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.converter.LocalTimeStringConverter;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Represents the query interface allowing users to select departure/arrival stops,
 * date, and time for a journey search.
 *
 * @param rootNode the JavaFX node at the root of the query interface scene graph
 * @param depStopO observable value containing the selected departure stop name
 * @param arrStopO observable value containing the selected arrival stop name
 * @param dateO    observable value containing the chosen travel date
 * @param timeO    observable value containing the chosen travel time
 */
public record QueryUI(
        Node rootNode,
        ObservableValue<String> depStopO,
        ObservableValue<String> arrStopO,
        ObservableValue<LocalDate> dateO,
        ObservableValue<LocalTime> timeO
) {

    /**
     * Builds and returns a QueryUI instance, constructing the scene graph
     * and wiring up all components according to the specification.
     *
     * @param index the StopIndex used to search stop names
     * @return a configured QueryUI
     */
    public static QueryUI create(StopIndex index) {
        // Departure stop input
        StopField depField = StopField.create(index);
        TextField depText = depField.textField();
        depText.setPromptText("Nom de l'arrêt de départ");
        depText.setId("depStop");
        Label depLabel = new Label("Départ\u202f:");

        // Arrival stop input
        StopField arrField = StopField.create(index);
        TextField arrText = arrField.textField();
        arrText.setPromptText("Nom de l'arrêt d'arrivée");
        arrText.setId("arrStop");
        Label arrLabel = new Label("Arrivée\u202f:");

        // Swap button between departure and arrival
        Button swapButton = new Button("\u21C4");
        swapButton.setOnAction(e -> {
            String depValue = depText.getText();
            String arrValue = arrText.getText();
            depField.setTo(arrValue);
            arrField.setTo(depValue);
        });

        // Top row HBox: departure, swap, arrival
        HBox topRow = new HBox();
        topRow.getChildren().addAll(
                depLabel, depText,
                swapButton,
                arrLabel, arrText
        );

        // Date picker for travel date
        DatePicker datePicker = new DatePicker(LocalDate.now());
        datePicker.setId("date");
        Label dateLabel = new Label("Date\u202f:");

        // Time input with formatter allowing H:mm and displaying HH:mm
        DateTimeFormatter toStringFmt = DateTimeFormatter.ofPattern("HH:mm");
        DateTimeFormatter fromStringFmt = DateTimeFormatter.ofPattern("H:mm");
        LocalTimeStringConverter converter =
                new LocalTimeStringConverter(toStringFmt, fromStringFmt);
        TextFormatter<LocalTime> timeFormatter = new TextFormatter<>(converter);
        timeFormatter.setValue(LocalTime.now());
        TextField timeField = new TextField();
        timeField.setId("time");
        timeField.setTextFormatter(timeFormatter);
        Label timeLabel = new Label("Heure\u202f:");

        // Bottom row HBox: date, time
        HBox bottomRow = new HBox();
        bottomRow.getChildren().addAll(
                dateLabel, datePicker,
                timeLabel, timeField
        );

        // Root VBox containing two rows
        VBox root = new VBox();
        root.getChildren().addAll(topRow, bottomRow);
        // Attach CSS using classpath resource
        root.setId("query");
        root.getStylesheets().add("query.css");


        return new QueryUI(
                root,
                depField.stopO(),
                arrField.stopO(),
                datePicker.valueProperty(),
                timeFormatter.valueProperty()
        );
    }
}
