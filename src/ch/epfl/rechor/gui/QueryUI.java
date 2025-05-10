package ch.epfl.rechor.gui;

import ch.epfl.rechor.StopIndex;
import javafx.beans.value.ObservableValue;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.converter.LocalTimeStringConverter;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * UI for querying journeys: departure/arrival stops, date, and time.
 */
public record QueryUI(
        Node root,
        ObservableValue<String> depStopO,
        ObservableValue<String> arrStopO,
        ObservableValue<LocalDate> dateO,
        ObservableValue<LocalTime> timeO
) {
    /**
     * Creates the QueryUI record initialized with stop index bindings.
     */
    public static QueryUI create(StopIndex index) {
        StopField depField = StopField.create(index);
        StopField arrField = StopField.create(index);

        TextField depText = depField.textField();
        depText.setPromptText("Nom de l'arrêt de départ");
        depText.setId("depStop");

        TextField arrText = arrField.textField();
        arrText.setPromptText("Nom de l'arrêt d'arrivée");
        arrText.setId("arrStop");

        Button swapButton = new Button("\uD83E\uDC58");
        swapButton.setOnAction(e -> {
            String tmp = depText.getText();
            depField.setTo(arrText.getText());
            arrField.setTo(tmp);
        });

        HBox topRow = new HBox(
                new Label("Départ\u202f:"),
                depText,
                swapButton,
                new Label("Arrivée\u202f:"),
                arrText
        );

        DatePicker datePicker = new DatePicker(LocalDate.now());
        datePicker.setId("date");

        DateTimeFormatter fmtOut = DateTimeFormatter.ofPattern("HH:mm");
        DateTimeFormatter fmtIn  = DateTimeFormatter.ofPattern("H:mm");
        TextFormatter<LocalTime> timeFmt = new TextFormatter<>(
                new LocalTimeStringConverter(fmtOut, fmtIn),
                LocalTime.now()
        );
        TextField timeField = new TextField();
        timeField.setId("time");
        timeField.setTextFormatter(timeFmt);

        HBox bottomRow = new HBox(
                new Label("Date\u202f:"),
                datePicker,
                new Label("Heure\u202f:"),
                timeField
        );

        VBox root = new VBox();
        root.getChildren().addAll(topRow, bottomRow);
        root.setId("query");
        root.getStylesheets().add("query.css");

        return new QueryUI(
                root,
                depField.stopO(),
                arrField.stopO(),
                datePicker.valueProperty(),
                timeFmt.valueProperty()
        );
    }
}
