package ch.epfl.rechor.gui;

import ch.epfl.rechor.FormatterFr;
import ch.epfl.rechor.journey.Journey;
import ch.epfl.rechor.journey.JourneyGeoJsonConverter;
import ch.epfl.rechor.journey.JourneyIcalConverter;
import javafx.beans.value.ObservableValue;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.control.*;
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
import java.time.LocalDate;

public record DetailUI(Node rootNode) {

    public static DetailUI create(ObservableValue<Journey> journeyObs) {
        StackPane root = new StackPane();
        root.getStyleClass().add("detail-root");
        root.getStylesheets().add("detail.css");

        Label noJourney = new Label("Aucun voyage");
        StackPane.setAlignment(noJourney, Pos.CENTER);

        BorderPane detailPane = new BorderPane();

        root.getChildren().addAll(noJourney, detailPane);

        journeyObs.addListener((o, oldVal, newVal)
                -> updateView(newVal, noJourney, detailPane));
        updateView(journeyObs.getValue(), noJourney, detailPane);

        return new DetailUI(root);
    }

    private static void updateView(Journey journey,
                                   Label noJourney,
                                   BorderPane detailPane) {
        if (journey == null) {
            noJourney.setVisible(true);
            detailPane.setVisible(false);
            detailPane.setCenter(null);
        } else {
            noJourney.setVisible(false);
            detailPane.setVisible(true);
            Node grid = buildStepsGrid(journey);
            Node buttons = buildButtonsBar(journey);
            VBox content = new VBox(grid, buttons);
            content.setSpacing(0);
            detailPane.setCenter(content);
        }
    }

    private static Node buildStepsGrid(Journey journey) {
        GridPane grid = new GridPane();
        grid.setVgap(0);
        grid.setPadding(new Insets(0, 0, 0, 10));
        grid.setId("steps-grid");

        ColumnConstraints col0 = new ColumnConstraints(); // Heure
        ColumnConstraints col1 = new ColumnConstraints(); // Cercle
        ColumnConstraints col2 = new ColumnConstraints(); // Nom station
        ColumnConstraints col3 = new ColumnConstraints(); // Direction
        col2.setHgrow(Priority.ALWAYS);
        col3.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(col0, col1, col2, col3);

        Pane lineLayer = new Pane();
        lineLayer.setMouseTransparent(true); // Important pour laisser passer les clics

        StepsGridPane stepsGrid = new StepsGridPane(lineLayer) {{
            setVgap(0);
            setHgap(0);
            setPadding(new Insets(0, 0, 0, 10));
            setId("steps-grid");
            getColumnConstraints().addAll(col0, col1, col2, col3);
        }};

        int row = 0;
        for (Journey.Leg leg : journey.legs()) {
            switch (leg) {
                case Journey.Leg.Foot foot -> {
                    Label text = new Label(FormatterFr.formatLeg(foot));
                    stepsGrid.add(text, 2, row, 2, 1);
                    row++;
                }
                case Journey.Leg.Transport tr -> {
                    row = addTransportLeg(stepsGrid, tr, row);
                }
            }
        }

        StackPane layered = new StackPane(stepsGrid, lineLayer);
        layered.setId("steps-layered");
        return layered;
    }


    private static int addTransportLeg(StepsGridPane grid,
                                       Journey.Leg.Transport tr,
                                       int row) {
        // Departure row
        Label depTime = new Label(FormatterFr.formatTime(tr.depTime()));
        depTime.getStyleClass().add("departure");
        GridPane.setHalignment(depTime, HPos.RIGHT);
        Circle depCircle = new Circle(3, Color.BLACK);
        Label depStation = new Label(tr.depStop().name());
        depStation.setPadding(Insets.EMPTY);
        Label depPlatform = new Label(FormatterFr.formatPlatformName(tr.depStop()));
        depPlatform.getStyleClass().add("departure");
        GridPane.setHalignment(depPlatform, HPos.LEFT);

        grid.add(depTime, 0, row);
        grid.add(depCircle, 1, row);
        grid.add(depStation, 2, row);
        grid.add(depPlatform, 3, row);
        row++;

        // Icon & route row
        ImageView icon = new ImageView(VehicleIcons.iconFor(tr.vehicle()));
        icon.setFitWidth(20); // taille plus compacte
        icon.setFitHeight(20);
        icon.setPreserveRatio(true);
        icon.setSmooth(true);
        icon.setCache(true);

        GridPane.setHalignment(icon, HPos.CENTER);
        GridPane.setValignment(icon, VPos.CENTER);

        Label routeDest = new Label(FormatterFr.formatRouteDestination(tr));
        routeDest.setWrapText(false);
        routeDest.setMaxWidth(Double.MAX_VALUE);
        GridPane.setHgrow(routeDest, Priority.ALWAYS);

        boolean hasInt = !tr.intermediateStops().isEmpty();
        grid.add(icon, 0, row, 1, hasInt ? 2 : 1);
        grid.add(routeDest, 2, row, 2, 1);
        row++;


        if (!tr.intermediateStops().isEmpty()) {
            int count = tr.intermediateStops().size();
            long dur = Duration.between(tr.depTime(), tr.arrTime()).toMinutes();
            TitledPane tp = new TitledPane(count + " arrêts, " + dur + " min", buildIntermediateGrid(tr));
            Accordion acc = new Accordion(tp);
            grid.add(acc, 2, row, 2, 1);
            row++;
        }

        // Arrival row
        Label arrTime = new Label(FormatterFr.formatTime(tr.arrTime()));
        GridPane.setHalignment(arrTime, HPos.RIGHT);
        Circle arrCircle = new Circle(3, Color.BLACK);
        Label arrStation = new Label(tr.arrStop().name());
        arrStation.setPadding(Insets.EMPTY);
        Label arrPlatform = new Label(FormatterFr.formatPlatformName(tr.arrStop()));
        GridPane.setHalignment(arrPlatform, HPos.LEFT);

        grid.add(arrTime, 0, row);
        grid.add(arrCircle, 1, row);
        grid.add(arrStation, 2, row);
        grid.add(arrPlatform, 3, row);
        grid.connect(depCircle, arrCircle);

        GridPane.setMargin(depStation, new Insets(0));
        GridPane.setMargin(routeDest, new Insets(0));

        return row + 1;
    }

    private static GridPane buildIntermediateGrid(Journey.Leg.Transport tr) {
        GridPane g = new GridPane();
        g.getStyleClass().add("intermediate-stops");

        int r = 0;
        for (Journey.Leg.IntermediateStop s : tr.intermediateStops()) {
            Label at = new Label(FormatterFr.formatTime(s.arrTime()));
            Label dt = new Label(FormatterFr.formatTime(s.depTime()));
            Label nm = new Label(s.stop().name());
            nm.setPadding(Insets.EMPTY);
            g.add(at, 0, r);
            g.add(dt, 1, r);
            g.add(nm, 2, r);
            r++;
        }
        return g;
    }

    private static Node buildButtonsBar(Journey journey) {
        HBox bar = new HBox(10);
        bar.setId("buttons-bar");
        bar.setAlignment(Pos.CENTER);
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
                    .toGeoJson(journey)
                    .toString()
                    .replaceAll("\\s+", "");

            String encoded = URLEncoder.encode(geo, StandardCharsets.UTF_8);

            URI uri = new URI("https", "umap.osm.ch",
                    "/fr/map/", "data=" + encoded, null);

            Desktop.getDesktop().browse(uri);
        } catch (IOException | URISyntaxException e) {
            e.printStackTrace();
        }
    }

    private static void exportToIcs(Journey j) {
        LocalDate d = j.depTime().toLocalDate();
        String fn = "voyage_" + d + ".ics";
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Exporter le voyage au format iCalendar");
        chooser.setInitialFileName(fn);
        File f = chooser.showSaveDialog(null);
        if (f != null) {
            try {
                Files.writeString(f.toPath(), JourneyIcalConverter.toIcalendar(j));
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    private static class StepsGridPane extends GridPane {
        private final java.util.List<Circle[]> connections = new java.util.ArrayList<>();
        private final Pane linePane;

        public StepsGridPane(Pane linePane) {
            this.linePane = linePane;
        }

        public void connect(Circle a, Circle b) {
            connections.add(new Circle[]{a, b});
        }

        @Override
        protected void layoutChildren() {
            super.layoutChildren();
            linePane.getChildren().clear();
            for (Circle[] pair : connections) {
                Circle c1 = pair[0], c2 = pair[1];
                var b1 = c1.localToScene(c1.getBoundsInLocal());
                var b2 = c2.localToScene(c2.getBoundsInLocal());
                var p1 = linePane.sceneToLocal(b1.getCenterX(), b1.getCenterY() + 4);
                var p2 = linePane.sceneToLocal(b2.getCenterX(), b2.getCenterY() - 4);
                Line ln = new Line(p1.getX(), p1.getY(), p2.getX(), p2.getY());
                ln.setStrokeWidth(2);
                ln.setStroke(Color.RED);
                linePane.getChildren().add(ln);
            }
        }
    }

}
