package ch.epfl.rechor.gui;

import ch.epfl.rechor.FormatterFr;
import ch.epfl.rechor.journey.Journey;
import ch.epfl.rechor.journey.JourneyGeoJsonConverter;
import ch.epfl.rechor.journey.JourneyIcalConverter;
import javafx.application.Platform;
import javafx.beans.value.ObservableValue;
import javafx.geometry.HPos;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.control.Accordion;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TitledPane;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.stage.FileChooser;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Duration;

public record DetailUI(Node rootNode) {

    public static DetailUI create(ObservableValue<Journey> journeyObs) {
        StackPane root = new StackPane();
        root.setId("detail");
        root.getStylesheets().add("detail.css");

        Label noJourney = new Label("Aucun voyage");
        noJourney.setId("no-journey");
        StackPane.setAlignment(noJourney, Pos.CENTER);

        BorderPane detailPane = new BorderPane();
        VBox content = new VBox();
        content.setSpacing(4);
        detailPane.setCenter(content);

        root.getChildren().addAll(noJourney, detailPane);

        journeyObs.addListener((o, oldVal, newVal) -> updateView(newVal, noJourney, content));
        Platform.runLater(() ->
                updateView(journeyObs.getValue(), noJourney, content));
        return new DetailUI(root);
    }

    private static void updateView(Journey journey, Label noJourney, VBox content) {
        noJourney.setVisible(journey == null);
        content.setVisible(journey != null);

        if (journey != null) {
            Node grid = buildStepsGrid(journey);
            Node buttons = buildButtonsBar(journey);
            content.getChildren().setAll(grid, buttons);
        } else {
            content.getChildren().clear();
        }
    }

    private static Node buildStepsGrid(Journey journey) {
        StepsGridPane grid = new StepsGridPane();
        grid.setId("legs");

        int row = 0;
        for (Journey.Leg leg : journey.legs()) {
            switch (leg) {
                case Journey.Leg.Transport tr -> row = addTransportLeg(grid, tr, row);
                case Journey.Leg.Foot foot -> {
                    Label text = new Label(FormatterFr.formatLeg(foot));
                    grid.add(text, 2, row, 2, 1);
                    row++;
                }
            }
        }
        return grid;
    }

    private static int addTransportLeg(StepsGridPane grid, Journey.Leg.Transport tr, int row) {
        // Departure details
        Label depTime = new Label(FormatterFr.formatTime(tr.depTime()));
        depTime.getStyleClass().add("departure");
        GridPane.setHalignment(depTime, HPos.RIGHT);
        Circle depCircle = new Circle(3, Color.BLACK);
        Label depStation = new Label(tr.depStop().name());
        Label depPlatform = new Label(FormatterFr.formatPlatformName(tr.depStop()));
        depPlatform.getStyleClass().add("departure");
        GridPane.setHalignment(depPlatform, HPos.LEFT);

        grid.addRow(row++, depTime, depCircle, depStation, depPlatform);

        // Icon & route info
        ImageView icon = new ImageView(VehicleIcons.iconFor(tr.vehicle()));
        icon.setFitWidth(31);
        icon.setFitHeight(31);
        icon.setPreserveRatio(true);
        GridPane.setHalignment(icon, HPos.CENTER);
        GridPane.setValignment(icon, VPos.CENTER);
        boolean hasIntermediate = !tr.intermediateStops().isEmpty();
        grid.add(icon, 0, row, 1, hasIntermediate ? 2 : 1);

        Label routeDest = new Label(FormatterFr.formatRouteDestination(tr));
        grid.add(routeDest, 2, row++, 2, 1);

        // Intermediate stops accordion
        if (hasIntermediate) {
            int count = tr.intermediateStops().size();
            long dur = Duration.between(tr.depTime(), tr.arrTime()).toMinutes();
            TitledPane tp = new TitledPane(count + " arrêts, " + dur + " min",
                    buildIntermediateGrid(tr));
            Accordion acc = new Accordion(tp);
            grid.add(acc, 2, row++, 2, 1);
        }

        // Arrival details
        Label arrTime = new Label(FormatterFr.formatTime(tr.arrTime()));
        GridPane.setHalignment(arrTime, HPos.RIGHT);
        Circle arrCircle = new Circle(3, Color.BLACK);
        Label arrStation = new Label(tr.arrStop().name());
        Label arrPlatform = new Label(FormatterFr.formatPlatformName(tr.arrStop()));
        GridPane.setHalignment(arrPlatform, HPos.LEFT);

        grid.addRow(row++, arrTime, arrCircle, arrStation, arrPlatform);
        grid.connect(depCircle, arrCircle);

        return row;
    }

    private static GridPane buildIntermediateGrid(Journey.Leg.Transport tr) {
        GridPane grid = new GridPane();
        grid.getStyleClass().add("intermediate-stops");

        int row = 0;
        for (Journey.Leg.IntermediateStop s : tr.intermediateStops()) {
            grid.addRow(row++,
                    new Label(FormatterFr.formatTime(s.arrTime())),
                    new Label(FormatterFr.formatTime(s.depTime())),
                    new Label(s.stop().name()));
        }
        return grid;
    }

    private static Node buildButtonsBar(Journey journey) {
        HBox bar = new HBox();
        bar.setId("buttons");
        bar.setAlignment(Pos.BASELINE_CENTER);

        Button map = new Button("Carte");
        map.setOnAction(e -> openGeoJsonMap(journey));
        Button cal = new Button("Calendrier");
        cal.setOnAction(e -> exportToIcs(journey));

        bar.getChildren().addAll(map, cal);
        return bar;
    }

    private static void openGeoJsonMap(Journey journey) {
        try {
            String geo = JourneyGeoJsonConverter
                    .toGeoJson(journey).toString()
                    .replaceAll("\\s+", "");

            String encoded = URLEncoder.encode(geo, StandardCharsets.UTF_8);

            URI uri = new URI("https", "umap.osm.ch",
                    "/fr/map/", "data=" + encoded, null);

            Desktop.getDesktop().browse(uri);
        } catch (IOException | URISyntaxException e) {
            e.printStackTrace();
        }
    }

    private static void exportToIcs(Journey journey) {
        String fileName = "voyage_" + journey.depTime().toLocalDate() + ".ics";
        FileChooser chooser = new FileChooser();
        chooser.setInitialFileName(fileName);
        chooser.setTitle("Exporter le voyage au format iCalendar");
        File file = chooser.showSaveDialog(null);
        if (file != null) {
            try {
                Files.writeString(file.toPath(), JourneyIcalConverter.toIcalendar(journey));
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    private static class StepsGridPane extends GridPane {
        private final java.util.List<Circle[]> connections = new java.util.ArrayList<>();

        public void connect(Circle a, Circle b) {
            connections.add(new Circle[]{a, b});
        }

        @Override
        protected void layoutChildren() {
            super.layoutChildren();
            getChildren().removeIf(Line.class::isInstance);
            connections.forEach(pair -> {
                var start = pair[0].localToParent(pair[0].getCenterX(), pair[0].getCenterY());
                var end = pair[1].localToParent(pair[1].getCenterX(), pair[1].getCenterY());
                Line line = new Line(start.getX(), start.getY() + 4, end.getX(), end.getY() - 4);
                line.setStroke(Color.RED);
                line.setStrokeWidth(2);
                getChildren().add(line);
            });
        }
    }
}