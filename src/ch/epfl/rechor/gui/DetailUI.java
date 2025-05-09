package ch.epfl.rechor.gui;

import ch.epfl.rechor.FormatterFr;
import ch.epfl.rechor.journey.Journey;
import ch.epfl.rechor.journey.JourneyGeoJsonConverter;
import ch.epfl.rechor.journey.JourneyIcalConverter;
import ch.epfl.rechor.journey.Vehicle;
import javafx.beans.value.ObservableValue;
import javafx.geometry.HPos;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.control.Accordion;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TitledPane;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.stage.FileChooser;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Displays journey details.
 */
public record DetailUI(Node rootNode) {
    private static final double CIRCLE_RADIUS = 3;
    private static final int ICON_SIZE = 31;

    public static DetailUI create(ObservableValue<Journey> journeyObs) {
        StackPane root = new StackPane();
        root.setId("detail");
        root.getStylesheets().add("detail.css");

        Label noJourney = new Label("Aucun voyage");
        noJourney.setId("no-journey");

        VBox content = new VBox();

        // Wrap content for scrolling
        ScrollPane scroll = new ScrollPane(content);
        scroll.setPannable(true);

        root.getChildren().setAll(scroll, noJourney);

        journeyObs.addListener((o, oldV, newV)
                -> updateView(newV, noJourney, content));

        updateView(journeyObs.getValue(), noJourney, content);
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
        Pane annotations = new Pane();
        StepsGridPane grid = new StepsGridPane(annotations);
        grid.setId("legs");

        int row = 0;
        for (Journey.Leg leg : journey.legs()) {
            switch (leg) {
                case Journey.Leg.Foot foot -> {
                    Label text = new Label(FormatterFr.formatLeg(foot));
                    grid.add(text, 2, row, 2, 1);
                    row++;
                }
                case Journey.Leg.Transport tr -> row = addTransportLeg(grid, tr, row);
            }
        }
        return new StackPane(annotations, grid);
    }

    private static int addTransportLeg(StepsGridPane grid, Journey.Leg.Transport tr, int row) {
        // Departure details
        Label depTime = new Label(FormatterFr.formatTime(tr.depTime()));
        depTime.getStyleClass().add("departure");
        GridPane.setHalignment(depTime, HPos.RIGHT);
        Circle depCircle = circle();
        Label depStation = new Label(tr.depStop().name());
        Label depPlatform = new Label(FormatterFr.formatPlatformName(tr.depStop()));
        depPlatform.getStyleClass().add("departure");
        GridPane.setHalignment(depPlatform, HPos.CENTER);

        grid.addRow(row++, depTime, depCircle, depStation, depPlatform);

        // Icon & route info
        ImageView icon = iconModeling(tr.vehicle());
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
        Circle arrCircle = circle();
        Label arrStation = new Label(tr.arrStop().name());
        Label arrPlatform = new Label(FormatterFr.formatPlatformName(tr.arrStop()));
        GridPane.setHalignment(arrPlatform, HPos.CENTER);

        grid.addRow(row++, arrTime, arrCircle, arrStation, arrPlatform);
        grid.connect(depCircle, arrCircle);

        return row;
    }

    private static GridPane buildIntermediateGrid(Journey.Leg.Transport tr) {
        GridPane grid = new GridPane();
        grid.getStyleClass().add("intermediate-stops");
        int row = 0;
        for (var s : tr.intermediateStops()) {
            grid.addRow(row++,
                    new Label(FormatterFr.formatTime(s.arrTime())),
                    new Label(FormatterFr.formatTime(s.depTime())),
                    new Label(s.stop().name())
            );
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
            String geo = JourneyGeoJsonConverter.toGeoJson(journey).toString()
                    .replaceAll("\\s+", "");
            String enc = URLEncoder.encode(geo, StandardCharsets.UTF_8);
            URI uri = new URI("https", "umap.osm.ch", "/fr/map/",
                    "data=" + enc, null);
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

    private static ImageView iconModeling(Vehicle v) {
        ImageView iv = new ImageView(VehicleIcons.iconFor(v));
        iv.setFitWidth(ICON_SIZE);
        iv.setFitHeight(ICON_SIZE);
        iv.setPreserveRatio(true);
        GridPane.setHalignment(iv, HPos.CENTER);
        GridPane.setValignment(iv, VPos.CENTER);
        return iv;
    }

    private static Circle circle() {
        return new Circle(CIRCLE_RADIUS, Color.BLACK);
    }

    private static class StepsGridPane extends GridPane {
        private final Pane annotationPane;
        private final List<Circle[]> connections = new ArrayList<>();

        public StepsGridPane(Pane annotationPane) {
            this.annotationPane = annotationPane;
        }

        public void connect(Circle a, Circle b) {
            connections.add(new Circle[]{a, b});
        }

        @Override
        protected void layoutChildren() {
            super.layoutChildren();
            annotationPane.getChildren().removeIf(Line.class::isInstance);
            for (Circle[] pair : connections) {
                Point2D srt = pair[0].localToParent(pair[0].getCenterX(), pair[0].getCenterY());
                Point2D end = pair[1].localToParent(pair[1].getCenterX(), pair[1].getCenterY());
                Line line = new Line(srt.getX(), srt.getY()+4, end.getX(), end.getY()-4);
                line.setStroke(Color.RED);
                line.setStrokeWidth(2);
                annotationPane.getChildren().add(line);
            }
        }
    }
}